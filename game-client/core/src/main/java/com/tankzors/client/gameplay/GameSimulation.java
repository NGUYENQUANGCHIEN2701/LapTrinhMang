package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.ai.DifficultyLevel;
import com.tankzors.client.gameplay.ai.ShootBehavior;
import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.TileType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Mô phỏng gameplay client-authoritative, fixed-step 1/30s. Port logic
 * deterministic fixed-step simulation chạy trên LibGDX client
 * từ render loop. Mọi giá trị parity server (xem MIGRATION-MAPPING mục 3-4).
 */
public class GameSimulation {

    private final GameWorld world = new GameWorld();
    private final ArrayDeque<Command> commandQueue = new ArrayDeque<>();

    // Identity & input đang được simulation áp dụng
    private String localPlayerId;
    private final Random matchRng;
    private final BulletIdCounter bulletIds = new BulletIdCounter();

    // Match timing
    private double matchElapsedSeconds = 0; // simulation time tích lũy
    private int aiKilledThisMatch = 0;
    private boolean matchEndFired = false;
    private String currentLevelId = "campaign_1";
    private boolean coopMode = false;

    private MatchEndHandler matchEndHandler;
    private com.tankzors.client.shop.PlayerShopState shopState;
    private GameEventListener eventListener;

    public GameSimulation(long matchSeed) {
        this.matchRng = new Random(matchSeed);
    }

    // ========== SETUP ==========
    public void setLocalPlayerId(String id) { this.localPlayerId = id; }
    public void setShopState(com.tankzors.client.shop.PlayerShopState shopState) { this.shopState = shopState; }
    public com.tankzors.client.shop.PlayerShopState getShopState() { return shopState; }

    public void setMatchEndHandler(MatchEndHandler handler) { this.matchEndHandler = handler; }
    public void setEventListener(GameEventListener listener) { this.eventListener = listener; }
    public GameEventListener getEventListener() { return eventListener; }

    public GameWorld getWorld() { return world; }
    public String getCurrentLevelId() { return currentLevelId; }

    // ========== INPUT (queue, áp dụng trong step) ==========
    public void enqueueMove(Direction direction, boolean moving) {
        commandQueue.add(new Command(localPlayerId, Command.Type.MOVE, direction, moving));
    }

    public void enqueueShoot() {
        commandQueue.add(new Command(localPlayerId, Command.Type.SHOOT, null, false));
    }

    public void enqueueStopAll() {
        commandQueue.add(new Command(localPlayerId, Command.Type.STOP, null, false));
    }

    public void enqueueStop(Direction direction) {
        commandQueue.add(new Command(localPlayerId, Command.Type.STOP_DIR, direction, false));
    }

    /** Input từ một player cụ thể (co-op: host áp input guest vào đúng tank). */
    public void enqueueMove(String playerId, Direction direction, boolean moving) {
        commandQueue.add(new Command(playerId, Command.Type.MOVE, direction, moving));
    }
    public void enqueueShoot(String playerId) {
        commandQueue.add(new Command(playerId, Command.Type.SHOOT, null, false));
    }
    public void enqueueStopAll(String playerId) {
        commandQueue.add(new Command(playerId, Command.Type.STOP, null, false));
    }

    /** Bắt đầu/tải một màn campaign: nạp map, spawn AI, đặt trạng thái PLAYING. */
    public void startCampaignLevel(MapDefinition map, String levelId, String playerId) {
        this.localPlayerId = playerId;
        this.currentLevelId = levelId != null ? levelId : map.getName();
        world.setCurrentMap(map);
        resetMatch();
        spawnPlayer(playerId);
        spawnMapEnemies();
        world.setStatus(GameWorld.Status.PLAYING);
    }

    /**
     * Bắt đầu màn co-op: 2 player tank tại spawn khác nhau + AI. Friendly fire
     * CHẶN giữa các player (xem {@link #friendlyFirePlayerOff}).
     * @param playerIds danh sách 2 ID người chơi (host + guest).
     */
    public void startCoopLevel(MapDefinition map, String levelId, java.util.List<String> playerIds) {
        this.currentLevelId = levelId != null ? levelId : map.getName();
        this.coopMode = true;
        this.localPlayerId = playerIds.isEmpty() ? null : playerIds.get(0);
        world.setCurrentMap(map);
        resetMatch();
        // Spawn player tanks - vị trí khác nhau (ô player spawn + ô enemy spawn đầu).
        java.util.List<Position> spots = collectPlayerSpawns(map);
        for (int i = 0; i < playerIds.size(); i++) {
            String pid = playerIds.get(i);
            Position spot = i < spots.size() ? spots.get(i) : new Position(100 + i * 80, 100);
            PlayerTank player = new PlayerTank(pid, spot.getX(), spot.getY());
            player.setBulletIdSource(() -> bulletIds.next(pid));
            world.addTank(player);
        }
        spawnMapEnemies();
        world.setStatus(GameWorld.Status.PLAYING);
    }

    private java.util.List<Position> collectPlayerSpawns(IMap map) {
        java.util.List<Position> spots = new java.util.ArrayList<>();
        Position playerSpawn = findPlayerSpawn();
        spots.add(playerSpawn);
        // Thêm ô enemy spawn đầu làm spawn cho player thứ 2 (co-op).
        IMap m = world.getCurrentMap();
        if (m != null) {
            outer:
            for (int row = 0; row < m.getHeight(); row++) {
                for (int col = 0; col < m.getWidth(); col++) {
                    if (m.getTileAt(col, row) == TileType.SPAWN_ENEMY) {
                        spots.add(new Position((col + 0.5) * m.getTileSize(), (row + 0.5) * m.getTileSize()));
                        break outer;
                    }
                }
            }
        }
        return spots;
    }

    /** Khởi động lại màn hiện tại (R). */
    public void restart() {
        resetMatch();
        IMap map = world.getCurrentMap();
        if (map != null) {
            spawnPlayer(localPlayerId);
            spawnMapEnemies();
            world.setStatus(GameWorld.Status.PLAYING);
        }
    }

    private void resetMatch() {
        world.reset();
        matchElapsedSeconds = 0;
        aiKilledThisMatch = 0;
        matchEndFired = false;
        commandQueue.clear();
    }

    private void spawnPlayer(String playerId) {
        IMap map = world.getCurrentMap();
        Position spawn = findPlayerSpawn();
        double speed = PlayerTank.DEFAULT_SPEED;
        int hp = PlayerTank.DEFAULT_HP;
        if (shopState != null) {
            speed *= shopState.getSpeedMultiplier();
            hp += shopState.getArmorHpBonus();
        }
        PlayerTank player = new PlayerTank(playerId, spawn.getX(), spawn.getY(), speed);
        player.setMaxHp(hp);
        player.setHp(hp);
        player.setBulletIdSource(() -> bulletIds.next("player"));
        if (shopState != null) {
            player.setActiveWeaponId(shopState.getActiveWeaponId());
        }
        world.addTank(player);
    }

    private void spawnMapEnemies() {
        IMap map = world.getCurrentMap();
        if (map == null) return;
        // Đã có AI rồi thì không spawn lại
        for (ITank t : world.getAllTanks()) {
            if (t instanceof AITank) return;
        }
        if (map instanceof MapDefinition) {
            MapDefinition def = (MapDefinition) map;
            if (def.getEnemies() == null || def.getEnemies().isEmpty()) {
                // fall-through tới fallback SPAWN_ENEMY
            } else {
            for (MapDefinition.EnemyConfig cfg : def.getEnemies()) {
                DifficultyLevel diff = parseDifficulty(cfg.getDifficulty());
                int col = cfg.getSpawnX();
                int row = cfg.getSpawnY();
                AITank enemy = new AITank("ai-" + col + "-" + row,
                        (col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize(), diff);
                wireAiBulletIds(enemy);
                if (!Collision.checkTankWallCollision(enemy, map, enemy.getPosition())) {
                    world.addTank(enemy);
                }
            }
            return;
            }
        }
        // Fallback: quét ô SPAWN_ENEMY
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                if (map.getTileAt(col, row) != TileType.SPAWN_ENEMY) continue;
                AITank enemy = new AITank("ai-" + col + "-" + row,
                        (col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize(), DifficultyLevel.EASY);
                wireAiBulletIds(enemy);
                if (!Collision.checkTankWallCollision(enemy, map, enemy.getPosition())) {
                    world.addTank(enemy);
                }
            }
        }
    }

    private void wireAiBulletIds(AITank enemy) {
        // ShootBehavior default đếm id riêng theo owner.
        IAIBehavior sb = enemy.getController().getShootBehavior();
        if (sb instanceof ShootBehavior) {
            ((ShootBehavior) sb).setBulletIdSource(owner -> bulletIds.next(owner));
        }
    }

    private Position findPlayerSpawn() {
        IMap map = world.getCurrentMap();
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

    private static DifficultyLevel parseDifficulty(String s) {
        if (s == null) return DifficultyLevel.EASY;
        try { return DifficultyLevel.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return DifficultyLevel.EASY; }
    }

    // ========== FIXED-STEP DRIVER ==========
    /**
     * Tiêu th rawDeltaTime (giây) từ render loop, bẻ thành các step 1/30.
     * Accumulator giới hạn backlog (MAX_FRAME_DELTA, MAX_STEPS_PER_FRAME).
     */
    public void advance(float rawDeltaTime) {
        double dt = Math.min(rawDeltaTime, GameplayConfig.MAX_FRAME_DELTA);
        accumulator += dt;
        int steps = 0;
        while (accumulator >= GameplayConfig.STEP_SECONDS && steps < GameplayConfig.MAX_STEPS_PER_FRAME) {
            step(GameplayConfig.STEP_SECONDS);
            accumulator -= GameplayConfig.STEP_SECONDS;
            steps++;
        }
        // Bỏ backlog dư có chủ đích: nếu còn dư > 1 step, reset để không chạy bù vô hạn.
        if (accumulator >= GameplayConfig.STEP_SECONDS) {
            accumulator = 0;
        }
    }

    private double accumulator = 0;

    // ========== Canonical fixed simulation step ==========
    public void step(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) return;
        processCommands();
        if (world.isPlaying()) {
            // Tích lũy simulation time chỉ khi đang chơi (pause không tăng).
            matchElapsedSeconds += dt;
            updateEntities(dt);
            checkCollisions();
            cleanupDeadTanks();
        }
        // Terminal: không cập nhật physics nữa. Renderer vẫn vẽ state cuối.
        world.incrementTick();
    }

    private void processCommands() {
        Command c;
        while ((c = commandQueue.poll()) != null) {
            applyCommand(c);
        }
    }

    private void applyCommand(Command c) {
        if (c.playerId == null) return;
        ITank raw = world.getTank(c.playerId);
        if (raw instanceof PlayerTank) {
            PlayerTank player = (PlayerTank) raw;
            switch (c.type) {
                case MOVE:
                    if (world.isPlaying() && player.isAlive()) {
                        boolean moving = !Boolean.FALSE.equals(c.moving);
                        player.setMoving(c.direction, moving);
                    }
                    break;
                case STOP:
                    if (world.isPlaying()) player.stopMoving();
                    break;
                case STOP_DIR:
                    if (world.isPlaying()) player.setMoving(c.direction, false);
                    break;
                case SHOOT:
                    if (world.isPlaying() && player.isAlive()) {
                        IBullet b;
                        if (shopState != null) {
                            int wid = shopState.getActiveWeaponId();
                            int count = shopState.getItemCount(wid);
                            if (wid != 0 && count <= 0) {
                                wid = 0;
                                shopState.setActiveWeaponId(0);
                            }
                            player.setActiveWeaponId(wid);
                            b = player.shoot();
                            if (b != null) {
                                world.addBullet(b);
                                if (wid != 0 && count > 0) {
                                    shopState.setItemCount(wid, count - 1);
                                }
                                if (eventListener != null) eventListener.onShoot(true);
                            }
                        } else {
                            b = player.shoot();
                            if (b != null) {
                                world.addBullet(b);
                                if (eventListener != null) eventListener.onShoot(true);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void updateEntities(double dt) {
        // (a) cooldown + movement player
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof PlayerTank && tank.isAlive()) {
                PlayerTank player = (PlayerTank) tank;
                player.updateCooldown(dt);
                player.updateMovement(dt, world.getCurrentMap());
            }
        }
        // (b) AI
        int bulletsBefore = world.getBulletCount();
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof AITank) ((AITank) tank).update(world, dt);
        }
        if (eventListener != null && world.getBulletCount() > bulletsBefore) {
            eventListener.onShoot(false);
        }
        // (c) bullets
        for (IBullet bullet : world.getAllBullets()) {
            if (bullet.isActive()) bullet.update(dt);
        }
        // (d) cleanup bullets chết
        world.removeInactiveBullets();
    }

    private void checkCollisions() {
        for (IBullet bullet : world.getAllBullets()) {
            if (!bullet.isActive()) continue;
            // Walls hấp thụ trước
            if (Collision.checkBulletWallCollision(bullet, world.getCurrentMap())) {
                bullet.deactivate();
                if (eventListener != null) eventListener.onHitWall();
                continue;
            }
            for (ITank tank : world.getAllTanks()) {
                // Đạn AI xuyên qua đồng đội AI; chỉ player mới là mục tiêu của AI.
                if (isAiFriendlyFire(bullet, tank)) continue;
                if (coopMode && isFriendlyFire(bullet, tank)) continue;
                if (Collision.checkBulletTankCollision(bullet, tank)) {
                    tank.takeDamage(bullet.getDamage());
                    bullet.deactivate();
                    boolean killed = !tank.isAlive();
                    boolean isPlayer = tank instanceof PlayerTank;
                    if (eventListener != null) {
                        if (killed) {
                            eventListener.onTankDestroyed(isPlayer);
                        } else {
                            eventListener.onHitTank(isPlayer);
                        }
                    }
                    if (killed) {
                        world.addScore(100);
                        if (tank instanceof AITank) aiKilledThisMatch++;
                    }
                    break; // 1 đạn 1 tank
                }
            }
        }
    }

    private boolean isFriendlyFire(IBullet bullet, ITank tank) {
        // Co-op: đạn của player không gây damage lên player khác (chỉ chặn cross-player,
        // tự-bắn-mình đã chặn trong Collision.checkBulletTankCollision).
        if (tank instanceof PlayerTank && bullet.getOwnerId() != null) {
            return world.getTank(bullet.getOwnerId()) instanceof PlayerTank;
        }
        return false;
    }

    private boolean isAiFriendlyFire(IBullet bullet, ITank tank) {
        if (!(tank instanceof AITank) || bullet.getOwnerId() == null) return false;
        return world.getTank(bullet.getOwnerId()) instanceof AITank;
    }

    private void cleanupDeadTanks() {
        List<String> deadIds = new ArrayList<>();
        for (ITank tank : world.getAllTanks()) {
            if (!tank.isAlive()) deadIds.add(tank.getId());
        }
        for (String id : deadIds) world.removeTank(id);

        boolean hasPlayer = false, hasAI = false;
        for (ITank t : world.getAllTanks()) {
            if (!t.isAlive()) continue;
            if (t instanceof PlayerTank) hasPlayer = true;
            if (t instanceof AITank) hasAI = true;
        }

        // Tiebreak: player chết ưu tiên DEFEAT (cùng tick).
        if (!hasPlayer) {
            world.setStatus(GameWorld.Status.DEFEAT);
            fireMatchEnd(GameWorld.Status.DEFEAT, 0);
        } else if (!hasAI) {
            world.setStatus(GameWorld.Status.VICTORY);
            int hpRemain = 0;
            for (ITank t : world.getAllTanks()) {
                if (t instanceof PlayerTank) { hpRemain = t.getHP(); break; }
            }
            fireMatchEnd(GameWorld.Status.VICTORY, hpRemain);
        }
    }

    private void fireMatchEnd(GameWorld.Status status, int hpRemain) {
        if (matchEndFired) return;
        matchEndFired = true;
        if (eventListener != null) {
            try {
                eventListener.onMatchEnd(status);
            } catch (Exception ignored) {}
        }
        if (matchEndHandler == null) return;
        int elapsedSeconds = (int) Math.max(0, matchElapsedSeconds);
        try {
            matchEndHandler.onMatchEnd(currentLevelId, status.name(), hpRemain, aiKilledThisMatch, elapsedSeconds);
        } catch (Exception ignored) {
            // không để callback làm rối simulation
        }
    }

    // ========== ACCESSORS cho renderer ==========
    public double getElapsedSeconds() { return matchElapsedSeconds; }
    public int getAiKilledThisMatch() { return aiKilledThisMatch; }

    public Position localPlayerPosition() {
        ITank t = world.getTank(localPlayerId);
        return t != null ? t.getPosition() : null;
    }

    // ========== INNER ==========
    private static final class Command {
        enum Type { MOVE, STOP, STOP_DIR, SHOOT }
        final String playerId;
        final Type type;
        final Direction direction;
        final Boolean moving;
        Command(String playerId, Type type, Direction direction, boolean moving) {
            this.playerId = playerId; this.type = type; this.direction = direction;
            this.moving = moving;
        }
    }

    /** Bộ đếm id đạn deterministic per-match — thay UUID. */
    private static final class BulletIdCounter {
        private long n = 0;
        String next(String ownerId) { return "b-" + ownerId + "-" + (n++); }
    }

    /** Callback khi trận kết thúc (VICTORY/DEFEAT) — client gọi ScoreCalculator + gửi REST. */
    public interface MatchEndHandler {
        void onMatchEnd(String levelId, String status, int hpRemain, int aiKills, int elapsedSeconds);
    }
}
