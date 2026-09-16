package com.tankzors.client.session;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.tankzors.client.gameplay.result.MatchResult;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.BulletSnapshot;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.model.MapSnapshot;
import com.tankzors.client.model.TankSnapshot;
import com.tankzors.client.network.GameSocket;
import com.tankzors.client.network.RelayPackets;

/**
 * Session guest co-op: render snapshot host gửi qua relay. KHÔNG chạy simulation.
 * Input local -> PLAYER_INPUT envelope -> socket (relay -> host).
 *
 * <p>Parse JSON bằng LibGDX JsonReader (GWT-safe). Guest không tự áp damage,
 * không gửi WORLD_STATE/MATCH_RESULT.</p>
 */
public class GuestGameSession implements GameSession {

    private final GameSocket socket;
    private final String localPlayerId;
    private final MatchResultSink resultHandler;
    private long inputSeq = 1;

    private GameStateSnapshot snapshot;
    private MatchResult matchResult;
    private String status = "WAITING";

    public interface MatchResultSink { void onResult(MatchResult r); }

    public GuestGameSession(GameSocket socket, String localPlayerId, MatchResultSink handler) {
        this.socket = socket;
        this.localPlayerId = localPlayerId;
        this.resultHandler = handler;
    }

    @Override public boolean requiresSocket() { return true; }

    @Override public void onMove(Direction direction, boolean moving) {
        String payload = RelayPackets.playerInputPayload(
                direction == null ? null : direction.name(), moving, false, false);
        send("PLAYER_INPUT", payload);
    }
    @Override public void onShoot() {
        String payload = RelayPackets.playerInputPayload(null, false, true, false);
        send("PLAYER_INPUT", payload);
    }
    @Override public void onAllReleased() {
        String payload = RelayPackets.playerInputPayload(null, false, false, true);
        send("PLAYER_INPUT", payload);
    }

    private void send(String type, String payload) {
        socket.send(RelayPackets.envelope(type, null, null, inputSeq++, payload));
    }

    @Override public void update(float rawDeltaTime) {
        String raw;
        while ((raw = socket.poll()) != null) handle(raw);
    }

    @Override public GameStateSnapshot snapshot() { return snapshot; }
    @Override public String localPlayerId() { return localPlayerId; }
    @Override public String status() { return status; }
    @Override public void restart() { /* host điều khiển */ }
    @Override public void nextLevel() { /* host điều khiển */ }
    @Override public MatchResult matchResult() { return matchResult; }
    @Override public void dispose() { socket.disconnect(); }

    private void handle(String raw) {
        JsonValue root;
        try { root = new JsonReader().parse(raw); }
        catch (Exception e) { return; }
        String type = root.getString("type", "");
        if ("WORLD_STATE".equals(type)) {
            JsonValue payload = root.get("payload");
            if (payload != null) {
                snapshot = parseSnapshot(payload);
                status = snapshot.gameStatus;
            }
        } else if ("MATCH_STARTED".equals(type)) {
            status = "PLAYING";
        } else if ("MATCH_RESULT".equals(type)) {
            JsonValue p = root.get("payload");
            if (p != null) {
                matchResult = new MatchResult(
                        p.getString("runId", ""), p.getString("mode", "COOP"),
                        p.getString("levelId", ""), p.getString("mapVersion", ""),
                        p.getString("status", ""), p.getInt("score", 0),
                        p.getInt("hpRemain", 0), p.getInt("aiKills", 0), p.getInt("elapsedSeconds", 0));
                if (resultHandler != null) resultHandler.onResult(matchResult);
            }
        } else if ("ROOM_CLOSED".equals(type)) {
            status = "WAITING";
        }
    }

    private GameStateSnapshot parseSnapshot(JsonValue payload) {
        GameStateSnapshot snap = new GameStateSnapshot();
        snap.clearEntities();
        snap.gameStatus = payload.getString("status", "PLAYING");
        snap.score = payload.getInt("score", 0);

        JsonValue map = payload.get("map");
        if (map != null) {
            MapSnapshot ms = new MapSnapshot();
            ms.width = map.getInt("width", 20);
            ms.height = map.getInt("height", 15);
            ms.tileSize = map.getInt("tileSize", 40);
            JsonValue grid = map.get("grid");
            if (grid != null && grid.isArray()) {
                ms.grid = new int[countChildren(grid)][];
                int i = 0;
                for (JsonValue r = grid.child; r != null; r = r.next, i++) {
                    ms.grid[i] = new int[countChildren(r)];
                    int j = 0;
                    for (JsonValue c = r.child; c != null; c = c.next, j++) ms.grid[i][j] = c.asInt();
                }
            }
            snap.map = ms;
        }

        JsonValue tanks = payload.get("tanks");
        if (tanks != null) {
            for (JsonValue t = tanks.child; t != null; t = t.next) {
                TankSnapshot ts = new TankSnapshot();
                ts.id = t.getString("id", "");
                ts.x = t.getFloat("x", 0);
                ts.y = t.getFloat("y", 0);
                ts.rotation = t.getFloat("rotation", 0);
                ts.hp = t.getInt("hp", 0);
                ts.maxHp = t.getInt("maxHp", 100);
                ts.alive = t.getBoolean("alive", true);
                ts.isPlayer = t.getBoolean("isPlayer", false);
                snap.tanks.add(ts);
            }
        }

        JsonValue bullets = payload.get("bullets");
        if (bullets != null) {
            for (JsonValue b = bullets.child; b != null; b = b.next) {
                BulletSnapshot bs = new BulletSnapshot();
                bs.id = b.getString("id", "");
                bs.x = b.getFloat("x", 0);
                bs.y = b.getFloat("y", 0);
                bs.direction = b.getFloat("direction", 0);
                bs.ownerId = b.getString("ownerId", "");
                bs.active = b.getBoolean("active", true);
                snap.bullets.add(bs);
            }
        }
        return snap;
    }

    private static int countChildren(JsonValue v) {
        int n = 0;
        for (JsonValue c = v.child; c != null; c = c.next) n++;
        return n;
    }
}
