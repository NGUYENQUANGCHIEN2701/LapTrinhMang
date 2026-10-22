package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.ai.DifficultyLevel;
import com.tankzors.client.gameplay.ai.ShootBehavior;
import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.Building;
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
import com.tankzors.client.gameplay.map.MissionConfig;
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
    private double basePlayerShootCooldown = 0.75;
    private double speedBoostRemaining;
    private double freezeRemaining;
    private final RadarState radar = new RadarState();
    private boolean shopStateDirty;
    private final List<WorldEffect> effects = new ArrayList<>();
    private final Map<String, RespawnPoint> respawnPoints = new HashMap<>();
    /** Runtime state for the three special rounds; snapshots still use Bullet. */
    private final Map<String, SpecialShot> specialShots = new HashMap<>();

    // Kịch bản màn Builds (mission): đợt địch, đồng minh, trigger, thuốc nổ.
    private MissionConfig mission;
    private MissionSpawner enemySpawner;
    private MissionSpawner allySpawner;
    private final List<TriggerState> triggerStates = new ArrayList<>();
    private final List<Dynamite> dynamites = new ArrayList<>();
    private final List<PlacedMine> mines = new ArrayList<>();
    private final List<AirRaid> airRaids = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();
    private boolean airAimActive;
    private int airAimCol, airAimRow;
    private Direction airAimDirection;
    private double airAimMoveDelay;
    private int lastTriggerCol = Integer.MIN_VALUE;
    private int lastTriggerRow = Integer.MIN_VALUE;
    private String hintText;
    private double hintRemaining;
    private int missionSerial;

    /** Builds: useFreezeBonus(1800) ở 60 Hz khi nhặt vật phẩm đóng băng. */
    public static final double FREEZE_PICKUP_SECONDS = 30.0;
    /** Builds: xu trên map cộng 35 xu. */
    public static final int COIN_PICKUP_VALUE = 35;
    /** Builds: thuốc nổ người chơi đặt cháy ngòi 180 frame. */
    public static final double DYNAMITE_FUSE_SECONDS = 3.0;
    /** Builds: thùng bẫy nổ sau 120 frame. */
    public static final double TRAP_FUSE_SECONDS = 2.0;
    /** Builds: gợi ý văn bản mặc định 120 frame. */
    public static final double HINT_SECONDS = 2.0;

    public static final double SPEED_BOOST_MULTIPLIER = 1.30;
    /** 2700 ticks at the reference build's 60 Hz rate. */
    public static final double SPEED_BOOST_SECONDS = 45.0;
    public static final double EX_CHASSIS_SPEED_BONUS_SECONDS = 15.0;
    public static final double FREEZE_SECONDS = 25.0;
    public static final double EX_CHASSIS_FREEZE_BONUS_SECONDS = 5.0;
    private static final double NITRO_RELOAD_REDUCTION_SECONDS = 10.0 / 60.0;

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
        setupMission();
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
        setupMission();
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
            setupMission();
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
        radar.reset();
        shopStateDirty = false;
        effects.clear();
        specialShots.clear();
        respawnPoints.clear();
        mission = null;
        enemySpawner = null;
        allySpawner = null;
        triggerStates.clear();
        dynamites.clear();
        mines.clear();
        airRaids.clear();
        blasts.clear();
        airAimActive = false;
        airAimDirection = null;
        lastTriggerCol = Integer.MIN_VALUE;
        lastTriggerRow = Integer.MIN_VALUE;
        hintText = null;
        hintRemaining = 0;
        missionSerial = 0;
    }

    // ========== MISSION (Builds campaign script) ==========
    private void setupMission() {
        IMap map = world.getCurrentMap();
        mission = map instanceof MapDefinition ? ((MapDefinition) map).getMission() : null;
        if (mission == null) return;
        // Builds: mỗi base chờ ngẫu nhiên 0..SPAWN_TIMEOUT trước lần sinh đầu.
        if (mission.getEnemyWaves() != null) {
            enemySpawner = new MissionSpawner(mission.getEnemyWaves(),
                    matchRng.nextDouble() * mission.getEnemyWaves().intervalSeconds);
        }
        if (mission.getAllies() != null) {
            allySpawner = new MissionSpawner(mission.getAllies(),
                    matchRng.nextDouble() * mission.getAllies().intervalSeconds);
        }
        int tileSize = map.getTileSize();
        for (MissionConfig.BuildingConfig cfg : mission.getBuildings()) {
            world.getAllBuildings().add(new Building("building-" + cfg.x + "-" + cfg.y, cfg.type, cfg.x, cfg.y, cfg.team, cfg.hitPoints));
        }
        for (MissionConfig.TurretConfig cfg : mission.getTurrets()) {
            AITank turret = AITank.turret("turret-" + cfg.x + "-" + cfg.y,
                    (cfg.x + 0.5) * tileSize, (cfg.y + 0.5) * tileSize, cfg.direction, cfg.range, cfg.hitPoints);
            wireAiBulletIds(turret);
            world.addTank(turret);
        }
        for (MissionConfig.TriggerConfig cfg : mission.getTriggers()) {
            triggerStates.add(new TriggerState(cfg));
        }
    }

    private void updateMission(double dt) {
        if (hintRemaining > 0) {
            hintRemaining = Math.max(0, hintRemaining - dt);
            if (hintRemaining == 0) hintText = null;
        }
        updateDynamites(dt);
        if (mission == null) return;
        checkTriggers();
        if (enemySpawner != null) {
            MissionConfig.SpawnBase base = enemySpawner.update(dt, countOnField(false), this::isCellFree);
            if (base != null) spawnWaveTank(base, enemySpawner, false);
        }
        if (allySpawner != null) {
            MissionConfig.SpawnBase base = allySpawner.update(dt, countOnField(true), this::isCellFree);
            if (base != null) spawnWaveTank(base, allySpawner, true);
        }
    }

    private void spawnWaveTank(MissionConfig.SpawnBase base, MissionSpawner spawner, boolean ally) {
        int tileSize = world.getCurrentMap().getTileSize();
        MissionConfig.Waves waves = spawner.waves();
        String id = (ally ? "ally-" : "ai-wave-") + (missionSerial++);
        AITank tank = AITank.builds(id, (base.x + 0.5) * tileSize, (base.y + 0.5) * tileSize,
                spawner.takeTankType(matchRng), waves.level);
        if (ally) {
            tank.setTeam(AITank.Team.ALLY);
            tank.setFollowPlayer(waves.followPlayer);
        }
        wireAiBulletIds(tank);
        world.addTank(tank);
    }

    /** Xe AI đang sống của một phe (không tính ụ súng). */
    private int countOnField(boolean ally) {
        int count = 0;
        for (ITank t : world.getAllTanks()) {
            if (t instanceof AITank && t.isAlive() && !((AITank) t).isTurret()
                    && ((AITank) t).isAlly() == ally) count++;
        }
        return count;
    }

    /** Builds teamsGrid: base chỉ sinh khi không có xe nào chiếm ô. */
    private boolean isCellFree(int col, int row) {
        int tileSize = world.getCurrentMap().getTileSize();
        com.tankzors.client.gameplay.entity.Rectangle cell =
                new com.tankzors.client.gameplay.entity.Rectangle(col * tileSize, row * tileSize, tileSize, tileSize);
        for (ITank t : world.getAllTanks()) {
            if (t.isAlive() && t instanceof Tank && ((Tank) t).getBoundingBox().intersects(cell)) return false;
        }
        return true;
    }

    /** Không cho xe người chơi đi xuyên xe khác (AI, ụ súng hoặc người chơi co-op). */
    private boolean isTileOccupiedByOtherTank(PlayerTank mover, int col, int row) {
        int tileSize = world.getCurrentMap().getTileSize();
        com.tankzors.client.gameplay.entity.Rectangle cell = new com.tankzors.client.gameplay.entity.Rectangle(
                col * tileSize + 1, row * tileSize + 1, tileSize - 2, tileSize - 2);
        for (ITank t : world.getAllTanks()) {
            if (t == mover || !t.isAlive()) continue;
            if (t instanceof AITank && !((AITank) t).isTurret()) {
                if (((AITank) t).getController().occupies(col, row)) return true;
            } else if (t instanceof PlayerTank) {
                if (((PlayerTank) t).occupies(col, row, tileSize)) return true;
            } else if (t instanceof Tank && ((Tank) t).getBoundingBox().intersects(cell)) {
                return true;
            }
        }
        return false;
    }

    /** Số xe địch còn phải hạ (đang sống + còn chờ sinh), không tính ụ súng. */
    public int getEnemyTanksRemaining() {
        int pending = enemySpawner != null ? enemySpawner.pending() : 0;
        return pending + countOnField(false);
    }

    public int getEnemyTanksTotal() {
        return mission != null && mission.getEnemyWaves() != null ? mission.getEnemyWaves().total() : 0;
    }

    public MissionConfig getMission() { return mission; }
    public String getHintText() { return hintText; }
    public double getHintRemaining() { return hintRemaining; }

    private void showHint(String text, double seconds) {
        if (text == null || text.isEmpty()) return;
        hintText = text;
        hintRemaining = Math.max(0.5, seconds);
    }

    private void checkTriggers() {
        PlayerTank player = localPlayer();
        IMap map = world.getCurrentMap();
        if (player == null || !player.isAlive() || map == null || triggerStates.isEmpty()) return;
        int col = (int) Math.floor(player.getPosition().getX() / map.getTileSize());
        int row = (int) Math.floor(player.getPosition().getY() / map.getTileSize());
        if (col == lastTriggerCol && row == lastTriggerRow) return;
        lastTriggerCol = col;
        lastTriggerRow = row;
        for (int i = 0; i < triggerStates.size(); i++) {
            MissionConfig.TriggerConfig cfg = triggerStates.get(i).config;
            if (cfg.x == col && cfg.y == row) pressTrigger(i, 0);
        }
    }

    /** Builds Trigger: chỉ phản hồi khi đang bật, không có cờ reusable nên chỉ một lần. */
    private void pressTrigger(int index, int depth) {
        if (index < 0 || index >= triggerStates.size() || depth > 8) return;
        TriggerState state = triggerStates.get(index);
        if (!state.active || state.fired) return;
        state.fired = true;
        MissionConfig.TriggerConfig cfg = state.config;
        switch (cfg.action) {
            case TEXT:
                showHint(cfg.text, cfg.seconds);
                break;
            case PRESS:
                pressTrigger(cfg.target, depth + 1);
                break;
            case ENABLE:
                if (cfg.target >= 0 && cfg.target < triggerStates.size()) triggerStates.get(cfg.target).active = true;
                break;
            default:
                break;
        }
    }

    public boolean isAirAimActive() { return airAimActive; }
    public void cancelAirAim() {
        airAimActive = false;
        airAimDirection = null;
        if (hintText != null && hintText.startsWith("AIR STRIKE:")) { hintText = null; hintRemaining = 0; }
    }

    private static boolean canPlaceOn(TileType tile) {
        return tile == TileType.GROUND || tile == TileType.SPAWN_PLAYER || tile == TileType.SPAWN_ENEMY;
    }

    private boolean mineAt(int col, int row) {
        for (PlacedMine mine : mines) if (mine.col == col && mine.row == row) return true;
        return false;
    }

    private boolean placeMine(PlayerTank player, int type) {
        IMap map = world.getCurrentMap();
        int col = cellOf(player.getPosition().getX(), map.getTileSize());
        int row = cellOf(player.getPosition().getY(), map.getTileSize());
        if (!canPlaceOn(map.getTileAt(col, row)) || mineAt(col, row)) return false;
        mines.add(new PlacedMine(col, row, type));
        if (eventListener != null) eventListener.onMinePlaced();
        return true;
    }

    private void confirmAirStrike() {
        if (shopState == null || !shopState.consumeItem(9)) { cancelAirAim(); return; }
        int level = shopState.getResearchLevel("airRaidResearch");
        airRaids.add(new AirRaid(airAimCol, airAimRow, level, world.getCurrentMap().getTileSize(), world.getCurrentMap().getHeight()));
        shopStateDirty = true;
        cancelAirAim();
    }

    private boolean hasGuidedRocket(String owner) {
        for (IBullet bullet : world.getAllBullets()) {
            SpecialShot special = bullet instanceof Bullet ? specialShots.get(((Bullet) bullet).getId()) : null;
            if (bullet.isActive() && special != null && special.type == 8 && special.ownerId.equals(owner)) return true;
        }
        return false;
    }

    private void updateSpecialFlight(Bullet bullet, double dt) {
        SpecialShot shot = specialShots.get(bullet.getId());
        if (shot == null || shot.type == 6) return;
        int size = world.getCurrentMap().getTileSize();
        if (shot.type == 8 && shot.nextDirection != bullet.getDirection()) {
            double axis = (bullet.getDirection() == 90 || bullet.getDirection() == 270)
                    ? bullet.getPosition().getX() : bullet.getPosition().getY();
            double offset = Math.abs(axis - (Math.floor(axis / size) + 0.5) * size);
            if (offset <= Math.max(size / 10.0, bullet.getSpeed() * dt)) {
                bullet.getPosition().setX((cellOf(bullet.getPosition().getX(), size) + 0.5) * size);
                bullet.getPosition().setY((cellOf(bullet.getPosition().getY(), size) + 0.5) * size);
                bullet.setDirection(shot.nextDirection);
                bullet.setSpeed(0);
            }
        }
        shot.smokeTime += dt;
        double smokeInterval = (10 - bullet.getSpeed() / (60 * size / 20.0) * 2) / 60.0;
        if (shot.smokeTime > smokeInterval) {
            effects.add(new WorldEffect(bullet.getPosition().getX(), bullet.getPosition().getY(), false, false, 1.5, -7));
            shot.smokeTime = 0;
        }
        // Builds starts stationary and adds .015 pixels/tick, capped at 4.
        double scale = size / 20.0;
        bullet.setSpeed(Math.min(4 * 60 * scale, bullet.getSpeed() + .015 * 60 * 60 * scale * dt));
    }

    private void updateSpecialItems(double dt) {
        IMap map = world.getCurrentMap();
        if (map == null) return;
        int size = map.getTileSize();
        if (airAimActive && airAimDirection != null) {
            airAimMoveDelay -= dt;
            if (airAimMoveDelay <= 0) {
                double angle = Math.toRadians(directionFor(airAimDirection));
                airAimCol = Math.max(0, Math.min(map.getWidth() - 1, airAimCol + (int) Math.round(Math.sin(angle))));
                airAimRow = Math.max(0, Math.min(map.getHeight() - 1, airAimRow - (int) Math.round(Math.cos(angle))));
                airAimMoveDelay = 8.0 / 60.0;
            }
        }
        for (Iterator<PlacedMine> it = mines.iterator(); it.hasNext();) {
            PlacedMine mine = it.next();
            for (ITank tank : world.getAllTanks()) {
                if (!tank.isAlive() || !isEnemySide(tank) || cellOf(tank.getPosition().getX(), size) != mine.col
                        || cellOf(tank.getPosition().getY(), size) != mine.row) continue;
                tank.takeDamage(mine.type == 10 ? 5 : 9);
                reportTankHit(tank);
                effects.add(WorldEffect.impact((mine.col + .5) * size, (mine.row + .5) * size, 0));
                it.remove();
                break;
            }
        }
        for (Iterator<AirRaid> it = airRaids.iterator(); it.hasNext();) {
            AirRaid raid = it.next();
            raid.x += size * 3 * dt;
            raid.y -= size * 3 * dt;
            int col = cellOf(raid.x, size);
            if (col != raid.previousCol && Math.abs(col - raid.targetCol) <= SpecialAmmoRules.airBombs(raid.level) / 2) {
                explodeArea(col, cellOf(raid.y, size), SpecialAmmoRules.airRadius(raid.level), SpecialAmmoRules.airPower(raid.level));
            }
            raid.previousCol = col;
            if (raid.y < -84 * size / 20.0) it.remove();
        }
        for (Iterator<Blast> it = blasts.iterator(); it.hasNext();) {
            Blast blast = it.next();
            blast.delay -= dt;
            while (blast.delay <= 0 && blast.ring < blast.radius) {
                applyBlastRing(blast);
                blast.ring++;
                blast.delay += 7.0 / 60.0;
            }
            if (blast.ring >= blast.radius) it.remove();
        }
        specialShots.entrySet().removeIf(e -> {
            for (IBullet bullet : world.getAllBullets()) {
                if (bullet instanceof Bullet && bullet.isActive() && ((Bullet) bullet).getId().equals(e.getKey())) return false;
            }
            return true;
        });
    }

    private static final class PlacedMine {
        final int col, row, type;
        PlacedMine(int col, int row, int type) { this.col = col; this.row = row; this.type = type; }
    }
    private static final class AirRaid {
        final int targetCol, level;
        int previousCol = Integer.MIN_VALUE;
        double x, y;
        AirRaid(int col, int row, int level, int size, int mapHeight) {
            this.targetCol = col; this.level = level;
            int min = Math.min(col, mapHeight - 1 - row);
            int spriteOffset = 5; // ceil(84px frame width / Builds 20px tile).
            x = (-(mapHeight - 1 - (row + min)) - spriteOffset - 5) * size;
            y = (mapHeight + col - min + spriteOffset + 5) * size - size / 20.0;
        }
    }
    private static final class Blast {
        final int col, row, radius, power;
        final java.util.Set<String> damaged = new java.util.HashSet<>();
        int ring;
        double delay;
        Blast(int col, int row, int radius, int power) { this.col = col; this.row = row; this.radius = radius; this.power = power; }
    }

    // ---- Dynamite (Builds game.effects.Dynamite + game.Explosion) ----
    private static boolean isDynamiteItem(int itemId) {
        return itemId == ShopItem.DYNAMITE.getId() || itemId == ShopItem.DYNAMITE2.getId()
                || itemId == ShopItem.DYNAMITE3.getId();
    }

    private boolean placeDynamite(PlayerTank player, int itemId) {
        IMap map = world.getCurrentMap();
        if (!(map instanceof MapDefinition)) return false;
        MapDefinition md = (MapDefinition) map;
        int col = (int) Math.floor(player.getPosition().getX() / md.getTileSize());
        int row = (int) Math.floor(player.getPosition().getY() / md.getTileSize());
        // Builds chỉ cho đặt lên ô không có vật gì ở lớp trên.
        if (!canPlaceOn(md.getTileAt(col, row)) || mineAt(col, row)) return false;
        int type = itemId == ShopItem.DYNAMITE3.getId() ? 2 : itemId == ShopItem.DYNAMITE2.getId() ? 1 : 0;
        md.setTileAt(col, row, TileType.DYNAMITE);
        dynamites.add(new Dynamite(col, row, type, DYNAMITE_FUSE_SECONDS));
        if (eventListener != null) eventListener.onMinePlaced();
        return true;
    }

    private void updateDynamites(double dt) {
        if (dynamites.isEmpty()) return;
        List<Dynamite> exploding = new ArrayList<>();
        for (Iterator<Dynamite> it = dynamites.iterator(); it.hasNext();) {
            Dynamite d = it.next();
            d.remaining -= dt;
            if (d.remaining <= 0) {
                it.remove();
                exploding.add(d);
            }
        }
        for (Dynamite d : exploding) {
            IMap map = world.getCurrentMap();
            if (map instanceof MapDefinition && map.getTileAt(d.col, d.row) == TileType.DYNAMITE) {
                ((MapDefinition) map).setTileAt(d.col, d.row, TileType.GROUND);
            }
            int radius = d.type == 2 ? 4 : 3;
            int power = d.type == 2 ? 19 : d.type == 1 ? 10 : 7;
            explodeArea(d.col, d.row, radius, power);
        }
    }

    /**
     * Builds game.Explosion: lan hình thoi tới radius-1 ô, sức nổ giảm 1 mỗi ô.
     * Phá gạch/bụi theo sức nổ, phá khối đá bất kể sức nổ, xoá vật phẩm, huỷ đạn,
     * và gây sát thương gấp đôi (isExpl) cho mọi xe trong vùng, kể cả người chơi.
     */
    private void explodeArea(int cx, int cy, int radius, int power) {
        blasts.add(new Blast(cx, cy, radius, power));
        if (eventListener != null) eventListener.onSpecialExplosion(ShopItem.DYNAMITE.getId(), radius);
    }

    private void applyBlastRing(Blast blast) {
        int cx = blast.col, cy = blast.row, radius = blast.radius, power = blast.power;
        IMap map = world.getCurrentMap();
        if (map == null) return;
        MapDefinition md = map instanceof MapDefinition ? (MapDefinition) map : null;
        int tileSize = map.getTileSize();
        int reach = radius - 1;
        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int dist = Math.abs(dx) + Math.abs(dy);
                if (dist != blast.ring) continue;
                int tx = cx + dx;
                int ty = cy + dy;
                if (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight()) continue;
                int cellPower = Math.max(0, power - dist);
                double px = (tx + 0.5) * tileSize;
                double py = (ty + 0.5) * tileSize;
                effects.add(dist == reach ? new WorldEffect(px, py, false, false, 0.40, 0, 0)
                        : new WorldEffect(px, py, false, false, 0.65, 6, 0));
                if (md != null) {
                    Wall wall = md.wallAt(tx, ty);
                    if (wall != null && !md.isBoundary(tx, ty)) {
                        if (wall.getTileType() == TileType.STONE_WALL) {
                            md.removeWall(wall);
                        } else if (cellPower > 0 && wall.getTileType() == TileType.BUSH) {
                            md.removeWall(wall);
                        } else if (cellPower > 0 && wall.getTileType() == TileType.WALL) {
                            for (int h = 0; h < cellPower && md.getWalls().contains(wall); h++) {
                                if (md.hitWall(wall) != null) break;
                            }
                        }
                    }
                    if (cellPower > 0) mines.removeIf(m -> m.col == tx && m.row == ty);
                    if (cellPower > 0 && md.getTileAt(tx, ty).isPickup()) md.setTileAt(tx, ty, TileType.GROUND);
                }
                if (cellPower == 0) continue;
                for (Building building : world.getAllBuildings()) {
                    if (building.contains(tx, ty) && blast.damaged.add(building.id)) damageBuilding(building, cellPower);
                }
                for (IBullet b : world.getAllBullets()) {
                    if (b.isActive() && cellOf(b.getPosition().getX(), tileSize) == tx
                            && cellOf(b.getPosition().getY(), tileSize) == ty) {
                        b.deactivate();
                    }
                }
                for (ITank tank : world.getAllTanks()) {
                    if (!tank.isAlive() || cellOf(tank.getPosition().getX(), tileSize) != tx
                            || cellOf(tank.getPosition().getY(), tileSize) != ty) continue;
                    if (!blast.damaged.add(tank.getId())) continue;
                    if (tank instanceof Tank) {
                        ((Tank) tank).takeExplosionDamage(cellPower, tank instanceof AITank && ((AITank) tank).isTurret());
                    } else tank.takeDamage(cellPower);
                    reportTankHit(tank);
                }
            }
        }
    }

    private static int cellOf(double pixel, int tileSize) {
        return (int) Math.floor(pixel / tileSize);
    }

    private void damageBuilding(Building building, int damage) {
        building.takeDamage(damage);
        if (!building.isAlive()) {
            int size = world.getCurrentMap().getTileSize();
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
                effects.add(new WorldEffect((building.col + x + .5) * size, (building.row + y + .5) * size, false, false, .65, 6));
            }
        }
    }

    private void reportTankHit(ITank tank) {
        boolean killed = !tank.isAlive();
        boolean isPlayer = tank instanceof PlayerTank;
        if (eventListener != null) {
            if (killed) eventListener.onTankDestroyed(isPlayer); else eventListener.onHitTank(isPlayer);
        }
        if (killed && isEnemySide(tank)) {
            world.addScore(100);
            aiKilledThisMatch++;
        }
    }

    /** Phe địch: AI không phải đồng minh. Người chơi và đồng minh cùng một phe. */
    private static boolean isEnemySide(ITank tank) {
        return tank instanceof AITank && !((AITank) tank).isAlly();
    }

    private boolean isEnemyOwner(String ownerId) {
        return Boolean.TRUE.equals(ownerSide(ownerId));
    }

    /**
     * Phe của chủ đạn: TRUE địch, FALSE phe người chơi, null khi không xác định
     * (chủ đã bị xoá và id không mang tiền tố do simulation cấp).
     */
    private Boolean ownerSide(String ownerId) {
        if (ownerId == null) return null;
        ITank owner = world.getTank(ownerId);
        if (owner != null) return isEnemySide(owner);
        if (ownerId.startsWith("ai-") || ownerId.startsWith("turret-")) return Boolean.TRUE;
        if (ownerId.startsWith("ally-")) return Boolean.FALSE;
        return null;
    }

    /** Đạn không gây sát thương cho xe cùng phe (đồng minh, AI với AI, player với player). */
    private boolean isSameSide(IBullet bullet, ITank tank) {
        Boolean side = ownerSide(bullet.getOwnerId());
        return side != null && side == isEnemySide(tank);
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
        player.setArmorHitPoints(shopState == null ? 0 : shopState.getArmorHpBonus());
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
        // Màn Builds có đợt địch: ô base chỉ là nơi sinh theo lượt, không đặt xe tĩnh.
        if (map instanceof MapDefinition && ((MapDefinition) map).getMission() != null
                && ((MapDefinition) map).getMission().getEnemyWaves() != null) {
            return;
        }
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
        // Seed RNG theo trận để AI rẽ ngẫu nhiên nhưng tái lập được; id đạn đếm theo owner.
        enemy.getController().seed(matchRng.nextLong(), owner -> bulletIds.next(owner));
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
        if (radar.isOpen()) { radar.update(rawDeltaTime); return; }
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

    /**
     * Tỉ lệ thời gian đã trôi qua giữa step trước và step kế (0..1). Renderer
     * dùng để nội suy vị trí, tránh hình giật khi FPS màn hình khác 30Hz.
     */
    public float getInterpolationAlpha() {
        double alpha = accumulator / GameplayConfig.STEP_SECONDS;
        return (float) Math.max(0, Math.min(1, alpha));
    }

    // ========== Canonical fixed simulation step ==========
    public void step(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) return;
        if (radar.isOpen()) { radar.update(dt); return; }
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof Tank) ((Tank) tank).capturePreviousPosition();
        }
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
            if (!world.isPlaying() || !player.isAlive()) return;
            if (airAimActive) {
                if (c.type == Command.Type.SHOOT) confirmAirStrike();
                else if (c.type == Command.Type.MOVE) {
                    airAimDirection = Boolean.FALSE.equals(c.moving) ? null : c.direction;
                    airAimMoveDelay = 0;
                } else if (c.type == Command.Type.STOP || c.type == Command.Type.STOP_DIR) airAimDirection = null;
                return;
            }
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
                            // A live guided missile keeps control even after its last inventory unit was used.
                            if (detonateGuidedRocket()) break;
                            int wid = shopState.getActiveWeaponId();
                            int count = shopState.getItemCount(wid);
                            if (count <= 0) {
                                wid = shopState.selectFirstAvailableWeapon();
                                count = shopState.getItemCount(wid);
                            }
                            if (count <= 0) break;
                            basePlayerShootCooldown = shopState.getShellReloadSeconds();
                            player.setShootCooldown(Math.max(0.10, basePlayerShootCooldown
                                    - (speedBoostRemaining > 0 ? NITRO_RELOAD_REDUCTION_SECONDS : 0)));
                            player.setActiveWeaponId(wid);
                            if (wid == ShopItem.AIR_SUPPORT.getId()) {
                                // Builds gates purchases, but owned ammunition remains usable.
                                player.stopMoving();
                                airAimActive = true;
                                airAimCol = cellOf(player.getPosition().getX(), world.getCurrentMap().getTileSize());
                                airAimRow = cellOf(player.getPosition().getY(), world.getCurrentMap().getTileSize());
                                airAimDirection = null;
                                showHint("AIR STRIKE: WASD/arrows aim, Space/Enter fire, Q cancel", 3600);
                                break;
                            }
                            if (wid == 10 || wid == 11) {
                                if (placeMine(player, wid)) {
                                    shopState.setItemCount(wid, count - 1);
                                    shopStateDirty = true;
                                }
                                break;
                            }
                            if (isDynamiteItem(wid)) {
                                if (placeDynamite(player, wid)) {
                                    shopState.setItemCount(wid, count - 1);
                                    shopStateDirty = true;
                                }
                                break;
                            }
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
        guided.nextDirection = directionFor(command.direction);
        return true;
    }

    private boolean fireSpecialShot(PlayerTank player, int itemId) {
        if (itemId != ShopItem.ROCKET.getId() && player.shoot() == null) return false;
        if (itemId == ShopItem.ROCKET.getId()) player.stopMoving();
        if (itemId == ShopItem.ARTILLERY.getId()) player.delayNextShot(67.0 / 60.0);
        int artilleryLevel = shopState.getResearchLevel("artilleryResearch");
        double direction = directionForRotation(player.getRotation());
        int power = itemId == ShopItem.ARTILLERY.getId() ? artilleryPower(artilleryLevel)
                : itemId == ShopItem.WARHEAD.getId() ? 9 : 32;
        int radius = itemId == ShopItem.ARTILLERY.getId() ? artilleryRadius(artilleryLevel)
                : itemId == ShopItem.WARHEAD.getId() ? 2 : 3;
        double speed = itemId == ShopItem.ARTILLERY.getId() ? SpecialAmmoRules.artillerySpeed(artilleryLevel, world.getCurrentMap().getTileSize()) : 0.0;
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
        specialShots.get(id).nextDirection = direction;
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

    private static int artilleryDistance(int level) { return SpecialAmmoRules.artilleryDistance(level); }
    private static int artilleryRadius(int level) { return SpecialAmmoRules.artilleryRadius(level); }
    private static int artilleryPower(int level) { return SpecialAmmoRules.artilleryPower(level); }

    private void updateEntities(double dt) {
        updateTimedEffects(dt);
        updateSpecialItems(dt);
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
                if (!airAimActive && !hasGuidedRocket(player.getId())) {
                    player.updateMovement(dt, world.getCurrentMap(), this::isTileOccupiedByOtherTank);
                }
            }
        }
        collectLocalPickup();
        updateMission(dt);
        // (b) AI — đóng băng chỉ tác dụng lên phe địch, đồng minh vẫn hoạt động.
        int bulletsBefore = world.getBulletCount();
        if (freezeRemaining > 0) freezeRemaining = Math.max(0, freezeRemaining - dt);
        for (ITank tank : new ArrayList<>(world.getAllTanks())) {
            if (tank instanceof AITank && (freezeRemaining <= 0 || ((AITank) tank).isAlly())) {
                ((AITank) tank).update(world, dt);
            }
        }
        if (eventListener != null && world.getBulletCount() > bulletsBefore) {
            eventListener.onShoot(false, 0);
        }
        // (c) bullets
        for (IBullet bullet : world.getAllBullets()) {
            // Builds freezes hostile ammunition together with tanks/turrets.
            boolean enemyBullet = isEnemyOwner(bullet.getOwnerId());
            if (bullet.isActive() && !(freezeRemaining > 0 && enemyBullet)) {
                if (bullet instanceof Bullet) updateSpecialFlight((Bullet) bullet, dt);
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
            // Builds: trên cát hiệu ứng tăng tốc kéo dài gấp đôi.
            PlayerTank boosted = localPlayer();
            boolean onSand = boosted != null && world.getCurrentMap() instanceof MapDefinition
                    && ((MapDefinition) world.getCurrentMap()).isSandAt(
                            boosted.getPosition().getX(), boosted.getPosition().getY());
            speedBoostRemaining = Math.max(0, speedBoostRemaining - (onSand ? dt / 2 : dt));
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
        MapDefinition md = (MapDefinition) world.getCurrentMap();
        double x = player.getPosition().getX();
        double y = player.getPosition().getY();
        TileType pickup = md.pickupAt(x, y);
        if (pickup == null) return;
        // Builds giữ bộ sửa trên map khi xe còn đầy máu.
        if (pickup == TileType.REPAIR_PICKUP && player.getHP() >= player.getMaxHP()) return;
        md.collectPickupAt(x, y);
        if (eventListener != null) eventListener.onPickup();
        switch (pickup) {
            case REPAIR_PICKUP:
                applyRepair(player);
                break;
            case SPEED_PICKUP:
                applySpeedBoost(player);
                break;
            case FREEZE_PICKUP:
                freezeRemaining = Math.max(freezeRemaining, FREEZE_PICKUP_SECONDS);
                if (eventListener != null) eventListener.onUseFreeze();
                break;
            case STAR_PICKUP:
                shopState.addStars(1);
                shopStateDirty = true;
                break;
            case COIN_PICKUP:
                shopState.addCoins(COIN_PICKUP_VALUE);
                shopStateDirty = true;
                break;
            case LIFE_PICKUP:
                shopState.setItemCount(ShopItem.ADD_LIFE.getId(),
                        shopState.getItemCount(ShopItem.ADD_LIFE.getId()) + 1);
                shopStateDirty = true;
                break;
            case BOX_PICKUP:
                openBox(md, cellOf(x, md.getTileSize()), cellOf(y, md.getTileSize()));
                break;
            default:
                break;
        }
    }

    /** Builds Player.getCellItem case 32/33: cộng vật phẩm vào kho và báo tên. */
    private void openBox(MapDefinition md, int col, int row) {
        MissionConfig.BoxConfig box = mission != null ? mission.boxAt(col, row) : null;
        if (box == null || box.itemId == -1) {
            showHint("Thung rong.", HINT_SECONDS);
            return;
        }
        if (box.itemId == -2) {
            showHint("Thung bay! Tranh xa!", HINT_SECONDS);
            md.setTileAt(col, row, TileType.DYNAMITE);
            dynamites.add(new Dynamite(col, row, 0, TRAP_FUSE_SECONDS));
            return;
        }
        shopState.setItemCount(box.itemId, shopState.getItemCount(box.itemId) + box.count);
        shopStateDirty = true;
        ShopItem item = ShopItem.findById(box.itemId);
        showHint((item != null ? item.getDisplayName() : "Vat pham") + " (" + box.count + ")", HINT_SECONDS);
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
                if (hitWall == null) bullet.deactivate();
                if (eventListener != null) eventListener.onHitWall();
                if (hitWall != null && world.getCurrentMap() instanceof MapDefinition) {
                    // Brick/bush durability is HP-based.  A projectile spends its
                    // current damage on the obstacle, so AP and Double rounds
                    // need fewer shots while a weakened projectile deals less.
                    int durability = hitWall.isDestructible() ? hitWall.getHitPoints() : Integer.MAX_VALUE;
                    int remainingDamage = Math.max(0, bullet.getDamage() - durability);
                    TileType destroyed = ((MapDefinition) world.getCurrentMap())
                            .hitWall(hitWall, Math.max(0, bullet.getDamage()));
                    if (destroyed != null && remainingDamage > 0 && bullet instanceof Bullet) {
                        ((Bullet) bullet).setDamage(remainingDamage);
                    } else bullet.deactivate();
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
            boolean hitBuilding = false;
            for (Building building : world.getAllBuildings()) {
                if (!building.isAlive() || (building.team > 0 && ((building.team == 2) == isEnemyOwner(bullet.getOwnerId())))) continue;
                if (bullet instanceof com.tankzors.client.gameplay.interfaces.ICollidable
                        && ((com.tankzors.client.gameplay.interfaces.ICollidable) bullet).getBoundingBox().intersects(building.bounds(world.getCurrentMap().getTileSize()))) {
                    damageBuilding(building, bullet.getDamage());
                    bullet.deactivate(); hitBuilding = true; break;
                }
            }
            if (hitBuilding) continue;
            for (ITank tank : world.getAllTanks()) {
                // Đạn xuyên qua xe cùng phe (AI với AI, người chơi với đồng minh/co-op).
                if (isSameSide(bullet, tank)) continue;
                if (Collision.checkBulletTankCollision(bullet, tank)) {
                    tank.takeDamage(bullet.getDamage());
                    // Builds damageTank → attackedSide: xe AI quay về phía phát đạn.
                    if (tank instanceof AITank) ((AITank) tank).getController().onAttacked(bullet.getDirection());
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
                        if (isEnemySide(tank)) aiKilledThisMatch++;
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
        if (bullet.isActive()) {
            for (Building building : world.getAllBuildings()) {
                if (building.isAlive() && bullet.getBoundingBox().intersects(building.bounds(world.getCurrentMap().getTileSize()))) {
                    explodeSpecial(bullet, special, null); break;
                }
            }
        }
        if (bullet.isActive() && special.type == ShopItem.WARHEAD.getId()) {
            for (ITank tank : world.getAllTanks()) {
                if (isSameSide(bullet, tank) || !tank.isAlive()) continue;
                if (Collision.checkBulletTankCollision(bullet, tank)) {
                    // Builds rounds the shared collision location to the struck tank cell.
                    bullet.getPosition().setX(tank.getPosition().getX());
                    bullet.getPosition().setY(tank.getPosition().getY());
                    explodeSpecial(bullet, special, null);
                    break;
                }
            }
        }
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
        if (directWall != null) {
            cx = cellOf(directWall.getPosition().getX(), tileSize);
            cy = cellOf(directWall.getPosition().getY(), tileSize);
        }
        IMap map = world.getCurrentMap();
        MapDefinition md = (map instanceof MapDefinition) ? (MapDefinition) map : null;

        explodeArea(cx, cy, special.radius, special.type == 7 ? bullet.getDamage() : special.power);
        // Guided rockets also clear their impact tile except indestructible walls.
        if (special.type == ShopItem.ROCKET.getId() && directWall != null && md != null
                && (directWall.getTileType() != TileType.WHITE_WALL
                    || md.legacyWallId(cx, cy) == 9 || md.legacyWallId(cx, cy) == 10)
                && !md.isBoundary(cx, cy)) {
            md.removeWall(directWall);
        }
    }

    private Wall findWallAt(MapDefinition map, int col, int row) {
        if (map == null) return null;
        for (Wall w : map.getWalls()) {
            if (w.getCol() == col && w.getRow() == row) return w;
        }
        return null;
    }

    private static boolean isAirborneProjectile(IBullet bullet) {
        return weaponTypeOf(bullet) == 6 || weaponTypeOf(bullet) == 8;
    }

    private void checkBulletCollisions() {
        List<IBullet> bullets = world.getAllBullets();
        for (int i = 0; i < bullets.size(); i++) {
            IBullet first = bullets.get(i);
            if (!first.isActive() || isAirborneProjectile(first)) continue;
            for (int j = i + 1; j < bullets.size(); j++) {
                IBullet second = bullets.get(j);
                if (!second.isActive() || isAirborneProjectile(second)) continue;
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
        Boolean a = ownerSide(first.getOwnerId());
        Boolean b = ownerSide(second.getOwnerId());
        return a != null && a.equals(b);
    }

    private static int weaponTypeOf(IBullet bullet) {
        return bullet instanceof Bullet ? ((Bullet) bullet).getWeaponType() : 0;
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
                player.setArmorHitPoints(player.getArmorHitPoints() - 1);
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
        boolean turretsCount = mission == null || mission.getObjective() == MissionConfig.Objective.DESTROY_ALL;
        for (ITank t : world.getAllTanks()) {
            if (!t.isAlive()) continue;
            if (t instanceof PlayerTank) hasPlayer = true;
            if (isEnemySide(t) && (turretsCount || !((AITank) t).isTurret())) hasAI = true;
        }
        // Địch còn chờ ở base cũng tính là chưa hạ xong.
        if (enemySpawner != null && enemySpawner.pending() > 0) hasAI = true;

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
        if (itemId >= ShopItem.ALLY_TANK.getId() && itemId <= ShopItem.ALLY_TANK3.getId()) {
            if (shopState.getItemCount(itemId) <= 0) return false;
            RespawnPoint spawn = respawnPoints.get(localPlayerId);
            IMap map = world.getCurrentMap();
            if (spawn == null || map == null) return false;
            int size = map.getTileSize();
            int col = cellOf(spawn.x, size), row = cellOf(spawn.y, size);
            if (!isCellFree(col, row)) {
                showHint("Can cu dang bi chiem cho. Hay di xe ra khoi can cu de goi dong minh.", 3000);
                return false;
            }
            AITank ally = AITank.support("ally-support-" + (missionSerial++), spawn.x, spawn.y, itemId);
            wireAiBulletIds(ally);
            world.addTank(ally);
            shopState.consumeItem(itemId);
            shopStateDirty = true;
            return true;
        }
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
            radar.open();
            commandQueue.clear();
            player.stopMoving();
            accumulator = 0;
            return true;
        }
        return false;
    }

    public double getSpeedBoostRemaining() { return speedBoostRemaining; }
    public double getFreezeRemaining() { return freezeRemaining; }
    public boolean isRadarActive() { return radar.isOpen(); }
    public boolean isRadarPaused() { return radar.isPaused(); }
    public void closeRadar() { radar.close(); commandQueue.clear(); accumulator = 0; }
    public void toggleRadarPause() { radar.togglePause(); }


    /** Applies a successful Workshop purchase to the currently deployed tank. */
    public void applyWorkshopPurchase(ShopItem item) {
        if (item == null) return;
        PlayerTank player = localPlayer();
        if (player == null || !player.isAlive()) return;
        if (item == ShopItem.ARMOR || item == ShopItem.ARMOR2) {
            player.addHullHitPoints(1);
            player.setArmorHitPoints(player.getArmorHitPoints() + 1);
        } else if (item == ShopItem.EX_CHASSIS) {
            player.addHullHitPoints(2);
        }
    }

    /** Mirrors a Workshop sale immediately so the deployed tank never keeps sold armor. */
    public void applyWorkshopSale(ShopItem item) {
        if (item != ShopItem.ARMOR && item != ShopItem.ARMOR2) return;
        PlayerTank player = localPlayer();
        if (player != null && player.isAlive()) {
            player.removeHullHitPoints(1);
            player.setArmorHitPoints(player.getArmorHitPoints() - 1);
        }
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

    public int getMissingAlliedHp() {
        int missing = 0;
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof AITank && ((AITank) tank).isAlly() && tank.isAlive()) {
                missing += Math.max(0, tank.getMaxHP() - tank.getHP());
            }
        }
        return missing;
    }

    public boolean repairAlliesAtWorkshop() {
        if (shopState == null) return false;
        int missing = getMissingAlliedHp();
        int cost = missing * (shopState.getResearchLevel("armorResearch") >= 3 ? 2 : 5);
        if (missing <= 0 || shopState.getCoins() < cost) return false;
        shopState.setCoins(shopState.getCoins() - cost);
        for (ITank tank : world.getAllTanks()) {
            if (tank instanceof AITank && ((AITank) tank).isAlly() && tank.isAlive()) ((AITank) tank).restoreHull();
        }
        shopStateDirty = true;
        return true;
    }

    public boolean consumeShopStateDirty() {
        boolean dirty = shopStateDirty;
        shopStateDirty = false;
        return dirty;
    }

    public void appendEffectsTo(GameStateSnapshot snap) {
        if (snap == null) return;
        snap.radarActive = radar.isOpen();
        snap.radarPaused = radar.isPaused();
        snap.radarShowObjects = radar.showsObjects();
        snap.hasRadar = shopState != null && shopState.hasRadar();
        snap.artilleryRange = shopState == null ? 4 : artilleryDistance(shopState.getResearchLevel("artilleryResearch"));
        snap.airAimActive = airAimActive;
        snap.airAimCol = airAimCol;
        snap.airAimRow = airAimRow;
        for (PlacedMine mine : mines) {
            EffectSnapshot out = new EffectSnapshot();
            out.x = (float) ((mine.col + .5) * world.getCurrentMap().getTileSize());
            out.y = (float) ((mine.row + .5) * world.getCurrentMap().getTileSize());
            out.weaponType = mine.type;
            snap.effects.add(out);
        }
        for (AirRaid raid : airRaids) {
            EffectSnapshot out = new EffectSnapshot();
            out.x = (float) raid.x; out.y = (float) raid.y; out.weaponType = 9;
            snap.effects.add(out);
        }
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
        double nextDirection;
        double smokeTime;
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

    private static final class TriggerState {
        final MissionConfig.TriggerConfig config;
        boolean active;
        boolean fired;
        TriggerState(MissionConfig.TriggerConfig config) {
            this.config = config;
            this.active = config.active;
        }
    }

    private static final class Dynamite {
        final int col;
        final int row;
        final int type;
        double remaining;
        Dynamite(int col, int row, int type, double remaining) {
            this.col = col;
            this.row = row;
            this.type = type;
            this.remaining = remaining;
        }
    }

    /** Callback khi trận kết thúc (VICTORY/DEFEAT) — client gọi ScoreCalculator + gửi REST. */
    public interface MatchEndHandler {
        void onMatchEnd(String levelId, String status, int hpRemain, int aiKills, int elapsedSeconds);
    }
}
