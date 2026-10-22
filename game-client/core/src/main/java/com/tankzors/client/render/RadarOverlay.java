package com.tankzors.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.model.*;

/**
 * Tactical radar minimap rendered using 100% authentic game assets:
 * real ground textures, wall sprites, hazard-striped spawn pads, flag and powerup sprites,
 * authentic tank sprites with team rings, and Builds HUD panel.
 */
public final class RadarOverlay {

    public static int menuHit(int x, int y, int width, int height) {
        if (x < width / 2 - 150 || x > width / 2 + 150) return -1;
        int index = (y - (height / 2 - 50)) / 30;
        return y >= height / 2 - 50 && index >= 0 && index < 4 ? index : -1;
    }

    public void render(GameStateSnapshot state, ShapeRenderer shapes, SpriteBatch batch, BitmapFont font,
                       AssetRepository assets, int screenW, int screenH) {
        MapSnapshot map = state.map;
        if (map == null || map.grid == null || map.width <= 0 || map.height <= 0) return;

        RadarLayout layout = new RadarLayout(screenW, screenH, map.width, map.height);
        float cell = layout.cell, left = layout.left, top = layout.top;
        float width = map.width * cell, height = map.height * cell;

        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 1. NỀN PANEL ĐỔ BÓNG MỜ TỰ NHIÊN TỪ ASSET BUILDS (gui/panel.png)
        if (assets.getPanel() != null) {
            batch.begin();
            batch.setColor(0f, 0f, 0f, 0.85f);
            batch.draw(assets.getPanel(), layout.x - 12, layout.y - 8, layout.width + 24, layout.height + 16);
            batch.end();
        }

        // 2. NỀN VÀ VIỀN NGOÀI PANEL
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.07f, 0.08f, 0.95f);
        shapes.rect(layout.x, layout.y, layout.width, layout.height);
        shapes.setColor(0f, 0f, 0f, 1f);
        shapes.rect(layout.x, layout.y, layout.width, 1f);
        shapes.rect(layout.x, layout.y + layout.height - 1f, layout.width, 1f);
        shapes.rect(layout.x, layout.y, 1f, layout.height);
        shapes.rect(layout.x + layout.width - 1f, layout.y, 1f, layout.height);
        shapes.end();

        // 3. VẼ BẢN ĐỒ MINIMAP BẰNG 100% ASSET SPRITE/TEXTURE THẬT CỦA GAME
        batch.begin();

        // (a) Lớp nền đất thật từ texture của game (ground.png)
        batch.setColor(0.85f, 0.85f, 0.85f, 1f);
        for (int row = 0; row < map.height; row++) {
            for (int col = 0; col < map.width; col++) {
                float ox = left + col * cell;
                float oy = top + row * cell;
                TextureRegion gFrame = null;
                int visualTile = map.visualGrid != null && row < map.visualGrid.length && col < map.visualGrid[row].length
                        ? map.visualGrid[row][col] : 0;
                if (visualTile > 0) {
                    gFrame = assets.groundFrameById(GameRenderer.legacyGroundTileId(visualTile));
                }
                if (gFrame == null) {
                    gFrame = assets.groundFrame(row, col);
                }
                if (gFrame != null) {
                    batch.draw(gFrame, ox, oy, cell, cell);
                }
            }
        }

        // (b) Lớp chướng ngại vật & bệ xuất phát & vật phẩm từ asset thật (walls.png, buildings.png, powerups.png)
        batch.setColor(Color.WHITE);
        for (int row = 0; row < map.height; row++) {
            for (int col = 0; col < map.width; col++) {
                int tile = map.grid[row][col];
                float ox = left + col * cell;
                float oy = top + row * cell;
                int visualTile = map.visualGrid != null && row < map.visualGrid.length && col < map.visualGrid[row].length
                        ? map.visualGrid[row][col] : 0;
                int legacyWall = GameRenderer.legacyWallTileId(visualTile);

                switch (tile) {
                    case 1: // WALL: Gạch / Hộp gỗ từ walls.png
                        TextureRegion wf = legacyWall >= 1 && legacyWall <= 8
                                ? assets.wallFrameById(legacyWall)
                                : assets.wallFrame(4);
                        if (wf != null) batch.draw(wf, ox, oy, cell, cell);
                        break;
                    case 5: // CONCRETE: Bê tông trắng từ walls.png
                        TextureRegion cf = legacyWall >= 9 && legacyWall <= 13 && legacyWall != 11
                                ? assets.wallFrameById(legacyWall)
                                : assets.whiteWallFrame(row, col);
                        if (cf != null) batch.draw(cf, ox, oy, cell, cell);
                        break;
                    case 6: // BUSH: Bụi cây từ walls.png
                        TextureRegion bf = assets.bushFrame(row, col);
                        if (bf != null) batch.draw(bf, ox, oy, cell, cell);
                        break;
                    case 2: // WATER/SPIKES: Chông gai từ spikes.png
                        TextureRegion sf = assets.spikesFrame(row, col);
                        if (sf != null) batch.draw(sf, ox, oy, cell, cell);
                        break;
                    case 3: // Team 1 Spawn: Bệ xuất phát sọc chéo xanh từ buildings.png
                        TextureRegion sp1 = assets.spawnBaseFrame(true);
                        if (sp1 != null) batch.draw(sp1, ox, oy, cell, cell);
                        break;
                    case 4: // Team 2 Spawn: Bệ xuất phát sọc chéo đỏ từ buildings.png
                        TextureRegion sp2 = assets.spawnBaseFrame(false);
                        if (sp2 != null) batch.draw(sp2, ox, oy, cell, cell);
                        break;
                    case 9: // FLAG: Sprite cờ vàng/xanh từ buildings.png
                        TextureRegion ff = assets.flagFrame();
                        if (ff != null) batch.draw(ff, ox, oy, cell, cell);
                        break;
                    case 7: case 8: case 10: case 11: case 12: case 13: case 14: // POWERUPS
                        if (state.radarShowObjects) {
                            TextureRegion pf = assets.powerupFrame(tile, 0f);
                            if (pf != null) batch.draw(pf, ox + cell * 0.1f, oy + cell * 0.1f, cell * 0.8f, cell * 0.8f);
                        }
                        break;
                    case 15: // ROCK BLOCK
                        TextureRegion rf = assets.wallFrameById(11);
                        if (rf != null) batch.draw(rf, ox, oy, cell, cell);
                        break;
                }
            }
        }

        // (c) Buildings (Công trình lớn từ big_builds.png)
        for (BuildingSnapshot b : state.buildings) {
            if (b.hp > 0) {
                TextureRegion bFrame = assets.largeBuildingFrame("reactor".equals(b.type));
                if (bFrame != null) {
                    batch.draw(bFrame, left + b.col * cell, top + b.row * cell, cell * 2f, cell * 2f);
                }
            }
        }

        // (d) Xe tăng (Tanks) - Sprite xe tăng xoay thật kèm vòng phe (team ring)
        for (TankSnapshot tank : state.tanks) {
            if (!tank.alive || tank.hp <= 0) continue;
            float ox = left + (tank.x / map.tileSize) * cell;
            float oy = top + (tank.y / map.tileSize) * cell;
            float tankSize = Math.max(cell * 1.4f, 12f);
            float tOffX = ox + cell / 2f - tankSize / 2f;
            float tOffY = oy + cell / 2f - tankSize / 2f;

            // Vòng phe (rings.png) dưới chân xe để nhận diện rõ ràng trên minimap
            TextureRegion ring = assets.teamRingFrame(tank.isPlayer || tank.ally);
            if (ring != null) {
                batch.draw(ring, tOffX, tOffY, tankSize, tankSize);
            }

            if (tank.turret) {
                TextureRegion tf = assets.turretFrame(tank.rotation);
                if (tf != null) batch.draw(tf, tOffX, tOffY, tankSize, tankSize);
            } else {
                TextureRegion tf = assets.tankFrame(tank.isPlayer || tank.ally, tank.rotation);
                if (tf != null) batch.draw(tf, tOffX, tOffY, tankSize, tankSize);
            }
        }
        batch.end();

        // 4. VIỀN BẢN ĐỒ VÀ MŨI TÊN CHỈ HƯỚNG XE NGƯỜI CHƠI
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 1f);
        shapes.rect(left, top, width, 1f);
        shapes.rect(left, top + height - 1f, width, 1f);
        shapes.rect(left, top, 1f, height);
        shapes.rect(left + width - 1f, top, 1f, height);

        // Mũi tên chỉ hướng nổi bật cho xe người chơi
        for (TankSnapshot tank : state.tanks) {
            if (!tank.alive || tank.hp <= 0) continue;
            if (tank.isPlayer) {
                float rotRad = tank.rotation * MathUtils.degreesToRadians;
                float dirX = MathUtils.sin(rotRad);
                float dirY = -MathUtils.cos(rotRad);
                float tcx = left + (tank.x / map.tileSize) * cell + cell / 2f;
                float tcy = top + (tank.y / map.tileSize) * cell + cell / 2f;
                float h = Math.max(cell * 0.55f, 5f);
                shapes.setColor(1f, 0.88f, 0.2f, 1f); // Vàng kim rực rỡ
                shapes.triangle(
                        tcx + h * dirX, tcy + h * dirY,
                        tcx - h * 0.6f * dirX + h * 0.5f * dirY, tcy - h * 0.6f * dirY - h * 0.5f * dirX,
                        tcx - h * 0.6f * dirX - h * 0.5f * dirY, tcy - h * 0.6f * dirY + h * 0.5f * dirX
                );
            }
        }
        shapes.end();

        // 5. HEADER & FOOTER TINH TẾ VỚI ASSET CỦA BUILDS
        batch.begin();
        TextureRegion icon = assets.getIcon(23);
        if (icon != null) {
            batch.setColor(Color.WHITE);
            batch.draw(icon, layout.x + 8, layout.y + 5, 18, 18);
        }
        font.setColor(0.92f, 0.78f, 0.28f, 1f); // Vàng gold cổ điển của Tankzors
        font.draw(batch, "RADAR", layout.x + 30, layout.y + 6);

        // Đếm số địch
        int hostileCount = 0;
        for (TankSnapshot t : state.tanks) {
            if (!t.isPlayer && !t.ally && t.alive && t.hp > 0) hostileCount++;
        }
        if (hostileCount > 0) {
            font.setColor(0.95f, 0.35f, 0.35f, 1f);
            font.draw(batch, "FOE: " + hostileCount, layout.x + layout.width - 52, layout.y + 6);
        }

        font.setColor(0.65f, 0.65f, 0.65f, 0.85f);
        font.draw(batch, "[Q] EXIT", layout.x + 8, top + height + 6);
        font.setColor(Color.WHITE);
        batch.end();

        // 6. PAUSE MENU (NẾU ĐANG TẠM DỪNG RADAR)
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (state.radarPaused) {
            shapes.setColor(0, 0, 0, .8f);
            shapes.rect(0, 0, screenW, screenH);
            shapes.setColor(.1f, .12f, .15f, 1);
            shapes.rect(screenW / 2f - 160, screenH / 2f - 100, 320, 190);
            if (!state.radarSettingsOpen) {
                shapes.setColor(.25f, .3f, .36f, 1);
                shapes.rect(screenW / 2f - 150, screenH / 2f - 50 + state.radarMenuSelection * 30, 300, 28);
            }
        }
        shapes.end();

        if (state.radarPaused) {
            batch.begin();
            font.setColor(Color.GOLD);
            font.draw(batch, state.radarSettingsOpen ? "SETTINGS" : "PAUSED", screenW / 2f - 130, screenH / 2f - 80);
            font.setColor(Color.WHITE);
            if (state.radarSettingsOpen) {
                font.draw(batch, "M: TOGGLE SOUND", screenW / 2f - 130, screenH / 2f - 32);
                font.draw(batch, "ESC / ENTER / CLICK: BACK", screenW / 2f - 130, screenH / 2f + 8);
            } else {
                String[] items = {"CONTINUE", "WORKSHOP", "SETTINGS", "EXIT TO CAMPAIGNS"};
                for (int i = 0; i < items.length; i++) font.draw(batch, items[i], screenW / 2f - 130, screenH / 2f - 44 + i * 30);
            }
            batch.end();
        }
    }
}
