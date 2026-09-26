package com.tankzors.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.model.BulletSnapshot;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.model.MapSnapshot;
import com.tankzors.client.model.TankSnapshot;

/**
 * Render snapshot của session hiện tại.
 *
 * <p>Chiến lược:</p>
 * <ul>
 *   <li>Map vẽ thủ công từng tile.</li>
 *   <li>Tank/bullet vẽ bằng SpriteBatch từ AssetRepository.</li>
 *   <li>HP bar + HUD vẽ bằng ShapeRenderer (chưa cần bitmap font).</li>
 * </ul>
 */
public class GameRenderer implements Disposable {

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final OrthographicCamera camera;
    private final AssetRepository assets;

    private MapSnapshot cachedMap;
    private int lastMapHash = 0;

    public GameRenderer(AssetRepository assets) {
        this.assets = assets;
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(true, 800, 600); // y-down để khớp logic server
        this.batch = new SpriteBatch();
        this.shapes = new ShapeRenderer();
        // Camera dùng hệ tọa độ y-down, vì vậy font cũng phải flip theo y-down.
        // Font mặc định không có đầy đủ glyph tiếng Việt; overlay dùng ASCII để
        // tránh ô vuông trên cả desktop và GWT HTML5.
        this.font = new BitmapFont(true);
    }

    public void resize(int width, int height) {
        camera.setToOrtho(true, width, height);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);
    }

    public void render(GameStateSnapshot state) {
        // Xóa màn hình
        Gdx.gl.glClearColor(0.12f, 0.13f, 0.16f, 1);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

        if (state == null) return;

        // Khi đang chuyển màn / lỗi tải: giữ world cũ (nếu có) và vẽ overlay transition.
        boolean transitioning = state.transitionStatus != null;

        // 1. Map
        if (state.map != null) {
            drawMap(state.map);
        }

        // 2. Tanks
        batch.begin();
        for (TankSnapshot tank : state.tanks) {
            drawTank(tank);
        }
        // 3. Bullets
        for (BulletSnapshot bullet : state.bullets) {
            drawBullet(bullet);
        }
        batch.end();

        // 4. Health bars (shape renderer)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (TankSnapshot tank : state.tanks) {
            drawHealthBar(tank);
        }
        shapes.end();

        if (transitioning) {
            drawTransitionOverlay(state.transitionStatus);
            return;
        }

        // 5. HUD (player HP + status + shop/weapon)
        drawHud(state);

        if ("VICTORY".equalsIgnoreCase(state.gameStatus)
            || "DEFEAT".equalsIgnoreCase(state.gameStatus)) {
            drawResultOverlay(state);
        }
    }

    /**
     * Overlay khi đang tải màn kế (LOADING) hoặc lỗi tải (LOAD_ERROR).
     * Giữ thông điệp ASCII (font mặc định thiếu glyph tiếng Việt).
     */
    private void drawTransitionOverlay(String transitionStatus) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        boolean error = "LOAD_ERROR".equalsIgnoreCase(transitionStatus);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0, 0, width, height);

        float panelWidth = Math.min(520f, width - 48f);
        float panelHeight = 160f;
        float panelX = (width - panelWidth) / 2f;
        float panelY = (height - panelHeight) / 2f;
        shapes.setColor(error ? 0.16f : 0.10f, 0.14f, 0.12f, 0.98f);
        shapes.rect(panelX, panelY, panelWidth, panelHeight);
        shapes.setColor(error ? 0.90f : 0.30f, error ? 0.25f : 0.55f, 0.15f, 1f);
        shapes.rect(panelX, panelY, panelWidth, 5f);
        shapes.end();

        font.getData().setScale(1.6f);
        font.setColor(error ? 1f : 0.85f, error ? 0.30f : 0.85f, error ? 0.15f : 0.55f, 1f);
        drawCentered(error ? "KHONG TAI DUOC MAN" : "DANG TAI MAN...", width / 2f, panelY + 95f);

        if (error) {
            font.getData().setScale(0.95f);
            font.setColor(0.75f, 0.78f, 0.82f, 1f);
            drawCentered("Nhan N / R de thu lai", width / 2f, panelY + 55f);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void drawMap(MapSnapshot map) {
        if (map.grid == null) return;
        if (cachedMap != map || lastMapHash != System.identityHashCode(map)) {
            cachedMap = map;
            lastMapHash = System.identityHashCode(map);
        }

        int ts = map.tileSize;
        batch.begin();
        for (int r = 0; r < map.height; r++) {
            for (int c = 0; c < map.width; c++) {
                int tile = map.grid[r][c];
                int px = c * ts;
                int py = r * ts;
                switch (tile) {
                    case 0: case 3: case 4: // GROUND / SPAWN_PLAYER / SPAWN_ENEMY
                        batch.draw(assets.groundFrame(r, c), px, py, ts, ts);
                        break;
                    case 1: // WALL
                        batch.draw(assets.wallFrame(r, c), px, py, ts, ts);
                        break;
                    case 2: // WATER/SPIKES overlay lên ground
                        batch.draw(assets.groundFrame(r, c), px, py, ts, ts);
                        batch.draw(assets.spikesFrame(r, c), px, py, ts, ts);
                        break;
                }
            }
        }
        batch.end();
    }

    private void drawTank(TankSnapshot tank) {
        if (!tank.alive || tank.hp <= 0) return;
        com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.tankFrame(tank.isPlayer, tank.rotation);
        if (frame != null) {
            batch.draw(frame, tank.x - 20, tank.y - 20, 40, 40);
        }
    }

    private void drawBullet(BulletSnapshot bullet) {
        if (!bullet.active) return;
        com.badlogic.gdx.graphics.Texture tex = assets.bullet();
        float w = 16, h = 16;
        batch.draw(tex,
            bullet.x - w / 2f, bullet.y - h / 2f,
            w / 2f, h / 2f,
            w, h,
            1f, 1f,
            bullet.direction - 90f,
            0, 0,
            tex.getWidth(), tex.getHeight(),
            false, true
        );
    }

    private void drawHealthBar(TankSnapshot tank) {
        if (!tank.alive || tank.hp <= 0) return;

        float barW = 36;
        float barH = 5;
        float bx = tank.x - barW / 2f;
        float by = tank.y - 30; // phía trên tank

        // Nền
        shapes.setColor(0, 0, 0, 0.6f);
        shapes.rect(bx - 1, by - 1, barW + 2, barH + 2);

        // Thanh máu
        float pct = MathUtils.clamp((float) tank.hp / tank.maxHp, 0, 1);
        if (pct > 0.6f) shapes.setColor(Color.GREEN);
        else if (pct > 0.3f) shapes.setColor(Color.YELLOW);
        else shapes.setColor(Color.RED);

        shapes.rect(bx, by, barW * pct, barH);
    }

    private void drawHud(GameStateSnapshot state) {
        TankSnapshot player = null;
        for (TankSnapshot t : state.tanks) {
            if (t.isPlayer) { player = t; break; }
        }
        int hp = player != null ? player.hp : 0;
        int maxHp = player != null ? player.maxHp : 100;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // 1. Nền panel HP (10, 10, 240, 45)
        shapes.setColor(0, 0, 0, 0.65f);
        shapes.rect(10, 10, 240, 45);

        // Thanh HP
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(22, 26, 216, 14);

        float pct = MathUtils.clamp((float) hp / maxHp, 0, 1);
        if (pct > 0.6f) shapes.setColor(Color.GREEN);
        else if (pct > 0.3f) shapes.setColor(Color.YELLOW);
        else shapes.setColor(Color.RED);
        shapes.rect(22, 26, 216 * pct, 14);
        drawHealthTicks(22, 26, 216, 14, 5);

        // 2. Nút [B] WORKSHOP (260, 10, 130, 45)
        shapes.setColor(0.12f, 0.16f, 0.22f, 0.85f);
        shapes.rect(260, 10, 130, 45);
        shapes.setColor(0.85f, 0.65f, 0.20f, 0.85f);
        shapes.rect(260, 10, 130, 2);
        shapes.rect(260, 53, 130, 2);
        shapes.rect(260, 10, 2, 45);
        shapes.rect(388, 10, 2, 45);

        // 3. Khung Vũ Khí hiện tại (400, 10, 95, 45)
        shapes.setColor(0.12f, 0.16f, 0.22f, 0.85f);
        shapes.rect(400, 10, 95, 45);
        shapes.setColor(0.30f, 0.45f, 0.65f, 0.85f);
        shapes.rect(400, 10, 95, 2);
        shapes.rect(400, 53, 95, 2);
        shapes.rect(400, 10, 2, 45);
        shapes.rect(493, 10, 2, 45);

        // 4. Khung Tiền & Sao (505, 10, 175, 45)
        shapes.setColor(0.10f, 0.12f, 0.15f, 0.85f);
        shapes.rect(505, 10, 175, 45);
        shapes.setColor(0.40f, 0.45f, 0.50f, 0.6f);
        shapes.rect(505, 10, 175, 2);
        shapes.rect(505, 53, 175, 2);
        shapes.rect(505, 10, 2, 45);
        shapes.rect(678, 10, 2, 45);
        shapes.end();

        // Vẽ Text & Icon trong batch
        batch.begin();
        // HP Label
        font.setColor(Color.WHITE);
        font.draw(batch, "HP: " + hp + "/" + maxHp, 22, 14);

        // Workshop text
        font.setColor(Color.GOLD);
        font.draw(batch, "[B] SHOP", 290, 20);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Workshop", 290, 36);

        // Weapon icon + ammo
        com.badlogic.gdx.graphics.g2d.TextureRegion icon = assets.getIcon(state.activeWeaponId);
        if (icon != null) {
            batch.draw(icon, 405, 14, 36, 36);
        }
        font.setColor(Color.WHITE);
        String ammoText = (state.activeWeaponId == 0) ? "inf" : ("x" + state.activeWeaponAmmo);
        font.draw(batch, ammoText, 446, 20);
        font.setColor(Color.GRAY);
        font.draw(batch, "[Q/E]", 446, 36);

        // Tiền & Sao
        font.setColor(Color.GOLD);
        font.draw(batch, "COINS: " + state.playerCoins, 515, 20);
        font.setColor(Color.CYAN);
        font.draw(batch, "STARS: " + state.playerStars, 515, 36);
        batch.end();
    }

    /** Vạch chia 5 mức (mỗi mức 20%) để thanh HP đọc nhanh hơn. */
    private void drawHealthTicks(float x, float y, float width, float height, int segments) {
        shapes.setColor(0f, 0f, 0f, 0.55f);
        for (int i = 1; i < segments; i++) {
            float tickX = x + width * i / segments;
            shapes.rect(tickX - 0.5f, y, 1f, height);
        }
    }

    private void drawResultOverlay(GameStateSnapshot state) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        boolean victory = "VICTORY".equalsIgnoreCase(state.gameStatus);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0, 0, width, height);

        float panelWidth = Math.min(520f, width - 48f);
        float panelHeight = 310f;
        float panelX = (width - panelWidth) / 2f;
        float panelY = (height - panelHeight) / 2f;
        shapes.setColor(victory ? 0.10f : 0.16f, 0.14f, 0.12f, 0.98f);
        shapes.rect(panelX, panelY, panelWidth, panelHeight);
        shapes.setColor(victory ? 0.95f : 0.90f, victory ? 0.76f : 0.25f, 0.15f, 1f);
        shapes.rect(panelX, panelY, panelWidth, 5f);
        shapes.end();

        float x = width / 2f;
        font.getData().setScale(2.0f);
        font.setColor(victory ? 1f : 1f, victory ? 0.78f : 0.30f, 0.12f, 1f);
        drawCentered(victory ? "CHIEN THANG!" : "THAT BAI", x, panelY + 52f);

        // DIEM
        font.getData().setScale(1.3f);
        font.setColor(1f, 0.92f, 0.40f, 1f);
        drawCentered("DIEM SO: " + state.score, x, panelY + 100f);

        // HP / AI / TIME
        font.getData().setScale(0.95f);
        font.setColor(0.80f, 0.83f, 0.88f, 1f);
        drawCentered("HP: " + Math.max(0, state.hpRemain)
                        + "   AI: " + Math.max(0, state.aiKills)
                        + "   TIME: " + Math.max(0, state.elapsedSeconds) + "s",
                x, panelY + 132f);

        // Reward (stars/coins) — chỉ khác 0 khi VICTORY.
        font.getData().setScale(1.05f);
        font.setColor(victory ? 1f : 0.55f, victory ? 0.85f : 0.55f, victory ? 0.30f : 0.58f, 1f);
        drawCentered("SAO: " + Math.max(0, state.starsEarned)
                        + "   VANG: " + Math.max(0, state.coinsEarned),
                x, panelY + 165f);

        // Hướng dẫn phím.
        font.getData().setScale(0.95f);
        font.setColor(0.75f, 0.78f, 0.82f, 1f);
        drawCentered(victory ? "Nhan N de sang man tiep theo" : "Nhan R de choi lai", x, panelY + 205f);
        drawCentered(victory ? "Nhan R de choi lai man nay" : "", x, panelY + 230f);
        font.setColor(Color.GOLD);
        drawCentered("Nhan B de vao Workshop / Cua Hang", x, panelY + 258f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    public void renderShop(com.tankzors.client.ui.ShopOverlay shopOverlay) {
        if (shopOverlay != null && shopOverlay.isVisible()) {
            shopOverlay.render(batch, shapes, font);
        }
    }

    private void drawCentered(String text, float centerX, float baselineY) {
        if (text == null || text.isEmpty()) return;
        GlyphLayout layout = new GlyphLayout(font, text);
        batch.begin();
        font.draw(batch, text, centerX - layout.width / 2f, baselineY);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}
