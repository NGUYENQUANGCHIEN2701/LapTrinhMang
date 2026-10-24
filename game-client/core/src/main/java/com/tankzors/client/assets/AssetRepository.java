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
    private Texture largeBuildingsSheet;
    private TextureRegion[] largeBuildingFrames;
    private Texture triggersSheet;
    private Texture shieldRingsSheet;
    private Texture turretSheet;
    private Texture minesSheet;
    private Texture aircraftSheet;
    private TextureRegion aircraftSprite, aircraftShadow;
    private Texture spawnOverlayTexture;

    // Shop GUI Textures
    private Texture iconsSheet;
    private com.badlogic.gdx.graphics.g2d.BitmapFont shopFont;
    private Texture shopDigits, shopBottom, shopFade;
    private Texture billetsTexture;
    private Texture billetTexture;
    private Texture panelTexture;
    private TextureRegion hudPlayerTankIcon;
    private TextureRegion hudEnemyTankIcon;
    private Texture statMoneyTexture;
    private Texture statStarTexture;
    private TextureRegion[] iconRegions;

    // SpriteRegions (chia theo frame)
    private TextureRegion[] playerTankFrames;
    private TextureRegion[] enemyTankFrames;
    private TextureRegion[] groundFrames;
    private TextureRegion[] wallFrames;
    private TextureRegion[] bushFrames;
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
    private TextureRegion[] turretFrames;
    private TextureRegion[] minesFrames;
    private TextureRegion[] buildingFrames;
    private TextureRegion campaignFlagFrame;
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
        largeBuildingsSheet = new Texture(Gdx.files.internal("map/big_builds.png"));
        largeBuildingFrames = split(largeBuildingsSheet, 40, 40, 2, false);
        buildingsSheet = new Texture(Gdx.files.internal("map/buildings.png"));
        triggersSheet = new Texture(Gdx.files.internal("map/triggers.png"));
        if (Gdx.files.internal("effects/rings.png").exists()) {
            shieldRingsSheet = new Texture(Gdx.files.internal("effects/rings.png"));
        }
        if (Gdx.files.internal("effects/overlay.png").exists()) {
            spawnOverlayTexture = new Texture(Gdx.files.internal("effects/overlay.png"));
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

        // Ground atlas gốc có 106 frame × 20px. Nạp toàn bộ để campaign_1 có
        // đúng cỏ, vệt bánh xe và hố đất theo mã tile của Builds.
        groundFrames = split(groundSheet, 20, 20,
                Math.max(1, groundSheet.getWidth() / 20), false);
        // Wall sheet gốc: gạch, bê tông trắng và cây/bụi.
        wallFrames = split(wallSheet, 20, 20, 18, false);
        // Builds dùng wall tile id 16..18 cho ba bụi cây tròn của map này.
        bushFrames = new TextureRegion[3];
        for (int i = 0; i < bushFrames.length; i++) {
            bushFrames[i] = new TextureRegion(wallFrames[15 + i]);
            bushFrames[i].flip(false, true);
        }
        // Spikes: 3 frame × 20px
        spikesFrames = split(spikesSheet, 20, 20, 3, false);
        // Palms: 5 frame × 88px
        palmsFrames = split(palmsSheet, 88, 88, 5, false);
        explosionFrames = split(explosionBigSheet, 35, 35, 28, false);
        powerupFrames = split(powerupsSheet, 20, 20, 20, false);
        buildingFrames = split(buildingsSheet, 20, 20, 6, false);
        campaignFlagFrame = new TextureRegion(buildingFrames[flagFrameIndex()]);
        campaignFlagFrame.flip(false, flagRequiresVerticalFlip());
        triggerFrames = split(triggersSheet, 20, 20, 10, false);
        if (shieldRingsSheet != null) shieldRingFrames = split(shieldRingsSheet, 28, 28, 6, true);
        aircraftSheet = new Texture(Gdx.files.internal("effects/aircrafts.png"));
        aircraftSprite = new TextureRegion(aircraftSheet, 0, 0, aircraftSheet.getWidth() / 2, aircraftSheet.getHeight());
        aircraftShadow = new TextureRegion(aircraftSheet, aircraftSheet.getWidth() / 2, 0, aircraftSheet.getWidth() / 2, aircraftSheet.getHeight());
        // The world camera is y-down; preserve the source sprite's nose toward (+x, -y).
        aircraftSprite.flip(false, true);
        aircraftShadow.flip(false, true);
        // Ụ súng Builds: 16 hướng 22x26; mines.png: 3 mìn + 2 khung thuốc nổ đang cháy ngòi.
        if (Gdx.files.internal("tanks/turret.png").exists()) {
            turretSheet = new Texture(Gdx.files.internal("tanks/turret.png"));
            turretFrames = split(turretSheet, 22, 26, 16, true);
        }
        if (Gdx.files.internal("effects/mines.png").exists()) {
            minesSheet = new Texture(Gdx.files.internal("effects/mines.png"));
            minesFrames = split(minesSheet, 20, 20, 5, false);
        }

        // Shop GUI
        shopFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("fonts/shop-font.fnt"), true);
        shopFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        shopDigits = new Texture(Gdx.files.internal("gui/digits.png"));
        shopBottom = new Texture(Gdx.files.internal("gui/menu_back_2.png"));
        shopFade = new Texture(Gdx.files.internal("gui/menu_pict_fade.png"));
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
        if (Gdx.files.internal("gui/billet.png").exists()) {
            billetTexture = new Texture(Gdx.files.internal("gui/billet.png"));
        }
        if (Gdx.files.internal("gui/panel.png").exists()) {
            panelTexture = new Texture(Gdx.files.internal("gui/panel.png"));
            // Native top-facing counter icons, separate from the side-view shop icons.
            hudPlayerTankIcon = new TextureRegion(panelTexture, 264, 160, 48, 48);
            hudEnemyTankIcon = new TextureRegion(panelTexture, 264, 230, 48, 48);
            hudPlayerTankIcon.flip(false, true);
            hudEnemyTankIcon.flip(false, true);
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
        if (id == -1) return iconRegions[25]; // ExChassis: biểu tượng thân xe tăng (Frame 25)
        if (id == -2) return iconRegions[36]; // Extra Life: biểu tượng mạng sống đỏ (Frame 36)
        return iconRegions[0];
    }

    public com.badlogic.gdx.graphics.g2d.BitmapFont getShopFont() { return shopFont; }
    public Texture getShopDigits() { return shopDigits; }
    public Texture getShopBottom() { return shopBottom; }
    public Texture getShopFade() { return shopFade; }
    public Texture getBillets() { return billetsTexture; }
    public Texture getBillet() { return billetTexture; }
    public Texture getPanel() { return panelTexture; }
    public TextureRegion getHudTankIcon(boolean player) { return player ? hudPlayerTankIcon : hudEnemyTankIcon; }
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
        int offset = weaponType == 1 || weaponType == 3 ? 4
                : weaponType == 6 || weaponType == 7 || weaponType == 8 ? 8
                : weaponType == 4 || weaponType == 5 ? 13 : 0;
        int index = Math.min(bulletFrames.length - 1, offset + directionFrame[normalized]);
        return bulletFrames[index];
    }

    public TextureRegion artilleryShadowFrame() {
        if (bulletFrames == null || bulletFrames.length <= 12) return defaultBulletFrame;
        return bulletFrames[12];
    }

    /** Frame 17 trong bullets.png: Hồng tâm ngắm mục tiêu (Target Reticle) màu đỏ. */
    public TextureRegion targetReticleFrame() {
        if (bulletFrames == null || bulletFrames.length <= 17) return defaultBulletFrame;
        return bulletFrames[17];
    }

    public TextureRegion artilleryPartFrame(int partIndex) {
        if (bulletFrames == null || bulletFrames.length <= 22) return defaultBulletFrame;
        int frame = 22 - Math.max(0, Math.min(3, partIndex));
        return bulletFrames[frame];
    }

    public TextureRegion smallExplosionFrame(float progress) {
        if (smallExplosionFrames == null || smallExplosionFrames.length == 0) return null;
        int index = Math.max(0, Math.min(smallExplosionFrames.length - 1, (int) (progress * smallExplosionFrames.length)));
        return smallExplosionFrames[index];
    }

    public TextureRegion bigExplosionFrame(float progress) {
        if (explosionFrames == null || explosionFrames.length == 0) return null;
        int index = Math.max(0, Math.min(explosionFrames.length - 1, (int) (progress * explosionFrames.length)));
        return explosionFrames[index];
    }

    public TextureRegion explosionFrame(int weaponType, float progress) {
        TextureRegion[] frames = weaponType == 6 || weaponType == 7 || weaponType == 8 ? explosionFrames
                : weaponType == 1 ? plasmaExplosionFrames
                : weaponType == 3 ? bigPlasmaExplosionFrames
                : weaponType == 4 || weaponType == 5 ? bigRedExplosionFrames
                : smallExplosionFrames;
        if (frames == null || frames.length == 0) return explosionFrame(progress);
        int index = Math.max(0, Math.min(frames.length - 1, (int) (progress * frames.length)));
        return frames[index];
    }

    public TextureRegion groundFrame(int row, int col) {
        // Map schema mới không có visualGrid vẫn giữ tập năm frame cỏ cũ;
        // chỉ campaign có mã Builds mới được truy cập toàn bộ atlas.
        int fallbackFrameCount = Math.min(5, groundFrames.length);
        return groundFrames[(row * 3 + col * 7) % fallbackFrameCount];
    }

    public TextureRegion groundFrameById(int oneBasedTileId) {
        return groundFrames[frameIndexForTileId(oneBasedTileId, groundFrames.length)];
    }

    public TextureRegion wallFrame(int hitPoints) {
        return wallFrames[brownWallFrameIndex(hitPoints)];
    }

    public TextureRegion wallFrameById(int oneBasedTileId) {
        return wallFrames[frameIndexForTileId(oneBasedTileId, wallFrames.length)];
    }

    public static int frameIndexForTileId(int oneBasedTileId, int frameCount) {
        if (frameCount <= 1) return 0;
        return Math.max(0, Math.min(frameCount - 1, oneBasedTileId - 1));
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
        // Builds dùng đúng wall tile id 9 và 10 cho bê tông trắng.
        return wallFrames[8 + (row + col) % 2];
    }

    public TextureRegion bushFrame(int row, int col) {
        return bushFrames[(row * 2 + col * 3) % bushFrames.length];
    }

    public TextureRegion pickupFrame(boolean repair) {
        return powerupFrames[repair ? 10 : 0];
    }

    /**
     * powerups.png của Builds: mỗi loại 3 khung động theo thứ tự tăng tốc, mạng,
     * đóng băng, sửa xe, sao, xu; thùng đồ chiếm 2 khung cuối.
     */
    public TextureRegion powerupFrame(int tileCode, float animTime) {
        int base = powerupBaseFrame(tileCode);
        int frames = base == 18 ? 2 : 3;
        int index = base + ((int) (animTime * 6f)) % frames;
        return powerupFrames[Math.min(powerupFrames.length - 1, index)];
    }

    /** Khung đầu tiên trong powerups.png cho mã tile vật phẩm của client. */
    public static int powerupBaseFrame(int tileCode) {
        switch (tileCode) {
            case 8: return 0;   // tăng tốc
            case 14: return 3;  // mạng
            case 10: return 6;  // đóng băng
            case 7: return 9;   // sửa xe
            case 11: return 12; // sao
            case 12: return 15; // xu
            case 13: return 18; // thùng đồ
            default: return 0;
        }
    }

    public TextureRegion aircraftFrame() { return aircraftSprite; }
    public TextureRegion aircraftShadowFrame() { return aircraftShadow; }
    public TextureRegion mineFrame(int frame) {
        return minesFrames == null ? null : minesFrames[Math.max(0, Math.min(2, frame))];
    }
    /** Thuốc nổ đang cháy ngòi: hai khung cuối mines.png luân phiên. */
    public TextureRegion dynamiteFrame(float animTime) {
        if (minesFrames == null || minesFrames.length < 5) return null;
        return minesFrames[3 + ((int) (animTime * 8f)) % 2];
    }

    /** Cùng quy ước khung với xe tăng (Renderer.ts cũ). */
    public TextureRegion turretFrame(float rotation) {
        if (turretFrames == null || turretFrames.length == 0) return null;
        float normalized = ((rotation % 360f) + 360f) % 360f;
        int frame = (int) (Math.floor((normalized + 11.25) / 22.5) + 8) % 16;
        return turretFrames[frame % turretFrames.length];
    }

    /** Cờ vàng gốc trong buildings.png (frame 3), lật dọc riêng cho camera y-down. */
    public TextureRegion flagFrame() { return campaignFlagFrame; }

    public TextureRegion largeBuildingFrame(boolean reactor) { return largeBuildingFrames[reactor ? 1 : 0]; }

    public static int flagFrameIndex() { return 3; }

    public static boolean flagRequiresVerticalFlip() { return true; }

    /** Base gốc: frame sọc vàng cho phe người chơi, sọc đỏ cho phe AI. */
    public TextureRegion spawnBaseFrame(boolean player) {
        return buildingFrames[spawnBaseFrameIndex(player)];
    }

    public static int spawnBaseFrameIndex(boolean player) { return player ? 0 : 1; }

    /** Vòng phe gốc trong rings.png: team 1 xanh, team 2 đỏ. */
    public TextureRegion teamRingFrame(boolean player) {
        if (shieldRingFrames == null || shieldRingFrames.length < 3) return null;
        return shieldRingFrames[teamRingFrameIndex(player)];
    }

    public static int teamRingFrameIndex(boolean player) { return player ? 1 : 2; }

    public Texture spawnOverlay() { return spawnOverlayTexture; }

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
        if (largeBuildingsSheet != null) largeBuildingsSheet.dispose();
        if (triggersSheet != null) triggersSheet.dispose();
        if (shieldRingsSheet != null) shieldRingsSheet.dispose();
        if (turretSheet != null) turretSheet.dispose();
        if (minesSheet != null) minesSheet.dispose();
        if (aircraftSheet != null) aircraftSheet.dispose();
        if (spawnOverlayTexture != null) spawnOverlayTexture.dispose();
        if (iconsSheet != null) iconsSheet.dispose();
        if (shopFont != null) shopFont.dispose();
        if (shopDigits != null) shopDigits.dispose();
        if (shopBottom != null) shopBottom.dispose();
        if (shopFade != null) shopFade.dispose();
        if (billetsTexture != null) billetsTexture.dispose();
        if (billetTexture != null) billetTexture.dispose();
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
