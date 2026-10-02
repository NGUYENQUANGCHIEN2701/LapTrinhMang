package com.tankzors.client.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Load và cung cấp sprite cho {@code GameRenderer}.
 *
 * <p>Mỗi sprite
 * được chia thành các frame riêng (vd tank có 16 frame góc 22.5°).</p>
 *
 * <p>Load các sprite chính: player_tank, enemy_tank, bullet,
 * ground, wall, water (spikes), bush (palms), explosion. HUD vẽ thủ công
 * bằng ShapeRenderer.</p>
 */
public class AssetRepository implements Disposable {

    // Sprites 2D
    private Texture playerTankSheet;
    private Texture enemyTankSheet;
    private Texture bulletSheet;
    private Texture smallExplosionSheet;
    private Texture plasmaExplosionSheet;
    private Texture bigPlasmaExplosionSheet;
    private Texture bigRedExplosionSheet;
    private Texture groundSheet;
    private Texture wallSheet;
    private Texture spikesSheet;
    private Texture palmsSheet;
    private Texture explosionBigSheet;
    private Texture powerupsSheet;
    private Texture buildingsSheet;
    private Texture triggersSheet;
    private Texture shieldRingsSheet;

    // Shop GUI Textures
    private Texture iconsSheet;
    private Texture billetsTexture;
    private Texture panelTexture;
    private Texture statMoneyTexture;
    private Texture statStarTexture;
    private TextureRegion[] iconRegions;

    // SpriteRegions (chia theo frame)
    private TextureRegion[] playerTankFrames;
    private TextureRegion[] enemyTankFrames;
    private TextureRegion[] groundFrames;
    private TextureRegion[] wallFrames;
    private TextureRegion[] spikesFrames;
    private TextureRegion[] palmsFrames;
    private TextureRegion defaultBulletFrame;
    private TextureRegion[] bulletFrames;
    private TextureRegion[] explosionFrames;
    private TextureRegion[] smallExplosionFrames;
    private TextureRegion[] plasmaExplosionFrames;
    private TextureRegion[] bigPlasmaExplosionFrames;
    private TextureRegion[] bigRedExplosionFrames;
    private TextureRegion[] powerupFrames;
    private TextureRegion[] buildingFrames;
    private TextureRegion[] triggerFrames;
    private TextureRegion[] shieldRingFrames;

    public void loadAll() {
        playerTankSheet = new Texture(Gdx.files.internal("tanks/player_tank.png"));
        enemyTankSheet = new Texture(Gdx.files.internal("tanks/enemy_tank.png"));
        bulletSheet = new Texture(Gdx.files.internal("effects/bullet.png"));
        smallExplosionSheet = new Texture(Gdx.files.internal("effects/small.png"));
        plasmaExplosionSheet = new Texture(Gdx.files.internal("effects/plasma.png"));
        bigPlasmaExplosionSheet = new Texture(Gdx.files.internal("effects/big_plasma.png"));
        bigRedExplosionSheet = new Texture(Gdx.files.internal("effects/big_red.png"));
        groundSheet = new Texture(Gdx.files.internal("map/ground.png"));
        wallSheet = new Texture(Gdx.files.internal("map/wall.png"));
        spikesSheet = new Texture(Gdx.files.internal("map/spikes.png"));
        palmsSheet = new Texture(Gdx.files.internal("map/palms.png"));
        explosionBigSheet = new Texture(Gdx.files.internal("effects/explosion_big.png"));
        powerupsSheet = new Texture(Gdx.files.internal("effects/powerups.png"));
        buildingsSheet = new Texture(Gdx.files.internal("map/buildings.png"));
        triggersSheet = new Texture(Gdx.files.internal("map/triggers.png"));
        if (Gdx.files.internal("effects/rings.png").exists()) {
            shieldRingsSheet = new Texture(Gdx.files.internal("effects/rings.png"));
        }

        // Tanks: 16 frame × 24px (theo Renderer.ts)
        // Camera renderer dùng y-down. Chỉ sprite tank có hướng cần đảo V để
        // frame 0=UP, 90=RIGHT, 180=DOWN, 270=LEFT khớp với logic/đạn.
        playerTankFrames = split(playerTankSheet, 24, 24, 16, true);
        enemyTankFrames = split(enemyTankSheet, 24, 24, 16, true);
        // Fallback nếu bộ icon shop bị thiếu.
        defaultBulletFrame = new TextureRegion(bulletSheet, 24, 0, 24, 24);
        defaultBulletFrame.flip(false, true);
        bulletFrames = split(bulletSheet, 24, 24, 23, true);
        smallExplosionFrames = split(smallExplosionSheet, 15, 15, 17, false);
        plasmaExplosionFrames = split(plasmaExplosionSheet, 15, 15, 13, false);
        bigPlasmaExplosionFrames = split(bigPlasmaExplosionSheet, 40, 40, 12, false);
        bigRedExplosionFrames = split(bigRedExplosionSheet, 40, 40, 12, false);

        // Ground: 5 frame × 20px
        groundFrames = split(groundSheet, 20, 20, 5, false);
        // Wall sheet gốc: gạch, bê tông trắng và cây/bụi.
        wallFrames = split(wallSheet, 20, 20, 18, false);
        // Spikes: 3 frame × 20px
        spikesFrames = split(spikesSheet, 20, 20, 3, false);
        // Palms: 5 frame × 88px
        palmsFrames = split(palmsSheet, 88, 88, 5, false);
        explosionFrames = split(explosionBigSheet, 35, 35, 28, false);
        powerupFrames = split(powerupsSheet, 20, 20, 20, false);
        buildingFrames = split(buildingsSheet, 20, 20, 6, false);
        triggerFrames = split(triggersSheet, 20, 20, 10, false);
        if (shieldRingsSheet != null) shieldRingFrames = split(shieldRingsSheet, 28, 28, 6, true);

        // Shop GUI
        if (Gdx.files.internal("gui/icons.png").exists()) {
            iconsSheet = new Texture(Gdx.files.internal("gui/icons.png"));
            iconRegions = new TextureRegion[40];
            for (int i = 0; i < 40; i++) {
                int col = i % 20;
                int row = i / 20;
                iconRegions[i] = new TextureRegion(iconsSheet, col * 100, row * 100, 100, 100);
                iconRegions[i].flip(false, true);
            }
            // Icon 0 là viên đạn cam có quầng sáng đúng với mẫu HUD/default.
            defaultBulletFrame = iconRegions[0];
        }
        if (Gdx.files.internal("gui/billets.png").exists()) {
            billetsTexture = new Texture(Gdx.files.internal("gui/billets.png"));
        }
        if (Gdx.files.internal("gui/panel.png").exists()) {
            panelTexture = new Texture(Gdx.files.internal("gui/panel.png"));
        }
        if (Gdx.files.internal("gui/stat_money.png").exists()) {
            statMoneyTexture = new Texture(Gdx.files.internal("gui/stat_money.png"));
        }
        if (Gdx.files.internal("gui/stat_star.png").exists()) {
            statStarTexture = new Texture(Gdx.files.internal("gui/stat_star.png"));
        }
    }

    public TextureRegion getIcon(int id) {
        if (iconRegions == null || iconRegions.length == 0) return null;
        if (id >= 0 && id < iconRegions.length) {
            return iconRegions[id];
        }
        if (id == -1) return iconRegions[17]; // ExChassis
        if (id == -2) return iconRegions[19]; // Extra Life
        return iconRegions[0];
    }

    public Texture getBillets() { return billetsTexture; }
    public Texture getPanel() { return panelTexture; }
    public Texture getStatMoney() { return statMoneyTexture; }
    public Texture getStatStar() { return statStarTexture; }

    public TextureRegion tankFrame(boolean isPlayer, float rotation) {
        // Renderer.ts cũ: frameIndex = (Math.floor((rotation + 11.25) / 22.5) + 8) % 16
        float normalized = ((rotation % 360f) + 360f) % 360f;
        int frame = (int) (Math.floor((normalized + 11.25) / 22.5) + 8) % 16;
        TextureRegion[] frames = isPlayer ? playerTankFrames : enemyTankFrames;
        if (frames == null || frames.length == 0) return null;
        return frames[frame % frames.length];
    }

    public Texture bullet() {
        return bulletSheet;
    }

    public TextureRegion defaultBulletFrame() { return defaultBulletFrame; }

    /** Frame layout follows Builds: four direction frames at offsets 0, 4, and 13. */
    public TextureRegion bulletFrame(int weaponType, float direction) {
        if (bulletFrames == null || bulletFrames.length == 0) return defaultBulletFrame;
        int normalized = ((int) Math.floor(((direction % 360 + 360) % 360 + 45) / 90)) % 4;
        int[] directionFrame = {2, 3, 0, 1}; // client UP/RIGHT/DOWN/LEFT -> Builds frames
        int offset = weaponType == 1 || weaponType == 3 ? 4 : weaponType == 4 || weaponType == 5 ? 13 : 0;
        int index = Math.min(bulletFrames.length - 1, offset + directionFrame[normalized]);
        return bulletFrames[index];
    }

    public TextureRegion explosionFrame(int weaponType, float progress) {
        TextureRegion[] frames = weaponType == 1 ? plasmaExplosionFrames
                : weaponType == 3 ? bigPlasmaExplosionFrames
                : weaponType == 4 || weaponType == 5 ? bigRedExplosionFrames
                : smallExplosionFrames;
        if (frames == null || frames.length == 0) return explosionFrame(progress);
        int index = Math.max(0, Math.min(frames.length - 1, (int) (progress * frames.length)));
        return frames[index];
    }

    public TextureRegion groundFrame(int row, int col) {
        return groundFrames[(row * 3 + col * 7) % groundFrames.length];
    }

    public TextureRegion wallFrame(int hitPoints) {
        return wallFrames[brownWallFrameIndex(hitPoints)];
    }

    /**
     * wall.png chứa sáu frame gạch nâu theo thứ tự nguyên vẹn → đổ nát.
     * Tường chịu bốn phát nên giữ bốn mốc rõ ràng, một mốc sau mỗi phát.
     */
    public static int brownWallFrameIndex(int hitPoints) {
        if (hitPoints >= 4) return 0;
        if (hitPoints == 3) return 2;
        if (hitPoints == 2) return 4;
        return 5;
    }

    public TextureRegion whiteWallFrame(int row, int col) {
        // Sprite 6..10 là nhóm bê tông/kim loại sáng; frame 11 đã là cỏ.
        return wallFrames[6 + (row + col) % 5];
    }

    public TextureRegion bushFrame(int row, int col) {
        // Sprite 11..17 là toàn bộ nhóm cây/cỏ xanh.
        return wallFrames[11 + (row * 2 + col * 3) % 7];
    }

    public TextureRegion pickupFrame(boolean repair) {
        return powerupFrames[repair ? 10 : 0];
    }

    /** Cờ vàng gốc trong buildings.png (frame 3). */
    public TextureRegion flagFrame() { return buildingFrames[3]; }

    /** Vòng spawn gốc: cặp đỏ ở 0..1, cặp xanh ở 2..3. */
    public TextureRegion spawnFrame(boolean player, int row, int col) {
        int firstFrame = player ? 2 : 0;
        return triggerFrames[firstFrame + ((row + col) & 1)];
    }

    public TextureRegion explosionFrame(float progress) {
        if (explosionFrames == null || explosionFrames.length == 0) return null;
        int index = Math.max(0, Math.min(explosionFrames.length - 1,
                (int) (progress * explosionFrames.length)));
        return explosionFrames[index];
    }

    public TextureRegion spikesFrame(int row, int col) {
        return spikesFrames[(row + col) % spikesFrames.length];
    }

    public TextureRegion palmsFrame(int row, int col) {
        return palmsFrames[(row * 2 + col * 3) % palmsFrames.length];
    }

    public Texture explosionBig() {
        return explosionBigSheet;
    }

    /** Blue energy-field ring frames copied from the reference Builds asset. */
    public TextureRegion shieldRingFrame() {
        if (shieldRingFrames == null || shieldRingFrames.length == 0) return null;
        // The source atlas uses its penultimate frame for a stable blue shield.
        return shieldRingFrames[Math.max(0, shieldRingFrames.length - 2)];
    }

    @Override
    public void dispose() {
        if (playerTankSheet != null) playerTankSheet.dispose();
        if (enemyTankSheet != null) enemyTankSheet.dispose();
        if (bulletSheet != null) bulletSheet.dispose();
        if (smallExplosionSheet != null) smallExplosionSheet.dispose();
        if (plasmaExplosionSheet != null) plasmaExplosionSheet.dispose();
        if (bigPlasmaExplosionSheet != null) bigPlasmaExplosionSheet.dispose();
        if (bigRedExplosionSheet != null) bigRedExplosionSheet.dispose();
        if (groundSheet != null) groundSheet.dispose();
        if (wallSheet != null) wallSheet.dispose();
        if (spikesSheet != null) spikesSheet.dispose();
        if (palmsSheet != null) palmsSheet.dispose();
        if (explosionBigSheet != null) explosionBigSheet.dispose();
        if (powerupsSheet != null) powerupsSheet.dispose();
        if (buildingsSheet != null) buildingsSheet.dispose();
        if (triggersSheet != null) triggersSheet.dispose();
        if (shieldRingsSheet != null) shieldRingsSheet.dispose();
        if (iconsSheet != null) iconsSheet.dispose();
        if (billetsTexture != null) billetsTexture.dispose();
        if (panelTexture != null) panelTexture.dispose();
        if (statMoneyTexture != null) statMoneyTexture.dispose();
        if (statStarTexture != null) statStarTexture.dispose();
    }

    private static TextureRegion[] split(Texture sheet, int tileW, int tileH, int frameCount, boolean flipY) {
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(sheet, i * tileW, 0, tileW, tileH);
            if (flipY) frames[i].flip(false, true);
        }
        return frames;
    }
}
