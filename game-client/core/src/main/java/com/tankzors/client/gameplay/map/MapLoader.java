package com.tankzors.client.gameplay.map;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse map JSON thành MapDefinition. Dùng LibGDX JsonReader (reflection-free, GWT-safe).
 * Thay thế server Gson#fromJson. Đọc field tường minh — không reflection.
 *
 * <p>Người gọi (HtmlLauncher/Desktop) lấy chuỗi JSON qua bootstrap REST hoặc asset,
 * rồi gọi {@link #parse(String)}. Không dùng ClassLoader/InputStream kiểu server.</p>
 */
public final class MapLoader {
    private MapLoader() {}

    /**
     * Parse một chuỗi JSON map thành MapDefinition đã initializeMap().
     * @param jsonText nội dung file map (UTF-8)
     * @return MapDefinition hoặc null nếu JSON lỗi
     */
    public static MapDefinition parse(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) return null;
        try {
            JsonValue root = new JsonReader().parse(jsonText);
            MapDefinition map = new MapDefinition();
            map.setName(root.getString("name", ""));
            map.setDifficulty(root.getString("difficulty", "EASY"));
            if (root.has("tileSize")) map.setTileSize(root.getInt("tileSize", MapDefinition.DEFAULT_TILE_SIZE));
            int width = root.getInt("width", 0);
            int height = root.getInt("height", 0);

            // enemies[]
            List<MapDefinition.EnemyConfig> enemies = new ArrayList<>();
            JsonValue enemyArray = root.get("enemies");
            if (enemyArray != null) {
                for (JsonValue e = enemyArray.child; e != null; e = e.next) {
                    MapDefinition.EnemyConfig cfg = new MapDefinition.EnemyConfig();
                    cfg.setType(e.getString("type", "AI"));
                    cfg.setDifficulty(e.getString("difficulty", "EASY"));
                    cfg.setSpawnX(e.getInt("spawnX", 0));
                    cfg.setSpawnY(e.getInt("spawnY", 0));
                    enemies.add(cfg);
                }
            }
            map.setEnemies(enemies);

            // rewards{}
            JsonValue rewardObj = root.get("rewards");
            if (rewardObj != null) {
                MapDefinition.RewardsConfig r = new MapDefinition.RewardsConfig();
                r.setStars(rewardObj.getInt("stars", 0));
                r.setCoins(rewardObj.getInt("coins", 0));
                map.setRewards(r);
            }

            // grid[row][col] — 2D int array
            JsonValue gridArray = root.get("grid");
            if (gridArray != null && gridArray.isArray()) {
                int rows = 0;
                for (JsonValue r = gridArray.child; r != null; r = r.next) rows++;
                int[][] grid;
                if (rows == 0) {
                    grid = new int[0][];
                } else {
                    grid = new int[rows][];
                    int i = 0;
                    for (JsonValue r = gridArray.child; r != null; r = r.next, i++) {
                        int cols = 0;
                        for (JsonValue c = r.child; c != null; c = c.next) cols++;
                        int[] row = new int[cols];
                        int j = 0;
                        for (JsonValue c = r.child; c != null; c = c.next, j++) {
                            row[j] = c.asInt();
                        }
                        grid[i] = row;
                    }
                }
                map.setGrid(grid);
                // Sau setGrid: width/height suy ra từ grid nếu chưa set
                if (width <= 0 && grid.length > 0 && grid[0] != null) width = grid[0].length;
                if (height <= 0) height = grid.length;
                map.setWidth(width);
                map.setHeight(height);
            }
            map.initializeMap();
            return map;
        } catch (Exception e) {
            // GWT không dùng logger; in stderr để debug desktop
            System.err.println("MapParser lỗi: " + e.getMessage());
            return null;
        }
    }
}
