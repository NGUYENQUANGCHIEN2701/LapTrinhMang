package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.ai.DifficultyLevel;
import com.tankzors.client.gameplay.ai.ShootBehavior;
import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Tank;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.TileType;
import com.tankzors.client.model.EffectSnapshot;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.shop.ShopItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private double basePlayerSpeed = PlayerTank.DEFAULT_SPEED;
    private double basePlayerShootCooldown = 0.5;
    private double speedBoostRemaining;
    private double freezeRemaining;
    private boolean radarActive;
    private boolean shopStateDirty;
    private final List<WorldEffect> effects = new ArrayList<>();
    private final Map<String, RespawnPoint> respawnPoints = new HashMap<>();
    /** Runtime state for the three special rounds; snapshots still use Bullet. */
    private final Map<String, SpecialShot> specialShots = new HashMap<>();

    public static final double SPEED_BOOST_MULTIPLIER = 1.30;
    /** 2700 ticks at the reference build's 60 Hz rate. */
    public static final double SPEED_BOOST_SECONDS = 45.0;
    public static final double EX_CHASSIS_SPEED_BONUS_SECONDS = 15.0;
    public static final double FREEZE_SECONDS = 25.0;
    public static final double EX_CHASSIS_FREEZE_BONUS_SECONDS = 5.0;
    private static final double NITRO_RELOAD_REDUCTION_SECONDS = 5.0 / 60.0;

    private static final class RespawnPoint {
        final double x;
        final double y;
        final boolean player;
        final DifficultyLevel difficulty;

        RespawnPoint(double x, double y, boolean player, DifficultyLevel difficulty) {
            this.x = x;
            this.y = y;
            this.player = player;
            this.difficulty = difficulty;
        }
    }

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

    public void enqueueReverse(boolean reversing) {
        commandQueue.add(new Command(localPlayerId, Command.Type.REVERSE, null, reversing));
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
        this.coopMode = false;
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
        speedBoostRemaining = 0;
        freezeRemaining = 0;
        radarActive = false;
        shopStateDirty = false;
        effects.clear();
        specialShots.clear();
        respawnPoints.clear();
    }

    private void spawnPlayer(String playerId) {
        Position spawn = findPlayerSpawn();
        spawnPlayerAt(playerId, spawn);
    }

    private void spawnPlayerAt(String playerId, Position spawn) {
        double speed = PlayerTank.DEFAULT_SPEED;
        int hp = PlayerTank.DEFAULT_HP;
        if (shopState != null) {
            speed *= shopState.getSpeedMultiplier();
            hp += shopState.getArmorHpBonus();
            if (shopState.hasExChassis()) hp += 2;
        }
        basePlayerSpeed = speed;
        double currentSpeed = speedBoostRemaining > 0 ? speed * SPEED_BOOST_MULTIPLIER : speed;
        PlayerTank player = new PlayerTank(playerId, spawn.getX(), spawn.getY(), currentSpeed);
        player.setMaxHp(hp);
        player.setHp(hp);
        player.setShieldResearchLevel(shopState == null ? 0 : shopState.getResearchLevel("powerFieldResearch"));
        if (shopState != null) {
            player.addShieldCapacity(shopState.getInstalledForceField());
        }
        player.setBulletIdSource(() -> bulletIds.next("player"));
        if (shopState != null) {
            player.setActiveWeaponId(shopState.getActiveWeaponId());
            player.setShootCooldown(shopState.getShellReloadSeconds());
            basePlayerShootCooldown = player.getShootCooldown();
        }
        world.addTank(player);
        respawnPoints.putIfAbsent(playerId,
                new RespawnPoint(spawn.getX(), spawn.getY(), true, null));
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
                spawnEnemyAt("ai-" + col + "-" + row,
                        (col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize(), diff);
            }
            return;
            }
        }
        // Fallback: quét ô SPAWN_ENEMY
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                if (map.getTileAt(col, row) != TileType.SPAWN_ENEMY) continue;
                spawnEnemyAt("ai-" + col + "-" + row,
                        (col + 0.5) * map.getTileSize(), (row + 0.5) * map.getTileSize(), DifficultyLevel.EASY);
            }
        }
    }

    private void spawnEnemyAt(String id, double x, double y, DifficultyLevel difficulty) {
        IMap map = world.getCurrentMap();
        AITank enemy = new AITank(id, x, y, difficulty);
        wireAiBulletIds(enemy);
        if (map == null || !Collision.checkTankWallCollision(enemy, map, enemy.getPosition())) {
            world.addTank(enemy);
            respawnPoints.putIfAbsent(id, new RespawnPoint(x, y, false, difficulty));
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
            if (!checkCaptureFlagGoal()) {
                checkCollisions();
                cleanupDeadTanks();
            }
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
            if (redirectGuidedRocketControl(c)) return;
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
                case REVERSE:
                    if (world.isPlaying() && player.isAlive()) {
                        player.setReversing(!Boolean.FALSE.equals(c.moving));
                    }
                    break;
                case SHOOT:
                    if (world.isPlaying() && player.isAlive()) {
                        IBullet b;
                        if (shopState != null) {
                            int wid = shopState.getActiveWeaponId();
                            int count = shopState.getItemCount(wid);
                            if (count <= 0) {
                                wid = shopState.selectFirstAvailableWeapon();
                                count = shopState.getItemCount(wid);
                            }
                            if (count <= 0) break;
                            player.setActiveWeaponId(wid);
                            if (wid >= ShopItem.ARTILLERY.getId() && wid <= ShopItem.ROCKET.getId()) {
                                if (wid == ShopItem.ROCKET.getId() && detonateGuidedRocket()) break;
                                if (fireSpecialShot(player, wid)) {
                                    shopState.setItemCount(wid, count - 1);
                                    shopStateDirty = true;
                                    if (eventListener != null) eventListener.onShoot(true, wid);
                                }
                                break;
                            }
                            b = player.shoot();
                            if (b != null) {
                                world.addBullet(b);
                                shopState.setItemCount(wid, count - 1);
                                if (eventListener != null) eventListener.onShoot(true, weaponTypeOf(b));
                            }
                        } else {
                            b = player.shoot();
                            if (b != null) {
                                world.addBullet(b);
                                if (eventListener != null) eventListener.onShoot(true, weaponTypeOf(b));
                            }
                        }
                    }
                    break;
            }
        }
    }

    private boolean redirectGuidedRocketControl(Command command) {
        if (command.type != Command.Type.MOVE || Boolean.FALSE.equals(command.moving)) return false;
        SpecialShot guided = null;
        for (SpecialShot shot : specialShots.values()) {
            if (shot.type == ShopItem.ROCKET.getId() && shot.ownerId.equals(command.playerId)) {
                guided = shot;
                break;
            }
        }
        if (guided == null) return false;
        Bullet rocket = null;
        for (IBullet candidate : world.getAllBullets()) {
            if (candidate instanceof Bullet && ((Bullet) candidate).getId().equals(guided.bulletId)) {
                rocket = (Bullet) candidate;
                break;
            }
        }
        if (rocket == null) return false;
        rocket.setDirection(directionFor(command.direction));
        return true;
    }

    private boolean fireSpecialShot(PlayerTank player, int itemId) {
        // Keep the same fire-key cooldown gate as the reference player controller.
        if (player.shoot() == null) return false;
        int artilleryLevel = shopState.getResearchLevel("artilleryResearch");
        double direction = directionForRotation(player.getRotation());
        int power = itemId == ShopItem.ARTILLERY.getId() ? artilleryPower(artilleryLevel)
                : itemId == ShopItem.WARHEAD.getId() ? 9 : 32;
        int radius = itemId == ShopItem.ARTILLERY.getId() ? artilleryRadius(artilleryLevel)
                : itemId == ShopItem.WARHEAD.getId() ? 2 : 3;
        double speed = itemId == ShopItem.ARTILLERY.getId() ? (artilleryLevel >= 4 ? 130.0 : 90.0)
                : itemId == ShopItem.ROCKET.getId() ? 260.0 : 220.0;
        String id = bulletIds.next("special");
        Bullet shot = new Bullet(id, player.getId(), new Position(player.getPosition().getX(), player.getPosition().getY()),
                direction, speed, power, 8, 8, itemId);
        double range = itemId == ShopItem.ARTILLERY.getId()
                ? artilleryDistance(artilleryLevel) * world.getCurrentMap().getTileSize() : Double.POSITIVE_INFINITY;
        shot.setStartX(player.getPosition().getX());
        shot.setStartY(player.getPosition().getY());
        shot.setMaxRange(range);
        world.addBullet(shot);
        specialShots.put(id, new SpecialShot(id, player.getId(), itemId, range, radius, power));
        return true;
    }

    private boolean detonateGuidedRocket() {
        for (IBullet bullet : world.getAllBullets()) {
            SpecialShot shot = bullet instanceof Bullet ? specialShots.get(((Bullet) bullet).getId()) : null;
            if (shot != null && shot.type == ShopItem.ROCKET.getId()) {
                explodeSpecial((Bullet) bullet, shot, null);
                return true;
            }
        }
        return false;
    }

    private static double directionFor(Direction direction) {
        if (direction == Direction.RIGHT) return 90;
        if (direction == Direction.DOWN) return 180;
        if (direction == Direction.LEFT) return 270;
        return 0;
    }

    private static double directionForRotation(double rotation) {
        return (Math.floor((((rotation % 360) + 360) % 360 + 45) / 90) * 90) % 360;
    }

    private static int artilleryDistance(int level) { return level >= 3 ? 5 : 4; }
    private static int artilleryRadius(int level) { return level >= 5 ? 4 : 3; }
    private static int artilleryPower(int level) {
        if (level >= 5) return 17;
        if (level >= 4) return 14;
        if (level >= 2) return 11;
        return 9;
    }

    private void updateEntities(double dt) {
        updateTimedEffects(dt);
        // (a) cooldown + movement player
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof PlayerTank && shopState != null) {
                // Research can be upgraded after a field was installed; apply
                // its new recharge rate without requiring a respawn.
                ((Tank) tank).setShieldResearchLevel(shopState.getResearchLevel("powerFieldResearch"));
            }
            if (tank instanceof Tank) ((Tank) tank).updateShield(dt);
            if (tank instanceof PlayerTank && tank.isAlive()) {
                PlayerTank player = (PlayerTank) tank;
                player.updateCooldown(dt);
                player.updateMovement(dt, world.getCurrentMap());
            }
        }
        collectLocalPickup();
        // (b) AI
        int bulletsBefore = world.getBulletCount();
        if (freezeRemaining > 0) freezeRemaining = Math.max(0, freezeRemaining - dt);
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof AITank && freezeRemaining <= 0) ((AITank) tank).update(world, dt);
        }
        if (eventListener != null && world.getBulletCount() > bulletsBefore) {
            eventListener.onShoot(false, 0);
        }
        // (c) bullets
        for (IBullet bullet : world.getAllBullets()) {
            // Builds freezes hostile ammunition together with tanks/turrets.
            boolean enemyBullet = bullet.getOwnerId() != null && world.getTank(bullet.getOwnerId()) instanceof AITank;
            if (bullet.isActive() && !(freezeRemaining > 0 && enemyBullet)) {
                bullet.update(dt);
                if (bullet instanceof Bullet) {
                    SpecialShot special = specialShots.get(((Bullet) bullet).getId());
                    if (special != null && special.type == ShopItem.ARTILLERY.getId()) {
                        special.remainingRange -= bullet.getSpeed() * dt;
                    }
                }
            }
        }
        // (d) cleanup bullets chết
        world.removeInactiveBullets();
    }

    private void updateTimedEffects(double dt) {
        if (speedBoostRemaining > 0) {
            speedBoostRemaining = Math.max(0, speedBoostRemaining - dt);
            if (speedBoostRemaining == 0) {
                PlayerTank player = localPlayer();
                if (player != null) {
                    player.setSpeed(basePlayerSpeed);
                    player.setShootCooldown(basePlayerShootCooldown);
                }
            }
        }
        for (Iterator<WorldEffect> it = effects.iterator(); it.hasNext();) {
            WorldEffect effect = it.next();
            effect.remaining -= dt;
            if (effect.remaining <= 0) it.remove();
        }
    }

    private void collectLocalPickup() {
        if (!(world.getCurrentMap() instanceof MapDefinition) || shopState == null) return;
        PlayerTank player = localPlayer();
        if (player == null) return;
        TileType pickup = ((MapDefinition) world.getCurrentMap()).collectPickupAt(
                player.getPosition().getX(), player.getPosition().getY());
        if (pickup == TileType.REPAIR_PICKUP) {
            if (eventListener != null) eventListener.onPickup();
            applyRepair(player);
        } else if (pickup == TileType.SPEED_PICKUP) {
            if (eventListener != null) eventListener.onPickup();
            applySpeedBoost(player);
        }
    }

    private void applyRepair(PlayerTank player) {
        player.setHp(player.getMaxHP());
        if (eventListener != null) eventListener.onUseRepair();
    }

    private void applySpeedBoost(PlayerTank player) {
        speedBoostRemaining = SPEED_BOOST_SECONDS
                + (shopState.hasExChassis() ? EX_CHASSIS_SPEED_BONUS_SECONDS : 0);
        player.setSpeed(basePlayerSpeed * SPEED_BOOST_MULTIPLIER);
        player.setShootCooldown(Math.max(0.10, basePlayerShootCooldown - NITRO_RELOAD_REDUCTION_SECONDS));
        if (eventListener != null) eventListener.onUseSpeed();
    }

    private void checkCollisions() {
        checkBulletCollisions();
        for (IBullet bullet : world.getAllBullets()) {
            if (!bullet.isActive()) continue;
            if (bullet instanceof Bullet) {
                SpecialShot special = specialShots.get(((Bullet) bullet).getId());
                if (special != null) {
                    resolveSpecialCollision((Bullet) bullet, special);
                    continue;
                }
            }
            // Walls hấp thụ trước
            Wall hitWall = Collision.findBulletWallCollision(bullet, world.getCurrentMap());
            if (hitWall != null || Collision.isBulletOutsideMap(bullet, world.getCurrentMap())) {
                bullet.deactivate();
                if (eventListener != null) eventListener.onHitWall();
                if (hitWall != null && world.getCurrentMap() instanceof MapDefinition) {
                    TileType destroyed = ((MapDefinition) world.getCurrentMap()).hitWall(hitWall);
                    double effectX = hitWall.getPosition().getX() + hitWall.getWidth() / 2.0;
                    double effectY = hitWall.getPosition().getY() + hitWall.getHeight() / 2.0;
                    if (destroyed != null) {
                        effects.add(WorldEffect.destroyed(effectX, effectY, destroyed == TileType.BUSH, weaponTypeOf(bullet)));
                    } else {
                        // Cho cả người chơi và AI cùng phản hồi va chạm nhìn thấy được.
                        // Hiệu ứng nhỏ chỉ báo "đã trúng", không có nghĩa tường đã vỡ.
                        effects.add(WorldEffect.impact(effectX, effectY, weaponTypeOf(bullet)));
                    }
                }
                continue;
            }
            for (ITank tank : world.getAllTanks()) {
                // Đạn AI xuyên qua đồng đội AI; chỉ player mới là mục tiêu của AI.
                if (isAiFriendlyFire(bullet, tank)) continue;
                if (coopMode && isFriendlyFire(bullet, tank)) continue;
                if (Collision.checkBulletTankCollision(bullet, tank)) {
                    tank.takeDamage(bullet.getDamage());
                    bullet.deactivate();
                    effects.add(WorldEffect.impact(tank.getPosition().getX(), tank.getPosition().getY(), weaponTypeOf(bullet)));
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

    private void resolveSpecialCollision(Bullet bullet, SpecialShot special) {
        if (special.type == ShopItem.ARTILLERY.getId()) {
            if (special.remainingRange <= 0 || Collision.isBulletOutsideMap(bullet, world.getCurrentMap())) {
                explodeSpecial(bullet, special, null);
            }
            // Artillery flies above all walls and tanks until its selected range.
            return;
        }
        Wall wall = Collision.findBulletWallCollision(bullet, world.getCurrentMap());
        if (wall != null || Collision.isBulletOutsideMap(bullet, world.getCurrentMap())) {
            explodeSpecial(bullet, special, wall);
        }
        // Warhead and guided missile fly over tanks/turrets; only their blast damages them.
    }

    private void explodeSpecial(Bullet bullet, SpecialShot special, Wall directWall) {
        if (!bullet.isActive()) return;
        bullet.deactivate();
        specialShots.remove(special.bulletId);
        double x = bullet.getPosition().getX();
        double y = bullet.getPosition().getY();
        int tileSize = world.getCurrentMap().getTileSize();
        int cx = (int) Math.floor(x / tileSize);
        int cy = (int) Math.floor(y / tileSize);
        IMap map = world.getCurrentMap();
        MapDefinition md = (map instanceof MapDefinition) ? (MapDefinition) map : null;

        if (special.type == ShopItem.ARTILLERY.getId()) {
            // Sóng nổ hình thoi lan toả theo từng ô lưới chuẩn Base Builds (game_Explosion trong kha.js)
            int radius = special.radius;
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int dist = Math.abs(dx) + Math.abs(dy);
                    if (dist > radius) continue;
                    int tx = cx + dx;
                    int ty = cy + dy;
                    if (map != null && (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight())) continue;
                    double tilePxX = (tx + 0.5) * tileSize;
                    double tilePxY = (ty + 0.5) * tileSize;

                    // Hiệu ứng nổ: rìa ngoài cùng nổ nhỏ (small.png), bên trong nổ lớn (big.png)
                    if (dist == radius) {
                        effects.add(new WorldEffect(tilePxX, tilePxY, false, false, 0.40, 0, 0));
                    } else {
                        effects.add(new WorldEffect(tilePxX, tilePxY, false, false, 0.65, 6, 0));
                    }

                    // Phá huỷ tường mềm và bụi cây cỏ tại ô (tx, ty)
                    if (md != null) {
                        Wall wall = findWallAt(md, tx, ty);
                        if (wall != null) {
                            if (wall.getTileType() == TileType.WALL) {
                                int wallDamage = Math.max(1, special.power - dist);
                                for (int h = 0; h < wallDamage; h++) {
                                    if (!md.getWalls().contains(wall) || md.hitWall(wall) == null) break;
                                }
                            } else if (wall.getTileType() == TileType.BUSH) {
                                md.hitWall(wall);
                            }
                        }
                    }
                }
            }
            // Hủy các đạn thông thường bay qua vùng nổ
            for (IBullet b : world.getAllBullets()) {
                if (!b.isActive() || b == bullet) continue;
                double bDist = (Math.abs(b.getPosition().getX() - x) + Math.abs(b.getPosition().getY() - y)) / tileSize;
                if (bDist <= radius) {
                    b.deactivate();
                }
            }
            if (eventListener != null) {
                eventListener.onSpecialExplosion(special.type, special.radius);
            }
        } else {
            effects.add(WorldEffect.specialExplosion(x, y, special.type, special.radius));
            if (eventListener != null) {
                eventListener.onSpecialExplosion(special.type, special.radius);
            }
        }

        for (ITank tank : world.getAllTanks()) {
            if (!tank.isAlive() || tank.getId().equals(special.ownerId)) continue;
            double cells = Math.hypot(tank.getPosition().getX() - x, tank.getPosition().getY() - y) / tileSize;
            int damage = special.power - (int) Math.ceil(cells);
            if (damage <= 0 || cells > special.radius) continue;
            tank.takeDamage(damage);
            boolean player = tank instanceof PlayerTank;
            boolean killed = !tank.isAlive();
            if (eventListener != null) {
                if (killed) eventListener.onTankDestroyed(player); else eventListener.onHitTank(player);
            }
            if (killed && tank instanceof AITank) {
                world.addScore(100);
                aiKilledThisMatch++;
            }
        }
        // In the current map schema WALL is destructible concrete; WHITE_WALL is armored
        // and deliberately remains indestructible, matching the reference restriction.
        if (special.type == ShopItem.ROCKET.getId() && directWall != null
                && directWall.getTileType() == TileType.WALL && world.getCurrentMap() instanceof MapDefinition) {
            MapDefinition mapDef = (MapDefinition) world.getCurrentMap();
            while (mapDef.getWalls().contains(directWall) && mapDef.hitWall(directWall) == null) {
                // Guided missile is the concrete-wall breaker: consume all wall HP at impact.
            }
        }
    }

    private Wall findWallAt(MapDefinition map, int col, int row) {
        if (map == null) return null;
        for (Wall w : map.getWalls()) {
            if (w.getCol() == col && w.getRow() == row) return w;
        }
        return null;
    }

    private void checkBulletCollisions() {
        List<IBullet> bullets = world.getAllBullets();
        for (int i = 0; i < bullets.size(); i++) {
            IBullet first = bullets.get(i);
            if (!first.isActive()) continue;
            for (int j = i + 1; j < bullets.size(); j++) {
                IBullet second = bullets.get(j);
                if (!second.isActive()) continue;
                if (sameBulletTeam(first, second)) continue;
                if (Collision.checkBulletBulletCollision(first, second)) {
                    resolveBulletImpact(first, second);
                    break;
                }
            }
        }
    }

    /**
     * Builds trừ damage hai chiều khi hai đạn gặp nhau. Vì Double có damage 2,
     * nó chỉ mất một nòng khi chặn Shell/Plasma (damage 1) rồi tiếp tục bay.
     */
    private void resolveBulletImpact(IBullet first, IBullet second) {
        if (!(first instanceof Bullet) || !(second instanceof Bullet)) {
            first.deactivate();
            second.deactivate();
            return;
        }
        Bullet a = (Bullet) first;
        Bullet b = (Bullet) second;
        int aDamage = a.getDamage();
        int bDamage = b.getDamage();
        a.setDamage(aDamage - bDamage);
        b.setDamage(bDamage - aDamage);
        if (a.getDamage() <= 0) a.deactivate();
        if (b.getDamage() <= 0) b.deactivate();
        effects.add(WorldEffect.impact(
                (a.getPosition().getX() + b.getPosition().getX()) / 2.0,
                (a.getPosition().getY() + b.getPosition().getY()) / 2.0,
                weaponTypeOf(a)));
    }

    private boolean sameBulletTeam(IBullet first, IBullet second) {
        ITank firstOwner = world.getTank(first.getOwnerId());
        ITank secondOwner = world.getTank(second.getOwnerId());
        if (firstOwner == null || secondOwner == null) return false;
        return (firstOwner instanceof AITank && secondOwner instanceof AITank)
                || (firstOwner instanceof PlayerTank && secondOwner instanceof PlayerTank);
    }

    private static int weaponTypeOf(IBullet bullet) {
        return bullet instanceof Bullet ? ((Bullet) bullet).getWeaponType() : 0;
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
        if (isCaptureFlagMode()) {
            respawnCaptureFlagTanks();
            return;
        }
        PlayerTank player = localPlayer();
        if (player != null && !player.isAlive() && shopState != null) {
            // Builds removes one temporary plate for every lost life, regardless
            // of whether another Extra Life is available. Permanent plates stay.
            if (shopState.consumeItem(ShopItem.ARMOR.getId())) {
                player.removeHullHitPoints(1);
                shopStateDirty = true;
            }
            // Field plates are installed equipment, not inventory. Builds drops
            // the entire field with the destroyed tank.
            if (shopState.getInstalledForceField() > 0) {
                shopState.clearInstalledForceField();
                player.clearShield();
                shopStateDirty = true;
            }
            if (shopState.consumeItem(ShopItem.ADD_LIFE.getId())) {
                shopStateDirty = true;
                RespawnPoint spawn = respawnPoints.get(player.getId());
                if (spawn != null) player.revive(spawn.x, spawn.y);
            }
        }
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

    private boolean isCaptureFlagMode() {
        if (coopMode || world.getCurrentMap() == null) return false;
        IMap map = world.getCurrentMap();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int col = 0; col < map.getWidth(); col++) {
                if (map.getTileAt(col, row) == TileType.FLAG) return true;
            }
        }
        return false;
    }

    private boolean checkCaptureFlagGoal() {
        if (!isCaptureFlagMode()) return false;
        PlayerTank player = localPlayer();
        IMap map = world.getCurrentMap();
        if (player == null || !player.isAlive() || map == null) return false;
        int col = (int) Math.floor(player.getPosition().getX() / map.getTileSize());
        int row = (int) Math.floor(player.getPosition().getY() / map.getTileSize());
        if (map.getTileAt(col, row) != TileType.FLAG) return false;
        world.setStatus(GameWorld.Status.VICTORY);
        if (eventListener != null) eventListener.onPickup();
        fireMatchEnd(GameWorld.Status.VICTORY, player.getHP());
        return true;
    }

    private void respawnCaptureFlagTanks() {
        List<String> deadIds = new ArrayList<>();
        for (ITank tank : world.getAllTanks()) {
            if (!tank.isAlive()) deadIds.add(tank.getId());
        }
        for (String id : deadIds) {
            RespawnPoint spawn = respawnPoints.get(id);
            world.removeTank(id);
            if (spawn == null) continue;
            if (spawn.player) {
                // Capture Flag creates a fresh player instance on respawn. Consume
                // temporary armor before doing so, otherwise spawnPlayerAt() would
                // read the unchanged inventory and grant the same plate again.
                if (shopState != null && shopState.consumeItem(ShopItem.ARMOR.getId())) {
                    shopStateDirty = true;
                }
                if (shopState != null && shopState.getInstalledForceField() > 0) {
                    shopState.clearInstalledForceField();
                    shopStateDirty = true;
                }
                spawnPlayerAt(id, new Position(spawn.x, spawn.y));
            } else {
                spawnEnemyAt(id, spawn.x, spawn.y, spawn.difficulty);
            }
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

    private PlayerTank localPlayer() {
        ITank tank = world.getTank(localPlayerId);
        return tank instanceof PlayerTank ? (PlayerTank) tank : null;
    }

    /** Dùng vật phẩm hỗ trợ đang có; chỉ trừ kho khi hiệu ứng thực sự áp dụng. */
    public boolean useInventoryItem(int itemId) {
        if (!world.isPlaying() || shopState == null) return false;
        PlayerTank player = localPlayer();
        if (player == null || !player.isAlive()) return false;
        if (itemId == ShopItem.REPAIR.getId()) {
            if (player.getHP() >= player.getMaxHP() || !shopState.consumeItem(itemId)) return false;
            applyRepair(player);
            shopStateDirty = true;
            return true;
        }
        if (itemId == ShopItem.SPEED_UP.getId()) {
            if (!shopState.consumeItem(itemId)) return false;
            applySpeedBoost(player);
            shopStateDirty = true;
            return true;
        }
        if (itemId == ShopItem.FREEZE.getId()) {
            if (!shopState.consumeItem(itemId)) return false;
            freezeRemaining = FREEZE_SECONDS
                    + (shopState.hasExChassis() ? EX_CHASSIS_FREEZE_BONUS_SECONDS : 0);
            shopStateDirty = true;
            if (eventListener != null) eventListener.onUseFreeze();
            return true;
        }
        if (itemId == ShopItem.FORCE_FIELD.getId()) {
            int installed = 0;
            int limit = shopState.getEffectiveLimit(ShopItem.FORCE_FIELD);
            while (shopState.getItemCount(itemId) > 0 && player.getMaxShieldHp() < limit) {
                shopState.consumeItem(itemId);
                player.addShieldCapacity(1);
                player.setShieldResearchLevel(shopState.getResearchLevel("powerFieldResearch"));
                shopState.addInstalledForceField(1);
                installed++;
            }
            if (installed > 0) shopStateDirty = true;
            return installed > 0;
        }
        if (itemId == ShopItem.RADAR.getId()) {
            if (shopState.getItemCount(itemId) <= 0) return false;
            radarActive = true;
            return true;
        }
        return false;
    }

    public double getSpeedBoostRemaining() { return speedBoostRemaining; }
    public double getFreezeRemaining() { return freezeRemaining; }
    public boolean isRadarActive() { return radarActive; }

    /** Applies a successful Workshop purchase to the currently deployed tank. */
    public void applyWorkshopPurchase(ShopItem item) {
        if (item == null) return;
        PlayerTank player = localPlayer();
        if (player == null || !player.isAlive()) return;
        if (item == ShopItem.ARMOR || item == ShopItem.ARMOR2) {
            player.addHullHitPoints(1);
        } else if (item == ShopItem.EX_CHASSIS) {
            player.addHullHitPoints(2);
        }
    }

    /** Mirrors a Workshop sale immediately so the deployed tank never keeps sold armor. */
    public void applyWorkshopSale(ShopItem item) {
        if (item != ShopItem.ARMOR && item != ShopItem.ARMOR2) return;
        PlayerTank player = localPlayer();
        if (player != null && player.isAlive()) player.removeHullHitPoints(1);
    }

    public int getLocalPlayerMissingHp() {
        PlayerTank player = localPlayer();
        return player == null ? 0 : Math.max(0, player.getMaxHP() - player.getHP());
    }

    /** Workshop repair: 6C/HP, or 3C/HP with armor research level 3. */
    public boolean repairLocalPlayerAtWorkshop() {
        PlayerTank player = localPlayer();
        if (player == null || shopState == null) return false;
        int missing = getLocalPlayerMissingHp();
        if (missing <= 0 || !shopState.repairTank(missing)) return false;
        player.restoreHull();
        return true;
    }

    public boolean consumeShopStateDirty() {
        boolean dirty = shopStateDirty;
        shopStateDirty = false;
        return dirty;
    }

    public void appendEffectsTo(GameStateSnapshot snap) {
        if (snap == null) return;
        for (WorldEffect effect : effects) {
            EffectSnapshot out = new EffectSnapshot();
            out.x = (float) effect.x;
            out.y = (float) effect.y;
            out.progress = (float) (1.0 - effect.remaining / effect.duration);
            out.bush = effect.bush;
            out.impact = effect.impact;
            out.weaponType = effect.weaponType;
            out.blastRadius = effect.blastRadius;
            snap.effects.add(out);
        }
    }

    // ========== INNER ==========
    private static final class Command {
        enum Type { MOVE, STOP, STOP_DIR, REVERSE, SHOOT }
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

    private static final class WorldEffect {
        final double x;
        final double y;
        final boolean bush;
        final boolean impact;
        final int weaponType;
        final int blastRadius;
        final double duration;
        double remaining;
        private WorldEffect(double x, double y, boolean bush, boolean impact, double duration, int weaponType) {
            this(x, y, bush, impact, duration, weaponType, 0);
        }
        private WorldEffect(double x, double y, boolean bush, boolean impact, double duration, int weaponType, int blastRadius) {
            this.x = x;
            this.y = y;
            this.bush = bush;
            this.impact = impact;
            this.weaponType = weaponType;
            this.blastRadius = blastRadius;
            this.duration = duration;
            this.remaining = duration;
        }
        static WorldEffect destroyed(double x, double y, boolean bush, int weaponType) {
            return new WorldEffect(x, y, bush, false, 0.70, weaponType);
        }
        static WorldEffect impact(double x, double y, int weaponType) {
            return new WorldEffect(x, y, false, true, 0.22, weaponType);
        }
        static WorldEffect specialExplosion(double x, double y, int weaponType, int radius) {
            return new WorldEffect(x, y, false, false, 0.70, weaponType, radius);
        }
    }

    private static final class SpecialShot {
        final String bulletId;
        final String ownerId;
        final int type;
        double remainingRange;
        final int radius;
        final int power;
        SpecialShot(String bulletId, String ownerId, int type, double remainingRange, int radius, int power) {
            this.bulletId = bulletId;
            this.ownerId = ownerId;
            this.type = type;
            this.remainingRange = remainingRange;
            this.radius = radius;
            this.power = power;
        }
    }

    /** Callback khi trận kết thúc (VICTORY/DEFEAT) — client gọi ScoreCalculator + gửi REST. */
    public interface MatchEndHandler {
        void onMatchEnd(String levelId, String status, int hpRemain, int aiKills, int elapsedSeconds);
    }
}
