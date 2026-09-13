package com.tankzors.engine;

import com.tankzors.ServerConfig;
import com.tankzors.ai.DifficultyLevel;
import com.tankzors.entity.AITank;
import com.tankzors.map.GameMap;
import com.tankzors.map.TileType;
import com.tankzors.entity.Direction;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;
import com.tankzors.interfaces.ITank;
import com.tankzors.network.protocol.InputMessage;
import com.tankzors.network.protocol.StateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Vòng lặp game chính (Game Loop) chạy ở tần suất 30Hz.
 *
 * <p>Mỗi tick (33ms), engine thực hiện tuần tự:
 * <ol>
 *   <li>{@link #processInputs()} - Nhận input từ hàng đợi</li>
 *   <li>{@link #updateEntities(double)} - Cập nhật vị trí tank/bullet</li>
 *   <li>{@link #checkCollisions()} - Kiểm tra va chạm</li>
 *   <li>{@link #broadcastState()} - Gửi trạng thái mới về client</li>
 * </ol>
 * </p>
 *
 * <p>Sử dụng {@link ScheduledExecutorService} với fixed-rate scheduling
 * để đảm bảo tần suất ổn định 30 tick/giây.</p>
 */
public class GameLoop {

    private static final Logger logger = LoggerFactory.getLogger(GameLoop.class);

    // ========== CORE COMPONENTS ==========

    private final GameState gameState;
    private final StatePublisherPort statePublisher;
    private MatchEndObserver matchEndObserver;

    // Snapshot time để tính elapsed khi match kết thúc
    private volatile long matchStartTime = 0L;
    // sessionId của player (set khi JOIN_GAME)
    private volatile String lastPlayerSessionId;
    // Đếm AI chết để truyền vào score calc
    private volatile int aiKilledThisMatch = 0;
    // Flag để chỉ gọi observer 1 lần mỗi match
    private volatile boolean matchEndFired = false;

    // ========== SCHEDULING ==========

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> gameLoopTask;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // ========== INPUT QUEUE ==========

    /**
     * Hàng đợi input thread-safe.
     * Network thread đẩy vào, game loop thread lấy ra.
     */
    private final ConcurrentLinkedQueue<PlayerInput> inputQueue = new ConcurrentLinkedQueue<>();

    // ========== TIMING & METRICS ==========

    private long lastTickTime;
    private double deltaTime; // Thời gian giữa 2 tick (giây)

    // Đo tick rate thực tế
    private final AtomicInteger ticksThisSecond = new AtomicInteger(0);
    private long lastMetricTime;
    private volatile int actualTickRate = 0;

    private String currentLevelId = "campaign_1";

    // ========== CONSTRUCTOR ==========

    /**
     * Khởi tạo GameLoop với GameState và StatePublisherPort.
     *
     * <p>Phase 2: thay cho constructor Javalin cũ (nhận {@code MessageSender}
     * cụ thể). Engine không còn biết chi tiết transport.</p>
     *
     * @param gameState     trạng thái game dùng chung
     * @param statePublisher port phát state (viết lại bằng Spring WebSocket)
     */
    public GameLoop(GameState gameState, StatePublisherPort statePublisher) {
        this.gameState = gameState;
        this.statePublisher = statePublisher;
    }

    /**
     * Đăng ký observer nhận callback khi match end (VICTORY/DEFEAT). Engine không
     * biết observer implementation là gì (LeaderboardService, MatchEndService...).
     */
    public void setMatchEndObserver(MatchEndObserver observer) {
        this.matchEndObserver = observer;
    }

    public String getCurrentLevelId() {
        return currentLevelId;
    }

    // ========== LIFECYCLE ==========

    /**
     * Khởi động game loop ở tần suất 30Hz.
     * Thread-safe: chỉ khởi động 1 lần duy nhất.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            lastTickTime = System.nanoTime();
            lastMetricTime = System.currentTimeMillis();

            // Tạo single-thread scheduler (game logic chạy trên 1 thread duy nhất = thread-safe)
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "GameLoop-Thread");
                t.setDaemon(true); // Daemon thread: tự dừng khi JVM shutdown
                return t;
            });

            // Schedule fixed-rate: cố gắng chạy mỗi 33ms bất kể tick trước mất bao lâu
            gameLoopTask = scheduler.scheduleAtFixedRate(
                    this::tick,
                    0,
                    ServerConfig.TICK_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );

            logger.info("====================================================");
            logger.info("[GameLoop] STARTED - Target: {}Hz ({}ms/tick)",
                    ServerConfig.TICK_RATE, ServerConfig.TICK_INTERVAL_MS);
            logger.info("====================================================");
        }
    }

    /**
     * Dừng game loop. Đợi tối đa 1 giây cho tick hiện tại hoàn thành.
     * Gọi khi server shutdown (Ctrl+C) hoặc trận đấu kết thúc.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("[GameLoop] Đang dừng game loop...");

            if (gameLoopTask != null) {
                gameLoopTask.cancel(false); // Không interrupt tick đang chạy
            }

            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    // Đợi tối đa 1 giây cho tick cuối hoàn thành
                    if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                        logger.warn("[GameLoop] Forced shutdown sau 1s timeout");
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            logger.info("[GameLoop] STOPPED. Tổng tick đã chạy: {}", gameState.getTickCount());
        }
    }

    /**
     * Kiểm tra game loop có đang chạy không.
     */
    public boolean isRunning() {
        return running.get();
    }

    // ========== MAIN TICK ==========

    /**
     * Một tick của game loop. Được gọi 30 lần/giây bởi scheduler.
     *
     * <p>Quy trình:
     * 1. Tính deltaTime
     * 2. processInputs() - xử lý input từ hàng đợi
     * 3. updateEntities() - cập nhật vị trí entities
     * 4. checkCollisions() - kiểm tra va chạm
     * 5. broadcastState() - gửi trạng thái mới về client
     * 6. Cập nhật metrics
     * </p>
     */
    private void tick() {
        try {
            // 1. Tính deltaTime (thời gian thực giữa 2 tick)
            long now = System.nanoTime();
            deltaTime = (now - lastTickTime) / 1_000_000_000.0; // nano -> giây
            lastTickTime = now;

            // 2-5: Pipeline xử lý game
            step(deltaTime);

            // 6. Cập nhật metrics
            updateMetrics();

        } catch (Exception e) {
            // Bắt mọi exception để tránh scheduler bị cancel khi có lỗi
            logger.error("[GameLoop] Lỗi trong tick #{}: {}",
                    gameState.getTickCount(), e.getMessage(), e);
        }
    }

    /** One deterministic simulation step, also used by engine integration tests. */
    void step(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) return;
        deltaTime = dt;
        processInputs();
        if (gameState.isPlaying()) {
            updateEntities(dt);
            checkCollisions();
            broadcastState();
            cleanupDeadTanks();
        } else if (gameState.getStatus() == GameState.Status.VICTORY || gameState.getStatus() == GameState.Status.DEFEAT) {
            broadcastState();
        }
        gameState.incrementTick();
    }

    // ========== STEP 1: PROCESS INPUTS ==========

    /**
     * Lấy tất cả input đang chờ trong hàng đợi và áp dụng vào game state.
     *
     * <p>Input từ client (di chuyển, bắn) được network thread đẩy vào queue,
     * game loop thread lấy ra và xử lý tuần tự ở đây.</p>
     */
    private void processInputs() {
        PlayerInput input;
        while ((input = inputQueue.poll()) != null) {
            applyInput(input.sessionId, input.message);
        }
    }

    /**
     * Áp dụng một input từ người chơi lên game state.
     */
    public void applyInput(String sessionId, InputMessage msg) {
        String action = msg.getAction();

        if (action == null) return;

        switch (action.toUpperCase()) {
            case "MOVE":
            case "MOVE_INPUT":
                handleMoveInput(sessionId, msg);
                break;

            case "STOP":
                if (gameState.getTank(sessionId) instanceof com.tankzors.entity.PlayerTank player) {
                    if (msg.getDirection() == null) player.stopMoving();
                    else player.setMoving(msg.getDirection(), false);
                }
                break;

            case "LEAVE_GAME":
                gameState.removeTank(sessionId);
                if (gameState.getAllTanks().stream().noneMatch(t -> t instanceof com.tankzors.entity.PlayerTank)) {
                    gameState.setStatus(GameState.Status.WAITING);
                    clearEnemiesAndBullets();
                }
                break;

            case "SHOOT":
            case "SHOOT_INPUT":
                handleShootInput(sessionId);
                break;

            case "JOIN_GAME":
                handleJoinGame(sessionId, msg);
                break;

            default:
                logger.debug("[GameLoop] Input action không xác định: {}", action);
                break;
        }
    }

    /**
     * Xử lý input di chuyển: cập nhật hướng và trạng thái moving của tank với kiểm tra va chạm tường.
     */
    private void handleMoveInput(String sessionId, InputMessage msg) {
        if (!gameState.isPlaying()) return;
        if (gameState.getTank(sessionId) instanceof com.tankzors.entity.PlayerTank player && player.isAlive()) {
            player.setMoving(msg.getDirection(), !Boolean.FALSE.equals(msg.getMoving()));
        }
    }

    /**
     * Xử lý input bắn: tạo viên đạn mới từ tank.
     */
    private void handleShootInput(String sessionId) {
        if (!gameState.isPlaying()) return;
        ITank tank = gameState.getTank(sessionId);
        if (tank == null || !tank.isAlive()) return;

        IBullet bullet = tank.shoot();
        if (bullet != null) {
            gameState.addBullet(bullet);
        }
    }

    /**
     * Xử lý khi người chơi join game.
     */
    private void handleJoinGame(String sessionId, InputMessage msg) {
        logger.info("[GameLoop] Player join: sessionId={}, name={}, level={}",
                sessionId, msg.getPlayerName(), msg.getLevelId());

        if ("campaign".equalsIgnoreCase(msg.getGameMode())) {
            // Trong chế độ campaign, chỉ có 1 người chơi; dọn các session cũ nếu có
            List<String> oldPlayerIds = gameState.getAllTanks().stream()
                    .filter(t -> t instanceof com.tankzors.entity.PlayerTank && !t.getId().equals(sessionId))
                    .map(ITank::getId)
                    .toList();
            for (String pid : oldPlayerIds) {
                gameState.removeTank(pid);
            }
        }

        boolean levelChanged = false;
        String requestedLevel = msg.getLevelId();
        if (requestedLevel != null && !requestedLevel.isBlank()) {
            if (!requestedLevel.equals(currentLevelId) || gameState.getCurrentMap() == null) {
                com.tankzors.map.GameMap newMap = com.tankzors.map.MapLoader.load(requestedLevel);
                if (newMap != null) {
                    gameState.setCurrentMap(newMap);
                    currentLevelId = requestedLevel;
                    levelChanged = true;
                    logger.info("[GameLoop] Đã nạp bản đồ mới cho level: {}", requestedLevel);
                }
            }
        }

        boolean shouldResetMatch = levelChanged
                || gameState.getStatus() != GameState.Status.PLAYING
                || gameState.getAllTanks().stream().noneMatch(t -> t instanceof com.tankzors.entity.PlayerTank && t.isAlive());

        if (shouldResetMatch) {
            clearEnemiesAndBullets();
            spawnMapEnemies();
            gameState.setStatus(GameState.Status.PLAYING);
            matchStartTime = System.currentTimeMillis();
            aiKilledThisMatch = 0;
            matchEndFired = false;
            lastPlayerSessionId = sessionId;
            logger.info("[GameLoop] Bắt đầu trận mới: reset AI và đạn, game status -> PLAYING");
        }

        if (gameState.getTank(sessionId) == null || shouldResetMatch) {
            Position spawn = findPlayerSpawn();
            gameState.removeTank(sessionId);
            com.tankzors.entity.PlayerTank playerTank = new com.tankzors.entity.PlayerTank(sessionId, spawn.getX(), spawn.getY());
            gameState.addTank(playerTank);
            logger.info("[GameLoop] Tạo/Reset PlayerTank cho {} tại vị trí ({}, {})", sessionId, spawn.getX(), spawn.getY());
        }

        broadcastState();
    }

    private void clearEnemiesAndBullets() {
        List<String> aiIds = gameState.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(ITank::getId)
                .toList();
        for (String id : aiIds) {
            gameState.removeTank(id);
        }
        for (IBullet bullet : gameState.getAllBullets()) {
            bullet.deactivate();
        }
        gameState.removeInactiveBullets();
    }

    private Position findPlayerSpawn() {
        var map = gameState.getCurrentMap();
        if (map != null) {
            for (int row = 0; row < map.getHeight(); row++) {
                for (int col = 0; col < map.getWidth(); col++) {
                    if (map.getTileAt(col, row) == TileType.SPAWN_PLAYER) {
                        return new Position((col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize());
                    }
                }
            }
        }
        return new Position(100, 100);
    }

    private void spawnMapEnemies() {
        var map = gameState.getCurrentMap();
        if (map == null || gameState.getAllTanks().stream().anyMatch(t -> t instanceof AITank)) return;

        // Nếu map có danh sách enemies metadata (Task 2.3)
        if (map instanceof GameMap gameMap && gameMap.getEnemies() != null && !gameMap.getEnemies().isEmpty()) {
            for (GameMap.EnemyConfig config : gameMap.getEnemies()) {
                DifficultyLevel diff = DifficultyLevel.EASY;
                if (config.getDifficulty() != null) {
                    try {
                        diff = DifficultyLevel.valueOf(config.getDifficulty().trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                int col = config.getSpawnX();
                int row = config.getSpawnY();
                AITank enemy = new AITank("ai-" + col + "-" + row,
                        (col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize(), diff);
                if (!CollisionManager.checkTankWallCollision(enemy, map, enemy.getPosition())) {
                    gameState.addTank(enemy);
                    logger.info("[AI] Spawn AI {} tại ({}, {}) với độ khó {}", enemy.getId(), col, row, diff);
                } else {
                    logger.warn("[AI] Bỏ qua spawn AI không hợp lệ tại ({}, {})", col, row);
                }
            }
            return;
        }

        // Fallback quét ô SPAWN_ENEMY nếu map không có enemies metadata
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                if (map.getTileAt(col, row) != TileType.SPAWN_ENEMY) continue;
                AITank enemy = new AITank("ai-" + col + "-" + row,
                        (col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize(), DifficultyLevel.EASY);
                if (!CollisionManager.checkTankWallCollision(enemy, map, enemy.getPosition())) {
                    gameState.addTank(enemy);
                } else {
                    logger.warn("[AI] Bỏ qua spawn AI không hợp lệ tại ({}, {})", col, row);
                }
            }
        }
    }

    // ========== STEP 2: UPDATE ENTITIES ==========

    /**
     * Cập nhật toàn bộ entities mỗi tick.
     * - Viên đạn tiến thêm một đoạn dựa trên tốc độ * deltaTime
     * - Xóa viên đạn đã bị deactivate
     *
     * @param dt deltaTime (giây)
     */
    private void updateEntities(double dt) {
        // Cập nhật cooldown bắn cho PlayerTank
        for (ITank tank : gameState.getAllTanks()) {
            if (tank.isAlive() && tank instanceof com.tankzors.entity.PlayerTank) {
                var player = (com.tankzors.entity.PlayerTank) tank;
                player.updateCooldown(dt);
                player.updateMovement(dt, gameState.getCurrentMap());
            }
        }

        // AI runs once per simulation tick, before bullets and state broadcast.
        for (ITank tank : gameState.getAllTanks()) {
            if (tank instanceof AITank ai) ai.update(gameState, dt);
        }

        // Cập nhật tất cả viên đạn
        for (IBullet bullet : gameState.getAllBullets()) {
            if (bullet.isActive()) {
                bullet.update(dt);
            }
        }

        // Xóa viên đạn không còn hoạt động
        gameState.removeInactiveBullets();
    }

    // ========== STEP 3: CHECK COLLISIONS ==========

    /**
     * Kiểm tra va chạm giữa các entities:
     * - Đạn vs Tường: hủy đạn
     * - Đạn vs Tank: giảm HP tank, hủy đạn
     * - Tank vs Tường: chặn di chuyển (handled in move logic)
     */
    private void checkCollisions() {
        for (IBullet bullet : gameState.getAllBullets()) {
            if (!bullet.isActive()) continue;
            // Use the actual map size and bullet footprint; walls absorb the shot first.
            if (CollisionManager.checkBulletWallCollision(bullet, gameState.getCurrentMap())) {
                bullet.deactivate();
                continue;
            }
            for (ITank tank : gameState.getAllTanks()) {
                if (CollisionManager.checkBulletTankCollision(bullet, tank)) {
                    tank.takeDamage(bullet.getDamage());
                    bullet.deactivate();
                    if (!tank.isAlive()) {
                        gameState.addScore(100);
                        if (tank instanceof AITank) aiKilledThisMatch++;
                    }
                    break;
                }
            }
        }
    }

    // ========== STEP 4: BROADCAST STATE ==========

    /**
     * Gửi trạng thái game hiện tại tới toàn bộ client đang kết nối.
     * Bỏ qua khi game đang ở trạng thái WAITING.
     */
    private void broadcastState() {
        if (gameState.getStatus() == GameState.Status.WAITING) return;

        if (statePublisher == null) return;
        StateMessage stateMsg = gameState.toStateMessage();
        statePublisher.broadcast(stateMsg);
    }

    // ========== STEP 5: CLEANUP DEAD TANKS ==========

    /**
     * Xóa xe tăng đã chết (HP <= 0) khỏi game state.
     * Được gọi SAU broadcastState() để client nhận được trạng thái alive=false
     * một lần trước khi tank biến mất hoàn toàn.
     */
    private void cleanupDeadTanks() {
        List<String> deadIds = new ArrayList<>();
        for (ITank tank : gameState.getAllTanks()) {
            if (!tank.isAlive()) {
                deadIds.add(tank.getId());
            }
        }
        for (String id : deadIds) {
            gameState.removeTank(id);
            logger.info("[GameLoop] Tank {} bị phá hủy và xóa khỏi game.", id);
        }

        boolean hasPlayer = gameState.getAllTanks().stream().anyMatch(t -> t instanceof com.tankzors.entity.PlayerTank && t.isAlive());
        boolean hasAI = gameState.getAllTanks().stream().anyMatch(t -> t instanceof AITank && t.isAlive());

        if (!hasPlayer) {
            gameState.setStatus(GameState.Status.DEFEAT);
            logger.info("[GameLoop] Người chơi đã bị tiêu diệt -> DEFEAT");
            broadcastState();
            fireMatchEnd("DEFEAT", 0);
        } else if (!hasAI) {
            gameState.setStatus(GameState.Status.VICTORY);
            logger.info("[GameLoop] Toàn bộ AI đã bị tiêu diệt -> VICTORY");
            broadcastState();
            int hpRemain = gameState.getAllTanks().stream()
                .filter(t -> t instanceof com.tankzors.entity.PlayerTank)
                .map(ITank::getHP)
                .findFirst().orElse(0);
            fireMatchEnd("VICTORY", hpRemain);
        }
    }

    private void fireMatchEnd(String status, int hpRemain) {
        if (matchEndFired) return;
        if (matchEndObserver == null) return;
        matchEndFired = true;

        String sid = lastPlayerSessionId != null ? lastPlayerSessionId : "unknown";
        long elapsedMs = matchStartTime > 0 ? (System.currentTimeMillis() - matchStartTime) : 0;
        int elapsedSeconds = (int) Math.max(0, elapsedMs / 1000);

        try {
            matchEndObserver.onMatchEnd(sid, currentLevelId, status, hpRemain, aiKilledThisMatch, elapsedSeconds);
        } catch (Exception e) {
            logger.warn("[GameLoop] MatchEndObserver throw: {}", e.getMessage());
        }
    }

    // ========== INPUT QUEUE (PUBLIC API) ==========

    /**
     * Đẩy input từ network thread vào hàng đợi.
     * Thread-safe: được gọi từ WebSocket message handler.
     *
     * @param sessionId ID phiên của người chơi
     * @param message   input message từ client
     */
    public void queueInput(String sessionId, InputMessage message) {
        if (running.get()) {
            inputQueue.offer(new PlayerInput(sessionId, message));
        }
    }

    // ========== METRICS ==========

    /**
     * Cập nhật và log metrics mỗi giây (tick rate thực tế).
     */
    private void updateMetrics() {
        ticksThisSecond.incrementAndGet();

        long now = System.currentTimeMillis();
        if (now - lastMetricTime >= 1000) {
            actualTickRate = ticksThisSecond.getAndSet(0);
            lastMetricTime = now;

            logger.info("[GameLoop] Tick rate: {}/s | deltaTime: {}s | tanks: {} | bullets: {}",
                    actualTickRate,
                    String.format("%.4f", deltaTime),
                    gameState.getTankCount(),
                    gameState.getBulletCount());
        }
    }

    // ========== GETTERS ==========

    public GameState getGameState() {
        return gameState;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public int getActualTickRate() {
        return actualTickRate;
    }

    // ========== INNER CLASS ==========

    /**
     * Wrapper cho input của người chơi trong hàng đợi.
     */
    private static class PlayerInput {
        final String sessionId;
        final InputMessage message;

        PlayerInput(String sessionId, InputMessage message) {
            this.sessionId = sessionId;
            this.message = message;
        }
    }
}
