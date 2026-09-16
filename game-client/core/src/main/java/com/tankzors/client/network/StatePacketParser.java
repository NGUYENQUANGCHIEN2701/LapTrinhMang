package com.tankzors.client.network;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.tankzors.client.model.BulletSnapshot;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.model.MapSnapshot;
import com.tankzors.client.model.TankSnapshot;

/**
 * Parse STATE packet (v1) dùng LibGDX {@link JsonReader}.
 *
 * <p>Tránh Gson để dễ port sang HTML/GWT phase sau (GWT không support
 * reflection). LibGDX JsonReader chỉ dùng String parsing nên chạy được
 * cả backend LWJGL3 lẫn GWT.</p>
 *
 * <p>Wire format tham khảo {@code docs/protocol-v1.md} mục 3.5.</p>
 */
public final class StatePacketParser {

    private final JsonReader jsonReader = new JsonReader();

    /**
     * Parse STATE JSON thành {@link GameStateSnapshot}.
     *
     * @param raw toàn bộ packet JSON (có type=STATE)
     * @return snapshot mới đã populate
     */
    public GameStateSnapshot parseState(String raw) {
        JsonValue root = jsonReader.parse(raw);
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.clearEntities();

        snapshot.gameStatus = root.getString("gameStatus", "WAITING");
        snapshot.score = root.getInt("score", 0);

        JsonValue mapNode = root.get("map");
        if (mapNode != null) {
            snapshot.map = parseMap(mapNode);
        }

        JsonValue tanksNode = root.get("tanks");
        if (tanksNode != null) {
            for (JsonValue t = tanksNode.child; t != null; t = t.next) {
                snapshot.tanks.add(parseTank(t));
            }
        }

        JsonValue bulletsNode = root.get("bullets");
        if (bulletsNode != null) {
            for (JsonValue b = bulletsNode.child; b != null; b = b.next) {
                if (b.getBoolean("active", true)) {
                    snapshot.bullets.add(parseBullet(b));
                }
            }
        }

        return snapshot;
    }

    /**
     * Lấy sessionId từ CONNECT packet.
     */
    public String parseSessionId(String raw) {
        return jsonReader.parse(raw).getString("sessionId", null);
    }

    /**
     * Lấy type của packet.
     */
    public String parseType(String raw) {
        return jsonReader.parse(raw).getString("type", "");
    }

    private TankSnapshot parseTank(JsonValue t) {
        TankSnapshot tank = new TankSnapshot();
        tank.id = t.getString("id", "");
        tank.x = t.getFloat("x", 0);
        tank.y = t.getFloat("y", 0);
        tank.rotation = t.getFloat("rotation", 0);
        tank.hp = t.getInt("hp", 0);
        tank.maxHp = t.getInt("maxHp", 100);
        tank.alive = t.getBoolean("alive", true);
        tank.isPlayer = t.getBoolean("isPlayer", false);
        return tank;
    }

    private BulletSnapshot parseBullet(JsonValue b) {
        BulletSnapshot bullet = new BulletSnapshot();
        bullet.id = b.getString("id", "");
        bullet.x = b.getFloat("x", 0);
        bullet.y = b.getFloat("y", 0);
        bullet.direction = b.getFloat("direction", 0);
        bullet.ownerId = b.getString("ownerId", "");
        bullet.active = b.getBoolean("active", true);
        return bullet;
    }

    private MapSnapshot parseMap(JsonValue m) {
        MapSnapshot map = new MapSnapshot();
        map.width = m.getInt("width", 0);
        map.height = m.getInt("height", 0);
        map.tileSize = m.getInt("tileSize", 40);

        JsonValue gridNode = m.get("grid");
        if (gridNode != null && gridNode.isArray()) {
            int rows = map.height > 0 ? map.height : gridNode.size;
            map.grid = new int[rows][];
            int r = 0;
            for (JsonValue row = gridNode.child; row != null && r < rows; row = row.next, r++) {
                int cols = map.width > 0 ? map.width : row.size;
                map.grid[r] = new int[cols];
                int c = 0;
                for (JsonValue cell = row.child; cell != null && c < cols; cell = cell.next, c++) {
                    map.grid[r][c] = cell.asInt();
                }
            }
        }
        return map;
    }
}
