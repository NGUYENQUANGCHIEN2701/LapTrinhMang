package com.tankzors.client.session;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.tankzors.client.gameplay.GameSimulation;
import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WorldSnapshotMapper;
import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.MapLoader;
import com.tankzors.client.gameplay.result.MatchResult;
import com.tankzors.client.gameplay.rules.ScoreCalculator;
import com.tankzors.client.input.ClientInputProcessor;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.network.GameSocket;
import com.tankzors.client.network.RelayPackets;

import java.util.ArrayList;
import java.util.List;

/**
 * Session host co-op: chạy GameSimulation local (2 player + AI), nhận input
 * guest qua relay, broadcast WORLD_STATE (~15Hz) + MATCH_RESULT. Friendly fire
 * chặn trong simulation. Host rời trận -> reload không chặn từ đây (server đóng phòng).
 */
public class HostGameSession implements GameSession {

    private static final float SNAPSHOT_INTERVAL = 1f / 15f; // 15Hz

    private final GameSocket socket;
    private final String hostPlayerId;
    private final ResultHandler resultHandler;

    private GameSimulation sim;
    private MapDefinition loadedMap;
    private String levelId;
    private String runId;
    private String mapVersion;
    private final List<String> playerIds = new ArrayList<>();

    private float snapshotTimer = 0;
    private long tick = 0;
    private MatchResult matchResult;
    private boolean matchResultFired;
    private String status = "WAITING";
    private boolean started = false;

    public interface ResultHandler { boolean onResult(MatchResult result); }

    public HostGameSession(GameSocket socket, String hostPlayerId,
                           String levelId, String runId, String mapVersion,
                           ResultHandler handler) {
        this.socket = socket;
        this.hostPlayerId = hostPlayerId;
        this.levelId = levelId;
        this.runId = runId;
        this.mapVersion = mapVersion;
        this.resultHandler = handler;
        this.sim = new GameSimulation(12345L);
    }

    /** Khi guest kết nối (server thông báo MATCH_STARTED), host biết danh sách player. */
    public void beginMatch(List<String> connectedPlayerIds) {
        playerIds.clear();
        playerIds.addAll(connectedPlayerIds);
        if (!playerIds.contains(hostPlayerId)) playerIds.add(0, hostPlayerId);
        sim = new GameSimulation(runId.hashCode() | 1L);
        sim.setLocalPlayerId(hostPlayerId);
        sim.setMatchEndHandler(this::onMatchEnd);
        if (loadedMap != null) {
            sim.startCoopLevel(loadedMap, levelId, playerIds);
            started = true;
            status = "PLAYING";
        }
    }

    public void loadMapJson(String mapJson) {
        loadedMap = MapLoader.parse(mapJson);
    }

    @Override public void start() { socket.connect(); }

    // ===== INPUT LOCAL (host tank) =====
    @Override public void onMove(ClientInputProcessor.Direction d, boolean moving) {
        Direction gd = convert(d);
        sim.enqueueMove(hostPlayerId, gd, moving);
    }
    @Override public void onShoot() { sim.enqueueShoot(hostPlayerId); }
    @Override public void onAllReleased() { sim.enqueueStopAll(hostPlayerId); }

    private static Direction convert(ClientInputProcessor.Direction d) {
        if (d == null) return null;
        switch (d) {
            case UP: return Direction.UP;
            case DOWN: return Direction.DOWN;
            case LEFT: return Direction.LEFT;
            case RIGHT: return Direction.RIGHT;
        }
        return null;
    }

    @Override public void update(float rawDeltaTime) {
        // Drain relay input từ guest.
        String raw;
        while ((raw = socket.poll()) != null) handleIncoming(raw);
        if (!started) return;
        sim.advance(rawDeltaTime);
        status = sim.getWorld().getStatus().name();

        snapshotTimer += rawDeltaTime;
        if (snapshotTimer >= SNAPSHOT_INTERVAL) {
            snapshotTimer = 0;
            broadcastWorldState();
        }
    }

    private void handleIncoming(String raw) {
        JsonValue root;
        try { root = new JsonReader().parse(raw); }
        catch (Exception e) { return; }
        String type = root.getString("type", "");
        if ("PLAYER_INPUT".equals(type)) {
            JsonValue payload = root.get("payload");
            String senderId = root.getString("senderId", null);
            if (senderId == null && payload != null) senderId = payload.getString("senderId", null);
            if (senderId == null || !playerIds.contains(senderId)) return;
            Direction dir = null;
            if (payload != null && payload.has("direction")) {
                try { dir = Direction.valueOf(payload.getString("direction")); }
                catch (IllegalArgumentException ignored) {}
            }
            boolean shoot = payload != null && payload.getBoolean("shoot", false);
            boolean stopAll = payload != null && payload.getBoolean("stopAll", false);
            if (stopAll) {
                sim.enqueueStopAll(senderId);
            } else if (shoot) {
                sim.enqueueShoot(senderId);
            } else if (dir != null) {
                boolean moving = payload != null && payload.getBoolean("moving", true);
                sim.enqueueMove(senderId, dir, moving);
            }
        } else if ("MATCH_STARTED".equals(type)) {
            // Server relay thông báo match thực sự bắt đầu; khởi tạo simulation.
            JsonValue p = root.get("payload");
            if (p != null) {
                List<String> ids = new ArrayList<>();
                JsonValue members = p.get("members");
                if (members != null) {
                    for (JsonValue m = members.child; m != null; m = m.next) {
                        ids.add(m.getString("playerId", ""));
                    }
                }
                beginMatch(ids);
            }
        }
    }

    private void broadcastWorldState() {
        GameWorld w = sim.getWorld();
        GameStateSnapshot snap = WorldSnapshotMapper.snapshot(w, hostPlayerId, w.getScore(), (int) sim.getElapsedSeconds());
        tick++;
        String payload = "\"status\":" + RelayPackets.quote(snap.gameStatus)
                + ",\"score\":" + snap.score
                + ",\"tick\":" + tick
                + ",\"map\":" + serializeMap(snap)
                + ",\"tanks\":" + serializeTanks(snap)
                + ",\"bullets\":" + serializeBullets(snap);
        socket.send(RelayPackets.envelope("WORLD_STATE", null, runId, tick, payload));
    }

    private String serializeMap(GameStateSnapshot s) {
        if (s.map == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"width\":").append(s.map.width)
          .append(",\"height\":").append(s.map.height)
          .append(",\"tileSize\":").append(s.map.tileSize)
          .append(",\"grid\":[");
        if (s.map.grid != null) {
            for (int r = 0; r < s.map.grid.length; r++) {
                if (r > 0) sb.append(',');
                sb.append('[');
                for (int c = 0; c < s.map.grid[r].length; c++) {
                    if (c > 0) sb.append(',');
                    sb.append(s.map.grid[r][c]);
                }
                sb.append(']');
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String serializeTanks(GameStateSnapshot s) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < s.tanks.size(); i++) {
            com.tankzors.client.model.TankSnapshot t = s.tanks.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(RelayPackets.quote(t.id))
              .append(",\"x\":").append(t.x).append(",\"y\":").append(t.y)
              .append(",\"rotation\":").append(t.rotation)
              .append(",\"hp\":").append(t.hp).append(",\"maxHp\":").append(t.maxHp)
              .append(",\"armorHp\":").append(t.armorHp)
              .append(",\"shieldHp\":").append(t.shieldHp).append(",\"maxShieldHp\":").append(t.maxShieldHp)
              .append(",\"alive\":").append(t.alive).append(",\"isPlayer\":").append(t.isPlayer)
              .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private String serializeBullets(GameStateSnapshot s) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < s.bullets.size(); i++) {
            com.tankzors.client.model.BulletSnapshot b = s.bullets.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(RelayPackets.quote(b.id))
              .append(",\"x\":").append(b.x).append(",\"y\":").append(b.y)
              .append(",\"direction\":").append(b.direction)
              .append(",\"ownerId\":").append(RelayPackets.quote(b.ownerId))
              .append(",\"weaponType\":").append(b.weaponType)
              .append(",\"damage\":").append(b.damage)
              .append(",\"active\":").append(b.active).append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private void onMatchEnd(String lvl, String statusStr, int hpRemain, int aiKills, int elapsedSeconds) {
        if (matchResultFired) return;
        matchResultFired = true;
        int score = ScoreCalculator.calculate(hpRemain, aiKills, elapsedSeconds);
        // Co-op reward chưa trong scope — chuyển tiếp 0 (client-reported, server vẫn clamp).
        matchResult = new MatchResult(runId, "COOP", lvl,
                mapVersion == null ? "" : mapVersion, statusStr, score, hpRemain, aiKills, elapsedSeconds, 0, 0);
        // Broadcast MATCH_RESULT tới guests.
        String payload = "\"runId\":" + RelayPackets.quote(runId)
                + ",\"mode\":\"COOP\""
                + ",\"levelId\":" + RelayPackets.quote(lvl)
                + ",\"mapVersion\":" + RelayPackets.quote(mapVersion == null ? "" : mapVersion)
                + ",\"status\":" + RelayPackets.quote(statusStr)
                + ",\"score\":" + score
                + ",\"hpRemain\":" + hpRemain
                + ",\"aiKills\":" + aiKills
                + ",\"elapsedSeconds\":" + elapsedSeconds
                + ",\"starsEarned\":0"
                + ",\"coinsEarned\":0";
        socket.send(RelayPackets.envelope("MATCH_RESULT", null, runId, 0, payload));
        if (resultHandler != null) {
            try { resultHandler.onResult(matchResult); }
            catch (Exception ignored) {}
        }
    }

    @Override public GameStateSnapshot snapshot() {
        if (!started) return null;
        return WorldSnapshotMapper.snapshot(sim.getWorld(), hostPlayerId,
                sim.getWorld().getScore(), (int) sim.getElapsedSeconds(), sim.getInterpolationAlpha());
    }
    @Override public String localPlayerId() { return hostPlayerId; }
    @Override public String status() { return status; }
    @Override public void restart() { /* host-only qua room state */ }
    @Override public void nextLevel() { /* host-only */ }
    @Override public MatchResult matchResult() { return matchResult; }
    @Override public void dispose() { socket.disconnect(); }

    public String getRunId() { return runId; }
}
