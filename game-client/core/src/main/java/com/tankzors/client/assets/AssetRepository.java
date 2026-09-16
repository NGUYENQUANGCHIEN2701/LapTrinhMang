package com.tankzors.client.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Load và cung cấp sprite cho {@code GameRenderer}.
 *
 * <p>Tương đương {@code SpriteManager.ts} của React client. Mỗi sprite
 * được chia thành các frame riêng (vd tank có 16 frame góc 22.5°).</p>
 *
 * <p>Phase 3 chỉ load các sprite chính: player_tank, enemy_tank, bullet,
 * ground, wall, water (spikes), bush (palms), explosion. HUD vẽ thủ công
 * bằng ShapeRenderer.</p>
 */
public class AssetRepository implements Disposable {

    // Sprites 2D
    private Texture playerTankSheet;
    private Texture enemyTankSheet;
    private Texture bulletSheet;
    private Texture groundSheet;
    private Texture wallSheet;
    private Texture spikesSheet;
    private Texture palmsSheet;
    private Texture explosionBigSheet;

    // SpriteRegions (chia theo frame)
    private TextureRegion[] playerTankFrames;
    private TextureRegion[] enemyTankFrames;
    private TextureRegion[] groundFrames;
    private TextureRegion[] wallFrames;
    private TextureRegion[] spikesFrames;
    private TextureRegion[] palmsFrames;

    public void loadAll() {
        playerTankSheet = new Texture(Gdx.files.internal("tanks/player_tank.png"));
        enemyTankSheet = new Texture(Gdx.files.internal("tanks/enemy_tank.png"));
        bulletSheet = new Texture(Gdx.files.internal("effects/bullet.png"));
        groundSheet = new Texture(Gdx.files.internal("map/ground.png"));
        wallSheet = new Texture(Gdx.files.internal("map/wall.png"));
        spikesSheet = new Texture(Gdx.files.internal("map/spikes.png"));
        palmsSheet = new Texture(Gdx.files.internal("map/palms.png"));
        explosionBigSheet = new Texture(Gdx.files.internal("effects/explosion_big.png"));

        // Tanks: 16 frame × 24px (theo Renderer.ts)
        playerTankFrames = split(playerTankSheet, 24, 24, 16);
        enemyTankFrames = split(enemyTankSheet, 24, 24, 16);

        // Ground: 5 frame × 20px
        groundFrames = split(groundSheet, 20, 20, 5);
        // Wall: 6 frame × 20px
        wallFrames = split(wallSheet, 20, 20, 6);
        // Spikes: 3 frame × 20px
        spikesFrames = split(spikesSheet, 20, 20, 3);
        // Palms: 5 frame × 88px
        palmsFrames = split(palmsSheet, 88, 88, 5);
    }

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

    public TextureRegion groundFrame(int row, int col) {
        return groundFrames[(row * 3 + col * 7) % groundFrames.length];
    }

    public TextureRegion wallFrame(int row, int col) {
        return wallFrames[(row + col) % wallFrames.length];
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

    @Override
    public void dispose() {
        if (playerTankSheet != null) playerTankSheet.dispose();
        if (enemyTankSheet != null) enemyTankSheet.dispose();
        if (bulletSheet != null) bulletSheet.dispose();
        if (groundSheet != null) groundSheet.dispose();
        if (wallSheet != null) wallSheet.dispose();
        if (spikesSheet != null) spikesSheet.dispose();
        if (palmsSheet != null) palmsSheet.dispose();
        if (explosionBigSheet != null) explosionBigSheet.dispose();
    }

    private static TextureRegion[] split(Texture sheet, int tileW, int tileH, int frameCount) {
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(sheet, i * tileW, 0, tileW, tileH);
        }
        return frames;
    }
}
