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
import com.tankzors.client.model.EffectSnapshot;

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
    private final OrthographicCamera worldCamera;
    private final OrthographicCamera hudCamera;
    private final AssetRepository assets;

    private MapSnapshot cachedMap;
    private int lastMapHash = 0;
    private float animTime = 0f;

    public GameRenderer(AssetRepository assets) {
        this.assets = assets;
        this.worldCamera = new OrthographicCamera();
        this.worldCamera.setToOrtho(true, 800, 600); // y-down để khớp logic gameplay
        this.hudCamera = new OrthographicCamera();
        this.hudCamera.setToOrtho(true, 800, 600);
        this.batch = new SpriteBatch();
        this.shapes = new ShapeRenderer();
        // Camera dùng hệ tọa độ y-down, vì vậy font cũng phải flip theo y-down.
        // Font mặc định không có đầy đủ glyph tiếng Việt; overlay dùng ASCII để
        // tránh ô vuông trên cả desktop và GWT HTML5.
        this.font = new BitmapFont(true);
    }

    public void resize(int width, int height) {
        int viewportWidth = Math.max(1, width);
        int viewportHeight = Math.max(1, height);
        worldCamera.setToOrtho(true, viewportWidth, viewportHeight);
        hudCamera.setToOrtho(true, viewportWidth, viewportHeight);
        worldCamera.update();
        hudCamera.update();
    }

    public void render(GameStateSnapshot state) {
        // Xóa màn hình
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

        if (state == null) return;
        animTime += Gdx.graphics.getDeltaTime();

        updateWorldCamera(state);
        useWorldProjection();

        // Khi đang chuyển màn / lỗi tải: giữ world cũ (nếu có) và vẽ overlay transition.
        boolean transitioning = state.transitionStatus != null;

        VisibilityMask visibility = createVisibility(state);

        // 1. Map
        if (state.map != null) {
            drawMap(state.map, visibility);
        }

        // 2. Tanks
        batch.begin();
        for (TankSnapshot tank : state.tanks) {
            drawTank(tank);
        }
        // 2.5 Aim preview khi trang bị Artillery
        drawArtilleryAimPreview(state);
        // 3. Bullets
        for (BulletSnapshot bullet : state.bullets) {
            if (isVisible(visibility, state.map, bullet.x, bullet.y)) drawBullet(bullet);
        }
        for (EffectSnapshot effect : state.effects) {
            if (isVisible(visibility, state.map, effect.x, effect.y)) drawEffect(effect);
        }
        batch.end();

        // 4. Health bars (shape renderer)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (TankSnapshot tank : state.tanks) {
            drawHealthBar(tank);
        }
        shapes.end();

        // HUD/menu/overlay dùng camera màn hình riêng nên luôn đứng yên trong
        // khung game, không trôi theo map khi world camera theo người chơi.
        useHudProjection();

        if (transitioning) {
            drawTransitionOverlay(state.transitionStatus);
            return;
        }

        // 5. HUD (player HP + status + shop/weapon)
        drawHud(state);

        if (state.inventoryMenuOpen) drawInventoryMenu(state);

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

    private VisibilityMask createVisibility(GameStateSnapshot state) {
        if (state.map == null) return null;
        // Radar in Builds reveals the whole operational map, including enemies
        // and bonuses. The normal fog mask remains active without a radar.
        if (state.radarActive) return null;
        TankSnapshot player = null;
        for (TankSnapshot tank : state.tanks) if (tank.isPlayer) { player = tank; break; }
        int col = player != null ? (int) (player.x / state.map.tileSize) : state.map.width / 2;
        int row = player != null ? (int) (player.y / state.map.tileSize) : state.map.height / 2;
        return VisibilityMask.coveringTwoThirds(state.map.width, state.map.height, col, row);
    }

    private boolean isVisible(VisibilityMask mask, MapSnapshot map, float x, float y) {
        return mask == null || map == null || mask.isPixelVisible(x, y, map.tileSize);
    }

    private void drawMap(MapSnapshot map, VisibilityMask visibility) {
        if (map.grid == null) return;
        if (cachedMap != map || lastMapHash != System.identityHashCode(map)) {
            cachedMap = map;
            lastMapHash = System.identityHashCode(map);
        }

        int ts = map.tileSize;
        batch.begin();
        for (int r = 0; r < map.height; r++) {
            for (int c = 0; c < map.width; c++) {
                if (visibility != null && !visibility.isVisible(c, r)) continue;
                int tile = map.grid[r][c];
                int visualTile = visualTileAt(map, r, c);
                int px = c * ts;
                int py = r * ts;
                if (visualTile > 0) {
                    batch.draw(assets.groundFrameById(legacyGroundTileId(visualTile)), px, py, ts, ts);
                } else {
                    batch.draw(assets.groundFrame(r, c), px, py, ts, ts);
                }
                switch (tile) {
                    case 0: // GROUND
                        break;
                    case 3: case 4: // SPAWN_PLAYER xanh / SPAWN_ENEMY đỏ
                        batch.draw(assets.spawnBaseFrame(tile == 3), px, py, ts, ts);
                        break;
                    case 1: // WALL
                        int hitPoints = wallHitPointsAt(map, r, c);
                        int legacyWall = legacyWallTileId(visualTile);
                        batch.draw(hitPoints >= 4 && legacyWall >= 1 && legacyWall <= 8
                                        ? assets.wallFrameById(legacyWall)
                                        : assets.wallFrame(hitPoints),
                                px, py, ts, ts);
                        break;
                    case 2: // WATER/SPIKES overlay lên ground
                        batch.draw(assets.spikesFrame(r, c), px, py, ts, ts);
                        break;
                    case 5: // bê tông trắng, không thể phá
                        int legacyConcrete = legacyWallTileId(visualTile);
                        batch.draw(legacyConcrete >= 9 && legacyConcrete <= 10
                                        ? assets.wallFrameById(legacyConcrete)
                                        : assets.whiteWallFrame(r, c),
                                px, py, ts, ts);
                        break;
                    case 6: // bụi/cỏ xanh, vỡ sau 1 hit
                        batch.draw(assets.bushFrame(r, c), px, py, ts, ts);
                        break;
                    case 7: case 8: // pickup trên nền
                        batch.draw(assets.pickupFrame(tile == 7), px + 6, py + 6, ts - 12, ts - 12);
                        break;
                    case 9: // cờ đích của campaign_1
                        batch.draw(assets.flagFrame(), px, py, ts, ts);
                        break;
                }
            }
        }
        drawSpawnOverlays(map, visibility, ts);
        batch.end();
    }

    private int visualTileAt(MapSnapshot map, int row, int col) {
        if (map.visualGrid == null || row < 0 || row >= map.visualGrid.length
                || map.visualGrid[row] == null || col < 0 || col >= map.visualGrid[row].length) {
            return 0;
        }
        return map.visualGrid[row][col];
    }

    /** Chuyển mã map_arr Builds thành tile nền 1-based trong ground.png. */
    static int legacyGroundTileId(int sourceTile) {
        if (sourceTile >= 1 && sourceTile <= 90) return sourceTile;
        if (sourceTile > 249) return Math.max(1, Math.min(106, sourceTile - 149));
        if (sourceTile >= 100 && sourceTile < 115) {
            return themeGround((sourceTile - 100) / 5);
        }
        if (sourceTile >= 115 && sourceTile < 130) {
            return themeGround((sourceTile - 115) / 5);
        }
        if (sourceTile >= 130 && sourceTile < 250) {
            return themeGround((sourceTile - 130) / 40);
        }
        return 1;
    }

    private static int themeGround(int theme) {
        if (theme <= 0) return 1;
        if (theme == 1) return 34;
        return 56;
    }

    /** Overlay wall id 1-based được đóng gói trong mã 130..249 của map cũ. */
    static int legacyWallTileId(int sourceTile) {
        if (sourceTile < 130 || sourceTile >= 250) return 0;
        return (sourceTile - 130) % 40 + 1;
    }

    private void updateWorldCamera(GameStateSnapshot state) {
        if (state.map == null) {
            worldCamera.position.set(worldCamera.viewportWidth / 2f,
                    worldCamera.viewportHeight / 2f, 0f);
            worldCamera.update();
            return;
        }

        float targetX = state.map.width * state.map.tileSize / 2f;
        float targetY = state.map.height * state.map.tileSize / 2f;
        for (TankSnapshot tank : state.tanks) {
            if (tank.isPlayer) {
                targetX = tank.x;
                targetY = tank.y;
                break;
            }
        }

        float mapWidth = state.map.width * state.map.tileSize;
        float mapHeight = state.map.height * state.map.tileSize;
        float cameraX = WorldCameraBounds.centerForAxis(
                targetX, mapWidth, worldCamera.viewportWidth);
        float cameraY = WorldCameraBounds.centerForAxis(
                targetY, mapHeight, worldCamera.viewportHeight);
        worldCamera.position.set(cameraX, cameraY, 0f);
        worldCamera.update();
    }

    private void useWorldProjection() {
        batch.setProjectionMatrix(worldCamera.combined);
        shapes.setProjectionMatrix(worldCamera.combined);
    }

    private void useHudProjection() {
        batch.setProjectionMatrix(hudCamera.combined);
        shapes.setProjectionMatrix(hudCamera.combined);
    }

    /** Overlay 100px được tint theo đúng game Base gốc và vẽ sau tilemap. */
    private void drawSpawnOverlays(MapSnapshot map, VisibilityMask visibility, int tileSize) {
        com.badlogic.gdx.graphics.Texture overlay = assets.spawnOverlay();
        if (overlay == null) return;
        for (int row = 0; row < map.height; row++) {
            for (int col = 0; col < map.width; col++) {
                if (visibility != null && !visibility.isVisible(col, row)) continue;
                int tile = map.grid[row][col];
                if (tile != 3 && tile != 4) continue;
                boolean player = tile == 3;
                batch.setColor(player ? 0.267f : 1f, player ? 1f : 0f,
                        0f, player ? 0.376f : 0.502f);
                float centerX = (col + 0.5f) * tileSize;
                float centerY = (row + 0.5f) * tileSize;
                batch.draw(overlay, centerX - 50f, centerY - 50f, 100f, 100f);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private int wallHitPointsAt(MapSnapshot map, int row, int col) {
        if (map.wallHitPoints == null || row < 0 || row >= map.wallHitPoints.length
                || map.wallHitPoints[row] == null || col < 0 || col >= map.wallHitPoints[row].length
                || map.wallHitPoints[row][col] <= 0) {
            // Snapshot cũ từ guest không có trường này: giữ frame nguyên vẹn.
            return 4;
        }
        return map.wallHitPoints[row][col];
    }

    private void drawTank(TankSnapshot tank) {
        if (!tank.alive || tank.hp <= 0) return;
        com.badlogic.gdx.graphics.g2d.TextureRegion teamRing = assets.teamRingFrame(tank.isPlayer);
        if (teamRing != null) {
            batch.draw(teamRing, tank.x - 26f, tank.y - 26f, 52f, 52f);
        }
        if (tank.maxShieldHp > 0) {
            com.badlogic.gdx.graphics.g2d.TextureRegion shield = assets.shieldRingFrame();
            if (shield != null) {
                batch.setColor(0.25f, 0.9f, 1f, tank.shieldHp > 0 ? 1f : 0.28f);
                batch.draw(shield, tank.x - 27, tank.y - 27, 54, 54);
                batch.setColor(Color.WHITE);
            }
        }
        com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.tankFrame(tank.isPlayer, tank.rotation);
        if (frame != null) {
            batch.draw(frame, tank.x - 20, tank.y - 20, 40, 40);
        }
    }

    private void drawArtilleryAimPreview(GameStateSnapshot state) {
        if (state.activeWeaponId != 6) return;
        TankSnapshot player = null;
        for (TankSnapshot tank : state.tanks) {
            if (tank.isPlayer && tank.alive && tank.hp > 0) {
                player = tank;
                break;
            }
        }
        if (player == null) return;

        int tileSize = cachedMap != null ? cachedMap.tileSize : 40;
        float aimDist = 4f * tileSize; // Tầm bắn chuẩn của Artillery là 4 tiles (160px)
        float rad = player.rotation * MathUtils.degreesToRadians;
        float dirX = MathUtils.sin(rad);
        float dirY = -MathUtils.cos(rad);

        float targetX = player.x + dirX * aimDist;
        float targetY = player.y + dirY * aimDist;

        // 1. Vẽ các chấm quỹ đạo ngắm (Aim trajectory dots) từ nòng xe tăng tới đích
        com.badlogic.gdx.graphics.g2d.TextureRegion dotFrame = assets.artilleryPartFrame(3);
        if (dotFrame != null) {
            float startDist = 24f;
            float step = 16f;
            for (float d = startDist; d < aimDist - 12f; d += step) {
                float dotX = player.x + dirX * d;
                float dotY = player.y + dirY * d;
                float dotAlpha = 0.40f + 0.20f * MathUtils.sin(animTime * 8f - d * 0.05f);
                batch.setColor(1f, 0.45f, 0.20f, dotAlpha);
                batch.draw(dotFrame, dotX - 4f, dotY - 4f, 8f, 8f);
            }
        }

        // 2. Vẽ Target Reticle (Frame 17 từ bullets.png) màu đỏ nhấp nháy tại điểm rơi dự kiến
        com.badlogic.gdx.graphics.g2d.TextureRegion reticle = assets.targetReticleFrame();
        if (reticle != null) {
            float pulse = 0.95f + 0.10f * MathUtils.sin(animTime * 8f);
            float rw = 32f * pulse;
            float rh = 32f * pulse;
            float alpha = 0.70f + 0.25f * MathUtils.sin(animTime * 10f);
            batch.setColor(1f, 0.20f, 0.20f, alpha);
            batch.draw(reticle, targetX - rw / 2f, targetY - rh / 2f, rw, rh);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBullet(BulletSnapshot bullet) {
        if (!bullet.active) return;
        if (bullet.weaponType == 6) {
            // Quỹ đạo Artillery: chuẩn Base Builds + Target Landing Reticle & 3D Perspective Floating Arc
            float maxDist = bullet.maxRange > 0 ? bullet.maxRange : 160f;
            float startX = (bullet.startX != 0f || bullet.startY != 0f) ? bullet.startX : bullet.x;
            float startY = (bullet.startX != 0f || bullet.startY != 0f) ? bullet.startY : bullet.y;
            float d = (float) Math.hypot(bullet.x - startX, bullet.y - startY);
            int tileSize = cachedMap != null ? cachedMap.tileSize : 40;

            float radians = bullet.direction * MathUtils.degreesToRadians;
            float dirX = MathUtils.sin(radians);
            float dirY = -MathUtils.cos(radians);

            // Tọa độ điểm rơi của quả pháo trên mặt đất
            float targetX = startX + dirX * maxDist;
            float targetY = startY + dirY * maxDist;

            // 1. Chỉ định điểm rơi: Target Reticle (Frame 17 từ bullets.png) nhấp nháy cảnh báo đỏ
            com.badlogic.gdx.graphics.g2d.TextureRegion reticle = assets.targetReticleFrame();
            if (reticle != null) {
                float progress = Math.min(1.0f, d / maxDist);
                float pulseFreq = 10f + progress * 8f; // Nhấp nháy nhanh dần khi đạn càng gần đích
                float pulse = 1.0f + 0.15f * MathUtils.sin(animTime * pulseFreq);
                float rw = 32f * pulse;
                float rh = 32f * pulse;
                float reticleAlpha = 0.75f + 0.25f * MathUtils.sin(animTime * pulseFreq);
                batch.setColor(1f, 0.15f, 0.15f, reticleAlpha);
                batch.draw(reticle, targetX - rw / 2f, targetY - rh / 2f, rw, rh);
                batch.setColor(Color.WHITE);
            }

            // 2. Bóng đạn trên mặt đất (Frame 12 từ bullets.png)
            com.badlogic.gdx.graphics.g2d.TextureRegion shadow = assets.artilleryShadowFrame();
            if (shadow != null) {
                // Tỷ lệ độ cao từ 0.0 (mặt đất) đến 1.0 (đỉnh parabol)
                float currentHeightRatio = (float) Math.sin(Math.min(1.0f, d / maxDist) * Math.PI);
                // Khi lên cao bóng co nhỏ (scale 0.72x) và mờ dần (alpha 0.40f); khi sắp chạm đất bóng to đậm lại
                float shadowScale = 1.0f - 0.28f * currentHeightRatio;
                float shadowAlpha = 0.85f - 0.45f * currentHeightRatio;
                batch.setColor(1f, 1f, 1f, shadowAlpha);
                float sw = 24f * shadowScale;
                float sh = 24f * shadowScale;
                batch.draw(shadow, bullet.x - sw / 2f, bullet.y - sh / 2f, sw, sh);
                batch.setColor(Color.WHITE);
            }

            // 3. Quả pháo và vệt khói lửa lơ lửng trên không:
            // Đỉnh parabol nâng cao lên tileSize * 2.6f (~104px) để tạo cảm giác bay bổng rõ rệt
            float maxHeight = tileSize * 2.6f;
            int partCount = 5;
            float[] alphas = { 1.0f, 0.90f, 0.75f, 0.60f, 0.45f };
            float[] delaySteps = { 0f, 8f, 16f, 24f, 32f };

            for (int i = 0; i < partCount; i++) {
                float delay = delaySteps[i];
                if (d < delay) break;
                float partX = bullet.x - dirX * delay;
                float partY = bullet.y - dirY * delay;
                float partD = Math.max(0f, d - delay);
                if (partD > maxDist) continue;

                // Vòng cung parabol sin: đỉnh cao nhất ở giữa quãng đường
                float heightRatio = (float) Math.sin(Math.min(1.0f, partD / maxDist) * Math.PI);
                float offY = heightRatio * maxHeight;

                // Phối cảnh 3D giả lập: đạn ở đỉnh cao phóng to (lên tới 1.70x ~41px) để nhìn rõ chi tiết trên không
                float baseScale = (i == 0) ? 1.20f : (1.0f - i * 0.08f);
                float scale = baseScale + 0.50f * heightRatio;
                float pw = 24f * scale;
                float ph = 24f * scale;

                com.badlogic.gdx.graphics.g2d.TextureRegion partFrame = assets.artilleryPartFrame(i);
                if (partFrame != null) {
                    batch.setColor(1f, 1f, 1f, alphas[i]);
                    // camera y-down: trừ offY để nâng vật thể bay lên cao!
                    batch.draw(partFrame, partX - pw / 2f, (partY - offY) - ph / 2f, pw, ph);
                }
            }
            batch.setColor(Color.WHITE);
            return;
        }

        com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.bulletFrame(bullet.weaponType, bullet.direction);
        if (frame == null) return;
        // Builds tạo một Bullet cho Double Shell/Double Plasma, rồi vẽ hai
        // sprite cách nhau 3 px theo phương vuông góc đường bay — không tạo hai
        // projectile độc lập. Khi va chạm làm damage còn 1, sprite chuyển mờ.
        if (bullet.weaponType == 2 || bullet.weaponType == 3) {
            float radians = bullet.direction * MathUtils.degreesToRadians;
            float perpendicularX = -MathUtils.cos(radians) * 3f;
            float perpendicularY = MathUtils.sin(radians) * 3f;
            if (bullet.damage == 1) batch.setColor(1f, 1f, 1f, 160f / 255f);
            drawBulletFrame(frame, bullet.x + perpendicularX, bullet.y + perpendicularY);
            drawBulletFrame(frame, bullet.x - perpendicularX, bullet.y - perpendicularY);
            batch.setColor(Color.WHITE);
            return;
        }
        drawBulletFrame(frame, bullet.x, bullet.y);
    }

    private void drawBulletFrame(com.badlogic.gdx.graphics.g2d.TextureRegion frame, float x, float y) {
        float w = 24, h = 24;
        batch.draw(frame,
            x - w / 2f, y - h / 2f,
            w / 2f, h / 2f,
            w, h,
            1f, 1f,
            0f
        );
    }

    private void drawEffect(EffectSnapshot effect) {
        if (effect.weaponType == 6 && effect.blastRadius == 0) {
            // Nổ lớn từng ô do Artillery tạo ra (big.png / explosion_big)
            com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.bigExplosionFrame(effect.progress);
            if (frame != null) {
                float size = 44f;
                batch.draw(frame, effect.x - size / 2f, effect.y - size / 2f, size, size);
            }
            return;
        }
        com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.explosionFrame(effect.weaponType, effect.progress);
        if (frame == null) return;
        float size = effect.blastRadius > 0 ? effect.blastRadius * 80f
                : effect.weaponType == 0 || effect.weaponType == 2 ? (effect.impact ? 20f : 26f)
                : effect.weaponType == 1 ? (effect.impact ? 22f : 30f)
                : 48f;
        batch.draw(frame, effect.x - size / 2f, effect.y - size / 2f, size, size);
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
        shapes.setColor(tank.isPlayer ? Color.GREEN : Color.RED);

        shapes.rect(bx, by, barW * pct, barH);
        if (tank.maxShieldHp > 0) {
            float shieldPct = MathUtils.clamp((float) tank.shieldHp / tank.maxShieldHp, 0, 1);
            shapes.setColor(0.1f, 0.85f, 1f, 0.95f);
            shapes.rect(bx, by - 3, barW * shieldPct, 2);
        }
    }

    private void drawHud(GameStateSnapshot state) {
        TankSnapshot player = null;
        for (TankSnapshot t : state.tanks) {
            if (t.isPlayer) { player = t; break; }
        }
        int hp = player != null ? player.hp : 0;
        int maxHp = player != null ? player.maxHp : 100;
        int shieldHp = player != null ? player.shieldHp : 0;
        int maxShieldHp = player != null ? player.maxShieldHp : 0;

        int inventoryCount = inventoryCount(state);
        float panelHeight = state.inventoryMenuOpen ? 48 + inventoryCount * 34 : 82;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Bình thường chỉ hiện HP và loại đang dùng; Q mở rộng ngay trong HUD này.
        shapes.setColor(0, 0, 0, 0.65f);
        shapes.rect(8, 8, 158, panelHeight);

        // Thanh HP
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(18, 30, 138, 12);

        float pct = MathUtils.clamp((float) hp / maxHp, 0, 1);
        shapes.setColor(Color.GREEN);
        shapes.rect(18, 30, 138 * pct, 12);
        drawHealthTicks(18, 30, 138, 12, 5);
        if (maxShieldHp > 0) {
            shapes.setColor(0.03f, 0.18f, 0.25f, 0.95f);
            shapes.rect(18, 44, 138, 4);
            float shieldPct = MathUtils.clamp((float) shieldHp / maxShieldHp, 0, 1);
            shapes.setColor(0.12f, 0.88f, 1f, 1f);
            shapes.rect(18, 44, 138 * shieldPct, 4);
        }
        shapes.end();

        // Vẽ Text & Icon trong batch.
        batch.begin();
        if (maxShieldHp > 0) {
            font.setColor(Color.CYAN);
            font.draw(batch, "SH " + shieldHp + "/" + maxShieldHp, 104, 14);
        }

        if (!state.inventoryMenuOpen) {
            drawInventoryItem(state.activeWeaponId, state.activeWeaponAmmo, 18, 50, false);
        }
        if (state.radarActive) {
            font.setColor(Color.CYAN);
            font.draw(batch, "RADAR ONLINE", 18, panelHeight - 8);
        }
        if (state.freezeSecondsRemaining > 0) {
            font.setColor(Color.CYAN);
            font.draw(batch, "FREEZE " + (int) Math.ceil(state.freezeSecondsRemaining) + "s", 18, panelHeight - 24);
        }
        batch.end();
    }

    private void drawInventoryMenu(GameStateSnapshot state) {
        int count = inventoryCount(state);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < count; i++) {
            if (i == state.inventorySelectionIndex) {
                shapes.setColor(0.42f, 0.42f, 0.42f, 0.95f);
                shapes.rect(14, 48 + i * 34, 104, 32);
            }
        }
        shapes.end();

        batch.begin();
        for (int i = 0; i < count; i++) {
            drawInventoryItem(state.inventoryItemIds[i], state.inventoryItemCounts[i],
                    18, 50 + i * 34, i == state.inventorySelectionIndex);
        }
        batch.end();
    }

    private int inventoryCount(GameStateSnapshot state) {
        if (state.inventoryItemIds == null || state.inventoryItemCounts == null) return 0;
        return Math.min(state.inventoryItemIds.length, state.inventoryItemCounts.length);
    }

    private void drawInventoryItem(int id, int count, float x, float y, boolean selected) {
        com.badlogic.gdx.graphics.g2d.TextureRegion icon = id == 0
                ? assets.defaultBulletFrame() : assets.getIcon(id);
        if (icon != null) batch.draw(icon, x, y, 30, 30);
        font.setColor(selected ? Color.WHITE : Color.LIGHT_GRAY);
        font.draw(batch, String.valueOf(count), x + 39, y + 8);
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
        drawCentered(victory ? "Nhan ESC de quay lai chon man" : "", x, panelY + 230f);
        font.setColor(Color.GOLD);
        drawCentered("Nhan B de vao Workshop / Cua Hang", x, panelY + 258f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    public void renderShop(com.tankzors.client.ui.ShopOverlay shopOverlay) {
        if (shopOverlay != null && shopOverlay.isVisible()) {
            useHudProjection();
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
