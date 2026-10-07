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
                int[][] grid = readGrid(gridArray);
                map.setGrid(grid);
                // Sau setGrid: width/height suy ra từ grid nếu chưa set
                if (width <= 0 && grid.length > 0 && grid[0] != null) width = grid[0].length;
                if (height <= 0) height = grid.length;
                map.setWidth(width);
                map.setHeight(height);
            }
            JsonValue visualGridArray = root.get("visualGrid");
            if (visualGridArray != null && visualGridArray.isArray()) {
                map.setVisualGrid(readGrid(visualGridArray));
            }
            JsonValue missionObj = root.get("mission");
            if (missionObj != null && missionObj.isObject()) {
                map.setMission(parseMission(missionObj));
            }
            map.initializeMap();
            return map;
        } catch (Exception e) {
            // GWT không dùng logger; in stderr để debug desktop
            System.err.println("MapParser lỗi: " + e.getMessage());
            return null;
        }
    }

    static MissionConfig parseMission(JsonValue root) {
        MissionConfig mission = new MissionConfig();
        mission.setObjective(parseEnum(root.getString("objective", null)));
        mission.setBriefing(root.getString("briefing", null));
        mission.setDebriefing(root.getString("debriefing", null));
        mission.setEnemyWaves(parseWaves(root.get("enemyWaves")));
        mission.setAllies(parseWaves(root.get("allies")));
        JsonValue turrets = root.get("turrets");
        if (turrets != null) {
            for (JsonValue t = turrets.child; t != null; t = t.next) {
                mission.getTurrets().add(new MissionConfig.TurretConfig(
                        t.getInt("x", 0), t.getInt("y", 0), parseDirection(t.getString("direction", "DOWN")),
                        t.getInt("range", 4), t.getInt("hitPoints", 8)));
            }
        }
        JsonValue boxes = root.get("boxes");
        if (boxes != null) {
            for (JsonValue b = boxes.child; b != null; b = b.next) {
                mission.getBoxes().add(new MissionConfig.BoxConfig(
                        b.getInt("x", 0), b.getInt("y", 0), b.getInt("itemId", -1), b.getInt("count", 0)));
            }
        }
        JsonValue triggers = root.get("triggers");
        if (triggers != null) {
            for (JsonValue t = triggers.child; t != null; t = t.next) {
                MissionConfig.TriggerAction action;
                try {
                    action = MissionConfig.TriggerAction.valueOf(t.getString("action", "NONE"));
                } catch (IllegalArgumentException e) {
                    action = MissionConfig.TriggerAction.NONE;
                }
                mission.getTriggers().add(new MissionConfig.TriggerConfig(
                        t.getInt("x", 0), t.getInt("y", 0), t.getBoolean("active", true), action,
                        t.getString("text", null), t.getDouble("seconds", 2), t.getInt("target", -1)));
            }
        }
        return mission;
    }

    private static MissionConfig.Waves parseWaves(JsonValue obj) {
        if (obj == null || !obj.isObject()) return null;
        List<MissionConfig.SpawnBase> bases = new ArrayList<>();
        JsonValue array = obj.get("bases");
        if (array != null) {
            for (JsonValue b = array.child; b != null; b = b.next) {
                bases.add(new MissionConfig.SpawnBase(b.getInt("x", 0), b.getInt("y", 0), b.getInt("count", 0)));
            }
        }
        return new MissionConfig.Waves(obj.getInt("maxOnField", 1), obj.getDouble("intervalSeconds", 1.0),
                obj.getString("difficulty", "EASY"), obj.getBoolean("followPlayer", false), bases);
    }

    private static MissionConfig.Objective parseEnum(String value) {
        if (value == null) return MissionConfig.Objective.DESTROY_TANKS;
        try {
            return MissionConfig.Objective.valueOf(value);
        } catch (IllegalArgumentException e) {
            return MissionConfig.Objective.DESTROY_TANKS;
        }
    }

    private static com.tankzors.client.gameplay.entity.Direction parseDirection(String value) {
        try {
            return com.tankzors.client.gameplay.entity.Direction.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return com.tankzors.client.gameplay.entity.Direction.DOWN;
        }
    }

    private static int[][] readGrid(JsonValue array) {
        int rows = 0;
        for (JsonValue r = array.child; r != null; r = r.next) rows++;
        int[][] result = new int[rows][];
        int i = 0;
        for (JsonValue r = array.child; r != null; r = r.next, i++) {
            int cols = 0;
            for (JsonValue c = r.child; c != null; c = c.next) cols++;
            result[i] = new int[cols];
            int j = 0;
            for (JsonValue c = r.child; c != null; c = c.next, j++) {
                result[i][j] = c.asInt();
            }
        }
        return result;
    }
}
