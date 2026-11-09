// Optional browser regression: run against an isolated test database, never production.
const path = require('node:path');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const { chromium } = require(path.resolve(__dirname, '../../../../.gradle/browser-check/node_modules/playwright'));
const base = process.env.TANKZORS_TEST_URL || 'http://127.0.0.1:18081';
const output = path.resolve(__dirname, '../../../build/browser-smoke');
fs.mkdirSync(output, { recursive: true });

async function waitFor(predicate, label) {
    const deadline = Date.now() + 60000;
    while (!predicate()) {
        if (Date.now() > deadline) throw new Error('Timeout: ' + label);
        await new Promise(resolve => setTimeout(resolve, 100));
    }
}

// Only the bootstrap map is replaced: real login, issued run, bridge and database writes are used.
const victoryMap = {
    name: 'Persistence smoke', width: 5, height: 4, tileSize: 40, enemies: [],
    rewards: { stars: 3, coins: 100 },
    grid: [[1,1,1,1,1], [1,3,0,0,1], [1,0,0,0,1], [1,1,1,1,1]]
};

async function signIn(page, username) {
    await page.goto(base + '/register');
    await page.locator('#username').fill(username);
    await page.locator('#password').fill('secret123');
    await page.locator('#confirmPassword').fill('secret123');
    await Promise.all([
        page.waitForURL('**/login'),
        page.locator('form.auth-form button[type=submit]').click()
    ]);
    await page.locator('#username').fill(username);
    await page.locator('#password').fill('secret123');
    await Promise.all([
        page.waitForURL('**/menu'),
        page.locator('form.auth-form button[type=submit]').click()
    ]);
}

async function run() {
    const launch = { headless: true, args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--autoplay-policy=no-user-gesture-required'] };
    if (process.env.BROWSER_EXECUTABLE) launch.executablePath = process.env.BROWSER_EXECUTABLE;
    else launch.channel = 'chrome';
    const browser = await chromium.launch(launch);
    const reports = [];
    try {
        for (const scenario of ['delayed-save-and-lost-response', 'conflict-is-not-success', 'reload-recovers-outbox']) {
            const context = await browser.newContext({ viewport: { width: 1100, height: 760 } });
            const page = await context.newPage();
            const errors = [];
            page.on('pageerror', error => errors.push(error.message));
            await signIn(page, 'qa-' + Date.now().toString(36) + '-' + reports.length);
            let bootstraps = 0, posts = 0;
            const responses = [], bodies = [];
            let release;
            const gate = new Promise(resolve => { release = resolve; });
            await page.route('**/api/game/bootstrap?**', async route => {
                bootstraps++;
                const response = await route.fetch();
                if (response.status() !== 200) return route.fulfill({ response });
                const data = await response.json();
                data.map = JSON.parse(JSON.stringify(victoryMap));
                // Keep level 2 playing so this test only credits the first victory.
                if (data.levelId !== 'campaign_1') data.map.grid[2][2] = 9;
                await route.fulfill({ response, json: data });
            });
            await page.route('**/api/game/results', async route => {
                posts++;
                const attempt = posts;
                bodies.push(route.request().postData());
                if (attempt === 1 && scenario !== 'conflict-is-not-success') await gate;
                if (attempt === 1 && scenario === 'conflict-is-not-success') {
                    return route.fulfill({ status: 409, contentType: 'application/json', body: '{"error":"test conflict"}' });
                }
                if (attempt === 1 && scenario === 'reload-recovers-outbox') {
                    try { await route.abort('failed'); } catch { /* reload already canceled it */ }
                    return;
                }
                const response = await route.fetch();
                responses.push(await response.json());
                if (attempt === 1 && scenario === 'delayed-save-and-lost-response') return route.abort('failed');
                await route.fulfill({ response });
            });

            await page.goto(base + '/game/libgdx/index.html?levelId=campaign_1');
            await waitFor(() => posts >= 1, 'first result: ' + scenario);
            let expectedCoins = 100;
            if (scenario === 'delayed-save-and-lost-response') {
                await page.locator('canvas').click({ position: { x: 70, y: 70 } });
                await page.keyboard.press('Enter');
                await new Promise(resolve => setTimeout(resolve, 300));
                assert.equal(bootstraps, 1, 'Next must wait for the save acknowledgment');
                release();
                await waitFor(() => responses.some(result => result.alreadySaved === true), 'idempotent retry');
                await page.waitForFunction(() => Object.keys(sessionStorage)
                    .filter(key => key.startsWith('tankzors_outbox:') && !key.includes('rejected:'))
                    .every(key => JSON.parse(sessionStorage.getItem(key)).length === 0));
                assert.equal(bodies[0], bodies[1], 'Retry must preserve the exact payload');
                await page.screenshot({ path: path.join(output, 'victory.png') });

                // Use the actual shop UI, then verify its final write does not trap transition.
                await page.keyboard.press('Enter');
                for (let i = 0; i < 28; i++) await page.keyboard.press('ArrowDown');
                const purchase = page.waitForResponse(response => response.url().endsWith('/api/game/shop/sync')
                    && response.status() === 200 && response.request().postData().includes('"BUY"'));
                await page.keyboard.press('ArrowRight');
                assert.equal((await (await purchase).json()).totalCoins, 65);
                await page.keyboard.press('Escape');
                await waitFor(() => bootstraps >= 2, 'transition after final shop acknowledgment');
                expectedCoins = 65;
            } else if (scenario === 'conflict-is-not-success') {
                await new Promise(resolve => setTimeout(resolve, 1400));
                assert.equal(posts, 1, '409 must not be retried automatically');
                const before = await (await context.request.get(base + '/api/game/account')).json();
                assert.equal(before.totalCoins, 0);
                await page.locator('canvas').click({ position: { x: 70, y: 70 } });
                await page.keyboard.press('KeyR');
                await waitFor(() => responses.some(result => result.saved === true), 'reload after rejected request');
            } else {
                const pending = await page.evaluate(() => Object.keys(sessionStorage)
                    .filter(key => key.startsWith('tankzors_outbox:')).map(key => sessionStorage.getItem(key)));
                assert(pending.some(value => value.includes('/api/game/results')), 'Pending result must be stored before sending');
                await page.reload();
                release();
                await waitFor(() => responses.some(result => result.saved === true), 'outbox replay after reload');
            }
            const account = await (await context.request.get(base + '/api/game/account')).json();
            const progress = await (await context.request.get(base + '/api/progress')).json();
            assert.equal(account.totalCoins, expectedCoins);
            assert.equal(account.totalStars, 3);
            assert.deepEqual(progress.unlockedLevels, ['campaign_2']);
            assert.equal(errors.length, 0, errors.join('\n'));
            reports.push({ scenario, posts, coins: account.totalCoins, stars: account.totalStars, pass: true });
            await context.close();
        }
        fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(reports, null, 2));
        console.log(JSON.stringify(reports, null, 2));
    } finally {
        await browser.close();
    }
}
run().catch(error => { console.error(error); process.exitCode = 1; });
