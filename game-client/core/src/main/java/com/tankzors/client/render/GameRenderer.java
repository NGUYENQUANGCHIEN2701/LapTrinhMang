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
    private final com.badlogic.gdx.graphics.g2d.TextureRegion[] hudDigits = new com.badlogic.gdx.graphics.g2d.TextureRegion[10];
    private final com.badlogic.gdx.graphics.g2d.TextureRegion hudCoin;
    private final com.badlogic.gdx.graphics.g2d.TextureRegion hudStar;
    private static final int[] HUD_DIGIT_X = {0, 72, 111, 179, 247, 319, 387, 460, 529, 601};
    private static final int[] HUD_DIGIT_WIDTH = {72, 39, 68, 68, 72, 68, 73, 69, 72, 72};

    private MapSnapshot cachedMap;
    private int lastMapHash = 0;
    private float animTime = 0f;
    private final RadarOverlay radarOverlay = new RadarOverlay();
    private final TrackMarks trackMarks = new TrackMarks();
    private final com.tankzors.client.ui.VictoryOverlay victoryOverlay = new com.tankzors.client.ui.VictoryOverlay();
    private final com.tankzors.client.ui.DefeatOverlay defeatOverlay = new com.tankzors.client.ui.DefeatOverlay();

    public int defeatKey(int key) { return defeatOverlay.key(key); }
    public int defeatTouch(float x, float y) { return defeatOverlay.touch(x, y); }

    public int victoryKey(int key) { return victoryOverlay.key(key); }
    public int victoryTouch(float x, float y) { return victoryOverlay.touch(x, y); }
    public void victoryScroll(float amount) { victoryOverlay.scroll(amount); }
    private VisibilityMask cachedVisibility;
    private int cachedVisibilityCol;
    private int cachedVisibilityRow;
    private int cachedVisibilityWidth;
    private int cachedVisibilityHeight;
    private final com.badlogic.gdx.graphics.Texture whitePixel;

    /** Khoảng cách từ tâm tank tới mỗi dải xích, và kích thước một vạch xích. */
    private static final float TRACK_OFFSET = 12f;
    /** Chiều cao vùng world nhìn thấy, bằng canvas 800x600 gốc. */
    static final float WORLD_VIEW_HEIGHT = 600f;
    private static final float TRACK_DASH_LENGTH = 7f;
    private static final float TRACK_DASH_THICKNESS = 2f;

    public GameRenderer(AssetRepository assets) {
        this.assets = assets;
        for (int i = 0; i < hudDigits.length; i++) {
            hudDigits[i] = new com.badlogic.gdx.graphics.g2d.TextureRegion(assets.getShopDigits(), HUD_DIGIT_X[i], 0, HUD_DIGIT_WIDTH[i], 95);
            hudDigits[i].flip(false, true);
        }
        hudCoin = new com.badlogic.gdx.graphics.g2d.TextureRegion(assets.getStatMoney());
        hudStar = new com.badlogic.gdx.graphics.g2d.TextureRegion(assets.getStatStar());
        hudCoin.flip(false, true);
        hudStar.flip(false, true);
        this.worldCamera = new OrthographicCamera();
        this.worldCamera.setToOrtho(true, 800, 600); // y-down để khớp logic gameplay
        this.hudCamera = new OrthographicCamera();
        this.hudCamera.setToOrtho(true, 800, 600);
        this.batch = new SpriteBatch();
        this.shapes = new ShapeRenderer();
        // Camera dùng hệ tọa độ y-down, vì vậy font cũng phải flip theo y-down.
        this.font = new BitmapFont(Gdx.files.internal("fonts/game-font.fnt"), true);
        this.font.getRegion().getTexture().setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        com.badlogic.gdx.graphics.Pixmap pixmap =
                new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        this.whitePixel = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
    }

    public void resize(int width, int height) {
        int viewportWidth = Math.max(1, width);
        int viewportHeight = Math.max(1, height);
        worldCamera.setToOrtho(true, viewportWidth, viewportHeight);
        // Canvas co giãn theo trang/fullscreen nhưng luôn thấy cùng một vùng map
        // (WORLD_VIEW_HEIGHT đơn vị theo chiều cao) — phóng to thay vì lộ thêm map.
        worldCamera.zoom = WORLD_VIEW_HEIGHT / viewportHeight;
        hudCamera.setToOrtho(true, viewportWidth, viewportHeight);
        worldCamera.update();
        hudCamera.update();
    }

    public void render(GameStateSnapshot state) {
        // Xóa màn hình
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

        if (state == null) return;
        if (!"VICTORY".equalsIgnoreCase(state.gameStatus)) victoryOverlay.reset();
        if (!"DEFEAT".equalsIgnoreCase(state.gameStatus)) defeatOverlay.reset();
        float delta = Gdx.graphics.getDeltaTime();
        if (!state.radarActive) animTime += delta;

        updateWorldCamera(state);
        useWorldProjection();

        // Khi đang chuyển màn / lỗi tải: giữ world cũ (nếu có) và vẽ overlay transition.
        boolean transitioning = state.transitionStatus != null;

        VisibilityMask visibility = createVisibility(state);
        if (!state.radarActive) updateTrackMarks(state, transitioning, delta);

        // 1. Map
        if (state.map != null) {
            drawMap(state.map, visibility);
        }

        // 2. Tanks (vết xích nằm dưới thân xe)
        batch.begin();
        drawTrackMarks(state.map, visibility);
        for (EffectSnapshot effect : state.effects) {
            if ((effect.weaponType == 10 || effect.weaponType == 11) && isVisible(visibility, state.map, effect.x, effect.y)) drawEffect(effect);
        }
        // Theo lượt để giữ cùng texture: vòng đội → khiên → thân xe.
        for (com.tankzors.client.model.BuildingSnapshot building : state.buildings) {
            com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.largeBuildingFrame("reactor".equals(building.type));
            if (frame != null && building.hp > 0) {
                float size = state.map.tileSize * 2f;
                batch.draw(frame, building.col * state.map.tileSize, building.row * state.map.tileSize, size, size);
            }
        }
        for (TankSnapshot tank : state.tanks) drawTankRing(tank);
        for (TankSnapshot tank : state.tanks) drawTankShield(tank);
        for (TankSnapshot tank : state.tanks) drawTankBody(tank);
        // 2.5 Aim preview khi trang bị Artillery
        drawArtilleryAimPreview(state);
        if (state.airAimActive && state.map != null) {
            com.badlogic.gdx.graphics.g2d.TextureRegion reticle = assets.targetReticleFrame();
            float size = state.map.tileSize;
            if (reticle != null) batch.draw(reticle, state.airAimCol * size, state.airAimRow * size, size, size);
        }
        // 3. Bullets
        for (BulletSnapshot bullet : state.bullets) {
            if (isVisible(visibility, state.map, bullet.x, bullet.y)) drawBullet(bullet);
        }
        for (EffectSnapshot effect : state.effects) {
            if (effect.weaponType != 10 && effect.weaponType != 11 && isVisible(visibility, state.map, effect.x, effect.y)) drawEffect(effect);
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

        if (state.briefingOpen) {
            drawBriefing(state);
        } else if ("VICTORY".equalsIgnoreCase(state.gameStatus)) {
            drawVictoryBanner(state);
        } else if ("DEFEAT".equalsIgnoreCase(state.gameStatus)) {
            defeatOverlay.render(batch, shapes);
        } else if (state.hintText != null) {
            drawHintBar(state.hintText);
        }
        if (state.radarActive) radarOverlay.render(state, shapes, batch, font, assets, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void renderPersistence(String message) {
        if (message == null) return;
        useHudProjection();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, .9f);
        shapes.rect(0, Gdx.graphics.getHeight() - 36, Gdx.graphics.getWidth(), 36);
        shapes.end();
        batch.begin();
        com.badlogic.gdx.graphics.g2d.BitmapFont messageFont = assets.getShopFont();
        messageFont.getData().setScale(.38f);
        messageFont.setColor(Color.WHITE);
        messageFont.draw(batch, message, 12, Gdx.graphics.getHeight() - 28, Gdx.graphics.getWidth() - 24,
                com.badlogic.gdx.utils.Align.left, true);
        batch.end();
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
        drawCentered(error ? "KHÔNG TẢI ĐƯỢC MÀN" : "ĐANG TẢI MÀN...", width / 2f, panelY + 95f);

        if (error) {
            font.getData().setScale(0.95f);
            font.setColor(0.75f, 0.78f, 0.82f, 1f);
            drawCentered("Nhấn N / R để thử lại", width / 2f, panelY + 55f);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void updateTrackMarks(GameStateSnapshot state, boolean transitioning, float delta) {
        // Đang tải màn mới / chơi lại: xoá vết của màn cũ.
        if (transitioning) {
            trackMarks.clear();
            return;
        }
        trackMarks.beginFrame(delta);
        for (TankSnapshot tank : state.tanks) {
            if (tank.alive && tank.hp > 0) trackMarks.track(tank.id, tank.x, tank.y);
        }
        trackMarks.endFrame();
    }

    private void drawTrackMarks(MapSnapshot map, VisibilityMask visibility) {
        int count = trackMarks.size();
        for (int i = 0; i < count; i++) {
            float x = trackMarks.x(i);
            float y = trackMarks.y(i);
            if (!isVisible(visibility, map, x, y)) continue;
            batch.setColor(0.10f, 0.08f, 0.04f, 0.5f * trackMarks.opacity(i));
            if (trackMarks.isHorizontal(i)) {
                // Đi ngang: hai dải xích trên/dưới, mỗi vạch dựng đứng.
                float w = TRACK_DASH_THICKNESS;
                float h = TRACK_DASH_LENGTH;
                batch.draw(whitePixel, x - w / 2f, y - TRACK_OFFSET - h / 2f, w, h);
                batch.draw(whitePixel, x - w / 2f, y + TRACK_OFFSET - h / 2f, w, h);
            } else {
                // Đi dọc: hai dải xích trái/phải, mỗi vạch nằm ngang.
                float w = TRACK_DASH_LENGTH;
                float h = TRACK_DASH_THICKNESS;
                batch.draw(whitePixel, x - TRACK_OFFSET - w / 2f, y - h / 2f, w, h);
                batch.draw(whitePixel, x + TRACK_OFFSET - w / 2f, y - h / 2f, w, h);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private VisibilityMask createVisibility(GameStateSnapshot state) {
        if (state.map == null) return null;
        if ("DEFEAT".equalsIgnoreCase(state.gameStatus) && cachedVisibility != null
                && cachedVisibilityWidth == state.map.width && cachedVisibilityHeight == state.map.height) {
            return cachedVisibility;
        }
        TankSnapshot player = null;
        for (TankSnapshot tank : state.tanks) if (tank.isPlayer) { player = tank; break; }
        int col = player != null ? (int) (player.x / state.map.tileSize) : state.map.width / 2;
        int row = player != null ? (int) (player.y / state.map.tileSize) : state.map.height / 2;
        for (BulletSnapshot bullet : state.bullets) {
            if (bullet.weaponType == 8) {
                col = (int) (bullet.x / state.map.tileSize);
                row = (int) (bullet.y / state.map.tileSize);
                break;
            }
        }
        // Mask chỉ đổi khi xe sang ô khác; tránh sort lại toàn map mỗi frame.
        if (cachedVisibility == null || cachedVisibilityCol != col || cachedVisibilityRow != row
                || cachedVisibilityWidth != state.map.width || cachedVisibilityHeight != state.map.height) {
            cachedVisibility = VisibilityMask.coveringTwoThirds(state.map.width, state.map.height, col, row);
            cachedVisibilityCol = col;
            cachedVisibilityRow = row;
            cachedVisibilityWidth = state.map.width;
            cachedVisibilityHeight = state.map.height;
        }
        return cachedVisibility;
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
        // SpriteBatch flush mỗi lần đổi texture; vẽ xen kẽ nền/tường theo từng ô
        // tạo ra hàng trăm draw call mỗi frame (rất chậm trên WebGL/ANGLE).
        // Vì vậy vẽ theo lượt: toàn bộ nền trước, rồi từng nhóm cùng sprite sheet.
        for (int r = 0; r < map.height; r++) {
            for (int c = 0; c < map.width; c++) {
                if (visibility != null && !visibility.isVisible(c, r)) continue;
                int visualTile = visualTileAt(map, r, c);
                if (visualTile > 0) {
                    batch.draw(assets.groundFrameById(legacyGroundTileId(visualTile)), c * ts, r * ts, ts, ts);
                } else {
                    batch.draw(assets.groundFrame(r, c), c * ts, r * ts, ts, ts);
                }
            }
        }
        for (int[] pass : MAP_OVERLAY_PASSES) {
            for (int r = 0; r < map.height; r++) {
                for (int c = 0; c < map.width; c++) {
                    int tile = map.grid[r][c];
                    if (!passContains(pass, tile)) continue;
                    if (visibility != null && !visibility.isVisible(c, r)) continue;
                    drawTileOverlay(map, tile, r, c, ts);
                }
            }
        }
        drawSpawnOverlays(map, visibility, ts);
        batch.end();
    }

    /** Nhóm mã tile theo sprite sheet: tường/bê tông/bụi, gai, bệ spawn, vật phẩm, cờ. */
    private static final int[][] MAP_OVERLAY_PASSES = {{1, 5, 6, 15}, {2}, {3, 4}, {7, 8, 10, 11, 12, 13, 14}, {9}, {16}};

    private static boolean passContains(int[] pass, int tile) {
        for (int code : pass) if (code == tile) return true;
        return false;
    }

    private void drawTileOverlay(MapSnapshot map, int tile, int r, int c, int ts) {
        int px = c * ts;
        int py = r * ts;
        int visualTile = visualTileAt(map, r, c);
        switch (tile) {
            case 3: case 4: // SPAWN_PLAYER xanh / SPAWN_ENEMY đỏ
                batch.draw(assets.spawnBaseFrame(tile == 3), px, py, ts, ts);
                break;
            case 1: // WALL
                int hitPoints = wallHitPointsAt(map, r, c);
                int legacyWall = legacyWallTileId(visualTile);
                batch.draw(legacyWall >= 1 && legacyWall <= 8
                                ? assets.wallFrameById((4 - Math.max(1, Math.min(4, hitPoints))) * 2
                                        + (legacyWall - 1) % 2 + 1)
                                : assets.wallFrame(hitPoints),
                        px, py, ts, ts);
                break;
            case 2: // WATER/SPIKES overlay lên ground
                batch.draw(assets.spikesFrame(r, c), px, py, ts, ts);
                break;
            case 5: // bê tông trắng / tấm thép, không thể phá
                int legacyConcrete = legacyWallTileId(visualTile);
                batch.draw(legacyConcrete >= 9 && legacyConcrete <= 13 && legacyConcrete != 11
                                ? assets.wallFrameById(legacyConcrete)
                                : assets.whiteWallFrame(r, c),
                        px, py, ts, ts);
                break;
            case 6: // bụi/cỏ xanh, vỡ sau 1 hit
                batch.draw(assets.bushFrame(r, c), px, py, ts, ts);
                break;
            case 7: case 8: case 10: case 11: case 12: case 13: case 14: // vật phẩm Builds
                batch.draw(assets.powerupFrame(tile, animTime), px + 6, py + 6, ts - 12, ts - 12);
                break;
            case 15: // khối đá (tile 11 Builds): chỉ vụ nổ mới phá được
                batch.draw(assets.wallFrameById(11), px, py, ts, ts);
                break;
            case 16: // thuốc nổ đang cháy ngòi
                com.badlogic.gdx.graphics.g2d.TextureRegion dynamite = assets.dynamiteFrame(animTime);
                if (dynamite != null) batch.draw(dynamite, px, py, ts, ts);
                break;
            case 9: // cờ đích của campaign_1
                batch.draw(assets.flagFrame(), px, py, ts, ts);
                break;
        }
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
        if ("DEFEAT".equalsIgnoreCase(state.gameStatus) && cachedVisibility != null) return;
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

        if (state.airAimActive) {
            targetX = (state.airAimCol + .5f) * state.map.tileSize;
            targetY = (state.airAimRow + .5f) * state.map.tileSize;
        } else {
            for (BulletSnapshot bullet : state.bullets) {
                if (bullet.weaponType == 8) { targetX = bullet.x; targetY = bullet.y; break; }
            }
        }
        float mapWidth = state.map.width * state.map.tileSize;
        float mapHeight = state.map.height * state.map.tileSize;
        float cameraX = WorldCameraBounds.centerForAxis(
                targetX, mapWidth, worldCamera.viewportWidth * worldCamera.zoom);
        float cameraY = WorldCameraBounds.centerForAxis(
                targetY, mapHeight, worldCamera.viewportHeight * worldCamera.zoom);
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

    private void drawTankRing(TankSnapshot tank) {
        if (!tank.alive || tank.hp <= 0 || tank.turret) return;
        com.badlogic.gdx.graphics.g2d.TextureRegion teamRing = assets.teamRingFrame(tank.isPlayer || tank.ally);
        if (teamRing != null) {
            batch.draw(teamRing, tank.x - 26f, tank.y - 26f, 52f, 52f);
        }
    }

    private void drawTankShield(TankSnapshot tank) {
        if (!tank.alive || tank.hp <= 0) return;
        if (tank.maxShieldHp > 0) {
            com.badlogic.gdx.graphics.g2d.TextureRegion shield = assets.shieldRingFrame();
            if (shield != null) {
                batch.setColor(0.25f, 0.9f, 1f, tank.shieldHp > 0 ? 1f : 0.28f);
                batch.draw(shield, tank.x - 27, tank.y - 27, 54, 54);
                batch.setColor(Color.WHITE);
            }
        }
    }

    private void drawTankBody(TankSnapshot tank) {
        if (!tank.alive || tank.hp <= 0) return;
        if (tank.turret) {
            // Sprite ụ súng 22x26 cùng tỉ lệ phóng như xe (24px -> 40 đơn vị).
            com.badlogic.gdx.graphics.g2d.TextureRegion turret = assets.turretFrame(tank.rotation);
            if (turret != null) batch.draw(turret, tank.x - 18f, tank.y - 24f, 36f, 43f);
            return;
        }
        com.badlogic.gdx.graphics.g2d.TextureRegion frame = assets.tankFrame(tank.isPlayer || tank.ally, tank.rotation);
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
        float aimDist = state.artilleryRange * tileSize; // Tầm bắn chuẩn của Artillery là 4 tiles (160px)
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
            float distance = (float) Math.hypot(bullet.x - bullet.startX, bullet.y - bullet.startY);
            float range = bullet.maxRange > 0 ? bullet.maxRange : 160;
            float tileSize = cachedMap == null ? 40 : cachedMap.tileSize;
            float radians = bullet.direction * MathUtils.degreesToRadians;
            float dx = MathUtils.sin(radians), dy = -MathUtils.cos(radians);
            com.badlogic.gdx.graphics.g2d.TextureRegion shadow = assets.artilleryShadowFrame();
            if (shadow != null) drawBulletFrame(shadow, bullet.x, bullet.y);
            for (int i = 0; i < 4; i++) {
                float delay = i * tileSize / 4;
                if (distance < delay || distance - delay > range) continue;
                float height = MathUtils.sin((distance - delay) / range * MathUtils.PI) * tileSize * 2;
                com.badlogic.gdx.graphics.g2d.TextureRegion part = assets.artilleryPartFrame(i);
                if (part != null) drawBulletFrame(part, bullet.x - dx * delay, bullet.y - dy * delay - height);
            }
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
        if (effect.weaponType == -7) {
            com.badlogic.gdx.graphics.g2d.TextureRegion smoke = assets.smallExplosionFrame(.7f + effect.progress * .3f);
            if (smoke != null) {
                batch.setColor(1, 1, 1, 1 - effect.progress);
                batch.draw(smoke, effect.x - 12, effect.y - 12, 24, 24);
                batch.setColor(Color.WHITE);
            }
            return;
        }
        if (effect.weaponType == 10 || effect.weaponType == 11) {
            com.badlogic.gdx.graphics.g2d.TextureRegion mine = assets.mineFrame(effect.weaponType == 10 ? 1 : 2);
            if (mine != null) batch.draw(mine, effect.x - 20, effect.y - 20, 40, 40);
            return;
        }
        if (effect.weaponType == 9) {
            com.badlogic.gdx.graphics.g2d.TextureRegion aircraft = assets.aircraftFrame();
            float size = (cachedMap == null ? 40 : cachedMap.tileSize) * 4.2f;
            com.badlogic.gdx.graphics.g2d.TextureRegion shadow = assets.aircraftShadowFrame();
            if (shadow != null) batch.draw(shadow, effect.x - size / 2 + 12, effect.y - size / 2 + 12, size, size);
            if (aircraft != null) batch.draw(aircraft, effect.x - size / 2, effect.y - size / 2, size, size);
            return;
        }
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

        float barW = Math.max(36, Math.min(25, tank.maxHp) * 3);
        float barH = 5 * ((tank.maxHp + 24) / 25);
        float bx = tank.x - barW / 2f;
        // World camera uses y-down: positive Y places the bars below the hull.
        float by = tank.y + 26;
        drawHitPointSegments(bx, by, barW, barH, tank.hp, tank.maxHp,
                tank.isPlayer || tank.ally ? Color.GREEN : Color.RED, tank);
        if (tank.maxShieldHp > 0) {
            float shieldHeight = 4 * ((tank.maxShieldHp + 24) / 25);
            drawHitPointSegments(bx, by + barH + 2, barW, shieldHeight,
                    tank.shieldHp, tank.maxShieldHp, Color.CYAN);
        }
    }

    private void drawHud(GameStateSnapshot state) {
        TankSnapshot player = null;
        for (TankSnapshot t : state.tanks) {
            if (t.isPlayer) { player = t; break; }
        }
        int hp = player != null ? player.hp : 0;
        int maxHp = player != null ? player.maxHp : 0;
        int shieldHp = player != null ? player.shieldHp : 0;
        int maxShieldHp = player != null ? player.maxShieldHp : 0;

        int inventoryCount = inventoryCount(state);
        int visibleRows = Math.min(inventoryCount, Math.max(1, (Gdx.graphics.getHeight() - 64) / 34));
        float panelHeight = state.inventoryMenuOpen ? Math.min(Gdx.graphics.getHeight() - 16, 52 + visibleRows * 34) : 82;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Draw the bars directly over the world, without an opaque HUD panel.
        drawHitPointSegments(18, 12, 138, 10, hp, maxHp, Color.GREEN, player);
        if (maxShieldHp > 0) {
            drawHitPointSegments(18, 26, 138, 4, shieldHp, maxShieldHp, Color.CYAN);
        }
        shapes.end();

        // Vẽ Text & Icon trong batch.
        batch.begin();
        if (!state.inventoryMenuOpen) {
            com.badlogic.gdx.graphics.g2d.TextureRegion icon = state.activeWeaponId == 0
                    ? assets.defaultBulletFrame() : assets.getIcon(state.activeWeaponId);
            if (icon != null) batch.draw(icon, 18, 34, 32, 32);
            drawCounterNumber(state.activeWeaponAmmo, 112, 39);
        }
        if (state.freezeSecondsRemaining > 0) {
            font.setColor(Color.CYAN);
            font.draw(batch, "ĐÓNG BĂNG " + (int) Math.ceil(state.freezeSecondsRemaining) + "s", 18,
                    state.inventoryMenuOpen ? panelHeight - 24 : 76);
        }
        batch.end();
        drawBattleCounters(state);
    }

    /** Builds-style resource/lives counters anchored to the upper-right HUD. */
    private void drawBattleCounters(GameStateSnapshot state) {
        if (state.playerLives < 0) return;
        float right = Gdx.graphics.getWidth() - 12;
        batch.begin();
        drawCounterNumber(state.playerCoins, right - 34, 16);
        batch.draw(hudCoin, right - 32, 11, 34, 34);
        drawCounterNumber(state.playerStars, right - 34, 48);
        batch.draw(hudStar, right - 32, 43, 34, 34);
        drawCounterNumber(state.playerLives, right - 34, 80);
        drawTankCounterIcon(assets.getHudTankIcon(true), right - 28, 78);
        if (state.enemiesRemaining >= 0) {
            drawCounterNumber(state.enemiesRemaining, right - 34, 112);
            drawTankCounterIcon(assets.getHudTankIcon(false), right - 28, 110);
        }
        batch.end();
    }

    private void drawTankCounterIcon(com.badlogic.gdx.graphics.g2d.TextureRegion icon, float x, float y) {
        if (icon != null) batch.draw(icon, x, y, 26, 26);
    }

    private void drawCounterNumber(int value, float right, float y) {
        String text = String.valueOf(Math.max(0, value));
        float factor = 24f / 95f;
        float width = 0;
        for (int i = 0; i < text.length(); i++) width += (HUD_DIGIT_WIDTH[text.charAt(i) - '0'] - 3) * factor;
        float x = right - width;
        for (int i = 0; i < text.length(); i++) {
            int digit = text.charAt(i) - '0';
            batch.draw(hudDigits[digit], x, y, HUD_DIGIT_WIDTH[digit] * factor, 24);
            x += (HUD_DIGIT_WIDTH[digit] - 3) * factor;
        }
    }

    private void drawInventoryMenu(GameStateSnapshot state) {
        int count = inventoryCount(state);

        int first = inventoryFirstVisible(state);
        int end = Math.min(count, first + Math.max(1, (Gdx.graphics.getHeight() - 64) / 34));
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = first; i < end; i++) {
            if (i == state.inventorySelectionIndex) {
                shapes.setColor(0.42f, 0.42f, 0.42f, 0.95f);
                shapes.rect(14, 48 + (i - first) * 34, 104, 32);
            }
        }
        shapes.end();

        batch.begin();
        for (int i = first; i < end; i++) {
            drawInventoryItem(state.inventoryItemIds[i], state.inventoryItemCounts[i],
                    18, 50 + (i - first) * 34, i == state.inventorySelectionIndex);
        }
        batch.end();
        if (count > 0) drawInventoryInfo(state);
    }

    /** A persistent card follows the selected Q-inventory row. */
    private void drawInventoryInfo(GameStateSnapshot state) {
        if (state.inventorySelectedName == null || state.inventorySelectedName.isEmpty()) return;
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        boolean beside = screenW >= 340;
        float x = beside ? 172 : 8;
        float width = Math.min(320, screenW - x - 10);
        if (width <= 40) return;
        float textWidth = width - 24;
        font.getData().setScale(1f);
        String details = state.inventorySelectedDescription == null ? "" : state.inventorySelectedDescription;
        String usage = state.inventorySelectedUsage == null ? "" : state.inventorySelectedUsage;
        GlyphLayout detailsLayout = new GlyphLayout(font, details, Color.LIGHT_GRAY, textWidth,
                com.badlogic.gdx.utils.Align.left, true);
        GlyphLayout usageLayout = new GlyphLayout(font, usage, Color.WHITE, textWidth,
                com.badlogic.gdx.utils.Align.left, true);
        GlyphLayout footerLayout = new GlyphLayout(font, "WASD / Mũi tên: xem  |  Enter / Space / J: dùng",
                Color.GRAY, textWidth, com.badlogic.gdx.utils.Align.left, true);
        float usageHeadingOffset = 88 + detailsLayout.height;
        float usageOffset = usageHeadingOffset + 22;
        float footerOffset = usageOffset + usageLayout.height + 18;
        float height = footerOffset + footerLayout.height + 16;
        float y = beside ? Math.max(8, Math.min(48 + (state.inventorySelectionIndex - inventoryFirstVisible(state)) * 34,
                screenH - height - 12)) : Math.max(8, screenH - height - 12);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.08f, 0.11f, 0.95f);
        shapes.rect(x, y, width, height);
        shapes.setColor(0.85f, 0.65f, 0.20f, 1f);
        shapes.rect(x, y, 3, height);
        shapes.rect(x, y, width, 2);
        shapes.end();

        batch.begin();
        com.tankzors.client.shop.ShopItem selected = com.tankzors.client.shop.ShopItem.findById(
                state.inventoryItemIds[Math.max(0, Math.min(state.inventorySelectionIndex, inventoryCount(state) - 1))]);
        if (selected != null && assets.getIcon(selected.getId()) != null) {
            batch.draw(assets.getIcon(selected.getId()), x + 10, y + 8, 30, 30);
        }
        font.setColor(Color.GOLD);
        font.draw(batch, state.inventorySelectedName, x + 46, y + 15, width - 56,
                com.badlogic.gdx.utils.Align.left, true);
        font.setColor(Color.CYAN);
        font.draw(batch, "THÔNG SỐ VÀ TÁC DỤNG", x + 12, y + 50);
        font.draw(batch, detailsLayout, x + 12, y + 70);
        float usageY = y + usageHeadingOffset;
        font.setColor(Color.CYAN);
        font.draw(batch, "CÁCH SỬ DỤNG", x + 12, usageY);
        font.draw(batch, usageLayout, x + 12, y + usageOffset);
        font.setColor(Color.GRAY);
        font.draw(batch, footerLayout, x + 12, y + footerOffset);
        batch.end();
    }

    private int inventoryFirstVisible(GameStateSnapshot state) {
        int rows = Math.max(1, (Gdx.graphics.getHeight() - 64) / 34);
        return Math.max(0, state.inventorySelectionIndex - rows + 1);
    }

    private int inventoryCount(GameStateSnapshot state) {
        if (state.inventoryItemIds == null || state.inventoryItemCounts == null) return 0;
        return Math.min(state.inventoryItemIds.length, state.inventoryItemCounts.length);
    }

    private void drawInventoryItem(int id, int count, float x, float y, boolean selected) {
        com.badlogic.gdx.graphics.g2d.TextureRegion icon = id == 0
                ? assets.defaultBulletFrame() : assets.getIcon(id);
        if (icon != null) batch.draw(icon, x, y, 30, 30);
        font.getData().setScale(1f);
        font.setColor(selected ? Color.WHITE : Color.LIGHT_GRAY);
        font.draw(batch, String.valueOf(count), x + 39, y + 8);
    }

    /** One filled cell = one HP; damage consumes shield cells before hull cells. */
    private void drawHitPointSegments(float x, float y, float width, float height,
                                      int hp, int maxHp, Color color) {
        drawHitPointSegments(x, y, width, height, hp, maxHp, color, null);
    }

    private void drawHitPointSegments(float x, float y, float width, float height,
                                      int hp, int maxHp, Color color, TankSnapshot hull) {
        if (maxHp <= 0) return;
        int columns = Math.min(25, maxHp);
        int rows = (maxHp + columns - 1) / columns;
        float cellWidth = width / columns;
        float cellHeight = height / rows;
        float gapX = Math.min(1f, cellWidth * 0.25f);
        float gapY = Math.min(1f, cellHeight * 0.25f);
        shapes.setColor(Color.BLACK);
        shapes.rect(x - 1, y - 1, width + 2, height + 2);
        for (int i = 0; i < maxHp; i++) {
            boolean armor = hull != null && hull.isArmorSegment(i);
            shapes.setColor(i < hp ? (armor ? Color.ORANGE : color) : Color.DARK_GRAY);
            shapes.rect(x + (i % columns) * cellWidth, y + (i / columns) * cellHeight,
                    cellWidth - gapX, cellHeight - gapY);
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
        drawCentered(victory ? "CHIẾN THẮNG!" : "THẤT BẠI", x, panelY + 52f);

        // DIEM
        font.getData().setScale(1.3f);
        font.setColor(1f, 0.92f, 0.40f, 1f);
        drawCentered("ĐIỂM SỐ: " + state.score, x, panelY + 100f);

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
                        + "   XU: " + Math.max(0, state.coinsEarned),
                x, panelY + 165f);

        // Hướng dẫn phím.
        font.getData().setScale(0.95f);
        font.setColor(0.75f, 0.78f, 0.82f, 1f);
        drawCentered(victory ? "Nhấn N để sang màn tiếp theo" : "Nhấn R để chơi lại", x, panelY + 205f);
        drawCentered(victory ? "Nhấn Esc để quay lại chọn màn" : "", x, panelY + 230f);
        font.setColor(Color.GOLD);
        drawCentered("Nhấn B để vào cửa hàng", x, panelY + 258f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /** Báo cáo sau chiến thắng, bố cục cuộn và nút hành động cố định. */
    private void drawVictoryBanner(GameStateSnapshot state) {
        victoryOverlay.render(state, batch, shapes, assets);
    }

    /** Dải gợi ý từ trigger/thùng đồ ở đáy màn hình (Builds addTextHint). */
    private void drawHintBar(String text) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float barHeight = 46f;
        float barY = height - barHeight;
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0, barY, width, barHeight);
        shapes.setColor(1f, 1f, 1f, 0.28f);
        shapes.rect(0, barY, width, 1.5f);
        shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        font.getData().setScale(1.25f);
        font.setColor(1f, 0.92f, 0.55f, 1f);
        GlyphLayout layout = new GlyphLayout(font, text);
        batch.begin();
        font.draw(batch, text, (width - layout.width) / 2f, barY + (barHeight - layout.height) / 2f);
        batch.end();
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /** Builds hiện briefing trước khi màn chạy; Enter để bắt đầu. */
    private void drawBriefing(GameStateSnapshot state) {
        drawTextPanel("NHIỆM VỤ", state.briefing, 0.82f);
        drawPromptBar("Nhấn Enter để bắt đầu", null, false);
    }

    /**
     * Panel nhiệm vụ dùng font Roboto Condensed có dấu và tự xuống dòng.
     */
    private void drawTextPanel(String title, String body, float dim) {
        if (body == null) return;
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float panelW = Math.min(680f, width - 40f);
        float textW = panelW - 56f;
        BitmapFont missionFont = assets.getShopFont();
        float bodyScale = Math.max(.24f, Math.min(.40f, width / 2000f));
        missionFont.getData().setScale(bodyScale);
        GlyphLayout bodyLayout = new GlyphLayout(missionFont, body, Color.WHITE, textW,
                com.badlogic.gdx.utils.Align.left, true);
        float panelH = bodyLayout.height + 92f;
        float panelX = (width - panelW) / 2f;
        float panelY = Math.max(16f, (height - 78f - panelH) / 2f);

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (dim > 0) {
            shapes.setColor(0f, 0f, 0f, dim * 0.6f);
            shapes.rect(0, 0, width, height);
        }
        shapes.setColor(0.08f, 0.07f, 0.06f, 0.92f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.setColor(0.95f, 0.76f, 0.15f, 1f);
        shapes.rect(panelX, panelY, panelW, 4f);
        shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        batch.begin();
        missionFont.getData().setScale(bodyScale * 1.45f);
        missionFont.setColor(1f, 0.78f, 0.12f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(missionFont, title);
        missionFont.draw(batch, title, (width - titleLayout.width) / 2f, panelY + 22f);
        missionFont.getData().setScale(bodyScale);
        missionFont.setColor(0.92f, 0.92f, 0.88f, 1f);
        missionFont.draw(batch, body, panelX + 28f, panelY + 64f, textW, com.badlogic.gdx.utils.Align.left, true);
        batch.end();
        missionFont.getData().setScale(1f);
        missionFont.setColor(Color.WHITE);
    }

    /** Dải nhắc bắt đầu ở đáy màn hình, tuỳ chọn dấu ✓ và dòng phụ. */
    private void drawPromptBar(String message, String subline, boolean check) {
        BitmapFont font = assets.getShopFont();
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float barHeight = 78f;
        float barY = height - barHeight;

        font.getData().setScale(.65f);
        GlyphLayout messageLayout = new GlyphLayout(font, message);
        float checkSize = check ? 30f : 0f;
        float gap = check ? 16f : 0f;
        float groupWidth = checkSize + gap + messageLayout.width;
        float groupX = (width - groupWidth) / 2f;
        float messageTop = barY + (subline != null ? 14f : (barHeight - messageLayout.height) / 2f);
        float checkCenterY = messageTop + messageLayout.height / 2f;

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0, barY, width, barHeight);
        shapes.setColor(1f, 1f, 1f, 0.28f);
        shapes.rect(0, barY, width, 1.5f);
        if (check) drawCheckMark(groupX, checkCenterY, checkSize);
        shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, message, groupX + checkSize + gap, messageTop);
        if (subline != null) {
            font.getData().setScale(.28f);
            font.setColor(0.72f, 0.72f, 0.68f, 1f);
            GlyphLayout subLayout = new GlyphLayout(font, subline);
            font.draw(batch, subline, (width - subLayout.width) / 2f, messageTop + messageLayout.height + 14f);
        }
        batch.end();

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /** Dấu ✓ xanh lá; font mặc định không có glyph này nên vẽ bằng nét. */
    private void drawCheckMark(float left, float centerY, float size) {
        float x1 = left, y1 = centerY + size * 0.05f;
        float x2 = left + size * 0.35f, y2 = centerY + size * 0.45f;
        float x3 = left + size, y3 = centerY - size * 0.6f;
        shapes.setColor(0.05f, 0.2f, 0.05f, 0.9f);
        shapes.rectLine(x1, y1, x2, y2, 7.5f);
        shapes.rectLine(x2, y2, x3, y3, 7.5f);
        shapes.setColor(0.42f, 0.86f, 0.22f, 1f);
        shapes.rectLine(x1, y1, x2, y2, 4.5f);
        shapes.rectLine(x2, y2, x3, y3, 4.5f);
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
        defeatOverlay.dispose();
        victoryOverlay.dispose();
        batch.dispose();
        shapes.dispose();
        font.dispose();
        whitePixel.dispose();
    }
}
