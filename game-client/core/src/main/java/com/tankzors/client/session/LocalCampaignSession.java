package com.tankzors.client.session;

import com.tankzors.client.GameClient;
import com.tankzors.client.gameplay.GameSimulation;
import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.GameplayConfig;
import com.tankzors.client.gameplay.WorldSnapshotMapper;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.MapLoader;
import com.tankzors.client.gameplay.result.MatchResult;
import com.tankzors.client.gameplay.rules.ScoreCalculator;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.GameStateSnapshot;

/**
 * Session campaign local: chạy simulation hoàn toàn trên client, KHÔNG WebSocket.
 * Map được nạp từ JSON (do launcher inject qua REST bootstrap/asset/param).
 * Khi terminal: tính điểm bằng ScoreCalculator, tạo MatchResult, gọi resultHandler.
 */
public class LocalCampaignSession implements GameSession {

    private final String playerId;
    private final String mode = "CAMPAIGN";
    private final ResultHandler resultHandler;
    private final MapSource mapSource;

    private GameSimulation sim;
    private String levelId;
    private String runId;
    private String mapVersion;
    private MatchResult matchResult;
    private boolean matchResultFired;
    private boolean started;

    // Map đã nạp, dùng cho restart/(initial) spawn
    private MapDefinition loadedMap;

    /** Callback lưu kết quả (do caller inject: BrowserResultGateway Bước D). */
    public interface ResultHandler {
        /**
         * @param result kết quả đã chốt. Client nên gửi REST idempotent.
         * @return true nếu chấp nhận (ển hiển thị ngay); false nếu lỗi thì vẫn hiển thị.
         */
        boolean onResult(MatchResult result);
    }

    public LocalCampaignSession(String playerId, String levelId, String runId, String mapVersion,
                                String mapJson, ResultHandler resultHandler) {
        this.playerId = playerId;
        this.levelId = levelId;
        this.runId = runId;
        this.mapVersion = mapVersion;
        this.resultHandler = resultHandler;
        this.mapSource = null;

        // Nạp ngay nếu có sẵn mapJson.
        this.sim = new GameSimulation(seedFor(runId, levelId));
        loadAndStart(mapJson);
        started = true;
    }

    /** Lazy variant: MapSource sẽ load trong ensureStarted() (Gdx đã init). */
    public LocalCampaignSession(String playerId, String levelId,
                                MapSource mapSource, ResultHandler resultHandler) {
        this.playerId = playerId;
        this.levelId = levelId;
        this.runId = mapSource.newRunId(levelId);
        this.mapVersion = mapSource.mapVersion(levelId);
        this.resultHandler = resultHandler;
        this.mapSource = mapSource;
        this.sim = new GameSimulation(seedFor(runId, levelId));
        // Chưa start — load trong ensureStarted.
    }

    private void ensureStarted() {
        if (started) return;
        String json = mapSource != null ? mapSource.loadMapJson(levelId) : null;
        if (json != null) {
            loadAndStart(json);
            started = true;
        }
    }

    private void loadAndStart(String mapJson) {
        loadedMap = MapLoader.parse(mapJson);
        if (loadedMap == null) {
            // Không throw — session sẽ ở WAITING và caller có thể hiển thị lỗi.
            System.err.println("[LocalCampaignSession] Không parse được map '" + levelId + "'");
            return;
        }
        sim.setMatchEndHandler(this::onMatchEnd);
        sim.startCampaignLevel(loadedMap, levelId, playerId);
    }

    // ===== INPUT =====
    @Override public void onMove(Direction direction, boolean moving) {
        sim.enqueueMove(toGameplayDirection(direction), moving);
    }
    @Override public void onShoot() { sim.enqueueShoot(); }
    @Override public void onAllReleased() { sim.enqueueStopAll(); }

    private static com.tankzors.client.gameplay.entity.Direction toGameplayDirection(Direction d) {
        if (d == null) return null;
        switch (d) {
            case UP: return com.tankzors.client.gameplay.entity.Direction.UP;
            case DOWN: return com.tankzors.client.gameplay.entity.Direction.DOWN;
            case LEFT: return com.tankzors.client.gameplay.entity.Direction.LEFT;
            case RIGHT: return com.tankzors.client.gameplay.entity.Direction.RIGHT;
        }
        return null;
    }

    // ===== UPDATE =====
    @Override public void update(float rawDeltaTime) {
        ensureStarted();
        if (started) sim.advance(rawDeltaTime);
    }

    @Override public GameStateSnapshot snapshot() {
        GameWorld w = sim.getWorld();
        int elapsedSec = (int) Math.max(0, sim.getElapsedSeconds());
        return WorldSnapshotMapper.snapshot(w, playerId, w.getScore(), elapsedSec);
    }

    @Override public String localPlayerId() { return playerId; }
    @Override public String status() {
        if (!started) return "WAITING";
        return sim.getWorld().getStatus().name();
    }

    // ===== RESTART / NEXT =====
    @Override
    public void restart() {
        matchResult = null;
        matchResultFired = false;
        runId = "local-" + levelId + "-" + nextSeed();
        sim = new GameSimulation(seedFor(runId, levelId));
        started = false;
        ensureStarted();
    }

    @Override
    public void nextLevel() {
        String next = nextCampaignLevel(levelId);
        if (next == null) return;
        matchResult = null;
        matchResultFired = false;
        levelId = next;
        runId = "local-" + next + "-" + nextSeed();
        // Nạp map kế từ MapSource (nếu có), hoặc caller phải gọi changeLevel().
        sim = new GameSimulation(seedFor(runId, next));
        started = false;
        ensureStarted();
    }

    /** Nạp map mới (dùng cho next level khi có JSON từ REST). */
    public void changeLevel(String newLevelId, String mapJson, String runId, String mapVersion) {
        this.levelId = newLevelId;
        this.runId = runId != null ? runId : "local-" + newLevelId + "-" + nextSeed();
        this.mapVersion = mapVersion;
        matchResult = null;
        matchResultFired = false;
        sim = new GameSimulation(seedFor(this.runId, newLevelId));
        sim.setMatchEndHandler(this::onMatchEnd);
        loadAndStart(mapJson);
    }

    private void onMatchEnd(String lvl, String status, int hpRemain, int aiKills, int elapsedSeconds) {
        if (matchResultFired) return;
        matchResultFired = true;
        int score = ScoreCalculator.calculate(hpRemain, aiKills, elapsedSeconds);
        matchResult = new MatchResult(runId, mode, lvl, mapVersion != null ? mapVersion : "",
                status, score, hpRemain, aiKills, elapsedSeconds);
        if (resultHandler != null) {
            try { resultHandler.onResult(matchResult); }
            catch (Exception ignored) { /* kết quả vẫn hiển thị */ }
        }
    }

    @Override public MatchResult matchResult() { return matchResult; }
    @Override public boolean requiresSocket() { return false; }
    @Override public void dispose() {}

    public String getLevelId() { return levelId; }
    public String getRunId() { return runId; }

    // ===== helpers =====
    private static long seedFor(String runId, String levelId) {
        // Deterministic: cùng runId+level → cùng seed → AI reproducible.
        long h = 0;
        String s = runId + ":" + levelId;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        if (h == 0) h = 1;
        return h;
    }

    private static long nextSeq = 1;
    private static long nextSeed() { return nextSeq++; } // monotonic; không Math.random
    private static String nextCampaignLevel(String current) {
        if (GameClient.DEFAULT_LEVEL.equals(current)) return "campaign_2";
        if ("campaign_2".equals(current)) return "campaign_3";
        return null;
    }
}
