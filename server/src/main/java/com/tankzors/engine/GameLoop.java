package com.tankzors.engine;

import com.tankzors.ServerConfig;
import com.tankzors.entity.Direction;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;
import com.tankzors.interfaces.ITank;
import com.tankzors.network.MessageSender;
import com.tankzors.network.protocol.InputMessage;
import com.tankzors.network.protocol.StateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
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
    private final MessageSender messageSender;

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

    // ========== CONSTRUCTOR ==========

    /**
     * Khởi tạo GameLoop với GameState và MessageSender.
     *
     * @param gameState    trạng thái game dùng chung
     * @param messageSender dịch vụ gửi message qua WebSocket
     */
    public GameLoop(GameState gameState, MessageSender messageSender) {
        this.gameState = gameState;
        this.messageSender = messageSender;
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
            processInputs();
            updateEntities(deltaTime);
            checkCollisions();
            broadcastState();

            // 6. Cập nhật metrics
            gameState.incrementTick();
            updateMetrics();

        } catch (Exception e) {
            // Bắt mọi exception để tránh scheduler bị cancel khi có lỗi
            logger.error("[GameLoop] Lỗi trong tick #{}: {}",
                    gameState.getTickCount(), e.getMessage(), e);
        }
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
            applyInput(input);
        }
    }

    /**
     * Áp dụng một input từ người chơi lên game state.
     */
    private void applyInput(PlayerInput input) {
        String sessionId = input.sessionId;
        InputMessage msg = input.message;
        String action = msg.getAction();

        if (action == null) return;

        switch (action.toUpperCase()) {
            case "MOVE":
            case "MOVE_INPUT":
                handleMoveInput(sessionId, msg);
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
        ITank tank = gameState.getTank(sessionId);
        if (tank == null || !tank.isAlive()) return;

        Direction direction = msg.getDirection();
        Boolean moving = msg.getMoving();

        if (direction != null && Boolean.TRUE.equals(moving)) {
            // Xoay góc theo hướng di chuyển
            switch (direction) {
                case UP: tank.rotate(0); break;
                case RIGHT: tank.rotate(90); break;
                case DOWN: tank.rotate(180); break;
                case LEFT: tank.rotate(270); break;
            }

            double speed = (tank instanceof com.tankzors.entity.Tank)
                    ? ((com.tankzors.entity.Tank) tank).getSpeed()
                    : 150.0;
            double distance = speed * deltaTime;
            Position currentPos = tank.getPosition();
            Position targetPos = new Position(currentPos.getX(), currentPos.getY());

            switch (direction) {
                case UP: targetPos.setY(currentPos.getY() - distance); break;
                case DOWN: targetPos.setY(currentPos.getY() + distance); break;
                case LEFT: targetPos.setX(currentPos.getX() - distance); break;
                case RIGHT: targetPos.setX(currentPos.getX() + distance); break;
            }

            Position validPos = CollisionManager.resolveCollision(tank, targetPos, gameState.getCurrentMap());
            currentPos.setX(validPos.getX());
            currentPos.setY(validPos.getY());
        }
    }

    /**
     * Xử lý input bắn: tạo viên đạn mới từ tank.
     */
    private void handleShootInput(String sessionId) {
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

        if (gameState.getTank(sessionId) == null) {
            Position spawn = (gameState.getCurrentMap() != null && !gameState.getCurrentMap().getSpawnPoints().isEmpty())
                    ? gameState.getCurrentMap().getSpawnPoints().get(0)
                    : new Position(100, 100);
            com.tankzors.entity.PlayerTank playerTank = new com.tankzors.entity.PlayerTank(sessionId, spawn.getX(), spawn.getY());
            gameState.addTank(playerTank);
            logger.info("[GameLoop] Tạo PlayerTank cho {} tại vị trí ({}, {})", sessionId, spawn.getX(), spawn.getY());
        }

        // Chuyển sang trạng thái PLAYING nếu đang WAITING
        if (gameState.getStatus() == GameState.Status.WAITING) {
            gameState.setStatus(GameState.Status.PLAYING);
            logger.info("[GameLoop] Game status -> PLAYING");
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
        // Đạn vs Tank
        for (IBullet bullet : gameState.getAllBullets()) {
            if (!bullet.isActive()) continue;

            for (ITank tank : gameState.getAllTanks()) {
                if (!tank.isAlive()) continue;
                // Không tự bắn chính mình
                if (bullet.getOwnerId().equals(tank.getId())) continue;

                // Kiểm tra va chạm AABB nếu cả 2 implement ICollidable
                if (bullet instanceof ICollidable && tank instanceof ICollidable) {
                    ICollidable bulletBox = (ICollidable) bullet;
                    ICollidable tankBox = (ICollidable) tank;

                    if (bulletBox.collidesWith(tankBox)) {
                        // Trúng đạn: giảm HP và hủy viên đạn
                        tank.takeDamage(bullet.getDamage());
                        bullet.deactivate();

                        logger.debug("[Collision] Bullet({}) hit Tank({}), HP còn: {}",
                                bullet.getOwnerId(), tank.getId(), tank.getHP());

                        // Nếu tank chết -> cộng điểm
                        if (!tank.isAlive()) {
                            gameState.addScore(100);
                            logger.info("[GameLoop] Tank {} bị tiêu diệt! Score: {}",
                                    tank.getId(), gameState.getScore());
                        }
                        break; // Mỗi viên đạn chỉ trúng 1 target
                    }
                }
            }
        }

        // Đạn vs Tường (kiểm tra khi IMap đã được load)
        if (gameState.getCurrentMap() != null) {
            for (IBullet bullet : gameState.getAllBullets()) {
                if (!bullet.isActive()) continue;

                // Chuyển pixel position sang tile coordinate
                int tileX = (int) (bullet.getPosition().getX() / ServerConfig.TILE_SIZE);
                int tileY = (int) (bullet.getPosition().getY() / ServerConfig.TILE_SIZE);

                // Kiểm tra out of bounds
                if (tileX < 0 || tileX >= gameState.getCurrentMap().getWidth()
                        || tileY < 0 || tileY >= gameState.getCurrentMap().getHeight()) {
                    bullet.deactivate();
                    continue;
                }

                // Kiểm tra đạn chạm tường
                if (!gameState.getCurrentMap().isWalkable(tileX, tileY)) {
                    bullet.deactivate();
                }
            }
        }
    }

    // ========== STEP 4: BROADCAST STATE ==========

    /**
     * Gửi trạng thái game hiện tại tới toàn bộ client đang kết nối.
     * Chỉ broadcast khi game đang PLAYING và có ít nhất 1 tank.
     */
    private void broadcastState() {
        if (!gameState.isPlaying()) return;
        if (gameState.getTankCount() == 0) return;

        StateMessage stateMsg = gameState.toStateMessage();
        messageSender.broadcast(stateMsg);
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
