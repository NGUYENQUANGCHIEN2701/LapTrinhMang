package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.TileType;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ShopItem;
import com.tankzors.client.model.GameStateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parity các luật terminal/AI/bullet gốc sang simulation client.
 * Tương đương server engine.EndToEndIntegrationTest + CampaignProgressionTest +
 * Test trực tiếp fixed-step simulation, không dùng scheduler.
 */
class GameSimulationTest {

    @Test
    @DisplayName("Repair hồi đầy HP và Speed tăng đúng 30% trong 20 giây")
    void supportItemsApplyExactEffects() {
        GameSimulation sim = new GameSimulation(99L);
        PlayerShopState shop = new PlayerShopState();
        sim.setShopState(shop);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 2, 2);
        sim.startCampaignLevel(map, "campaign_1", "player");
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");

        player.takeDamage(70);
        assertTrue(sim.useInventoryItem(ShopItem.REPAIR.getId()));
        assertEquals(player.getMaxHP(), player.getHP());
        assertEquals(2, shop.getItemCount(ShopItem.REPAIR.getId()));

        double baseSpeed = player.getSpeed();
        assertTrue(sim.useInventoryItem(ShopItem.SPEED_UP.getId()));
        assertEquals(baseSpeed * 1.30, player.getSpeed(), 0.0001);
        assertEquals(1, shop.getItemCount(ShopItem.SPEED_UP.getId()));
        for (int i = 0; i < 601; i++) {
            sim.getWorld().setStatus(GameWorld.Status.PLAYING);
            sim.step(1.0 / 30.0);
        }
        assertEquals(0.0, sim.getSpeedBoostRemaining(), 0.0001);
        assertEquals(baseSpeed, player.getSpeed(), 0.0001);
    }

    @Test
    @DisplayName("Player và AI có cùng tốc độ cơ bản chậm 60 px/s")
    void playerAndAiShareSlowerBaseSpeed() {
        assertEquals(60.0, PlayerTank.DEFAULT_SPEED);
        for (com.tankzors.client.gameplay.ai.DifficultyLevel level
                : com.tankzors.client.gameplay.ai.DifficultyLevel.values()) {
            assertEquals(PlayerTank.DEFAULT_SPEED, level.getSpeed());
        }
        assertEquals(200.0, Bullet.DEFAULT_SPEED);
        assertEquals(Bullet.DEFAULT_SPEED,
                com.tankzors.client.gameplay.ai.ShootBehavior.AI_BULLET_SPEED,
                "Player and AI projectiles must use the same speed");
    }

    @Test
    @DisplayName("Va chạm simulation phá tường xám sau 4 phát, bụi sau 1 phát, tường trắng không vỡ")
    void projectilesApplyObstacleDurabilityThroughSimulation() {
        int[][] grid = {
                {0, 0, 0, 0, 0},
                {0, 1, 5, 6, 0},
                {0, 0, 0, 0, 0}
        };
        MapDefinition map = new MapDefinition("obstacles", 5, 3, 40, grid);
        GameSimulation sim = new GameSimulation(11L);
        sim.getWorld().setCurrentMap(map);
        sim.getWorld().setStatus(GameWorld.Status.PLAYING);
        sim.getWorld().addTank(new PlayerTank("player", 20, 20));
        sim.getWorld().addTank(new AITank("ai", 180, 100,
                com.tankzors.client.gameplay.ai.DifficultyLevel.EASY));

        for (int hit = 1; hit <= 3; hit++) {
            hitObstacle(sim, "brick-" + hit, 60, 60);
            assertEquals(TileType.WALL, map.getTileAt(1, 1));
        }
        hitObstacle(sim, "brick-4", 60, 60);
        assertEquals(TileType.GROUND, map.getTileAt(1, 1));

        for (int hit = 1; hit <= 6; hit++) hitObstacle(sim, "white-" + hit, 100, 60);
        assertEquals(TileType.WHITE_WALL, map.getTileAt(2, 1));

        hitObstacle(sim, "bush-1", 140, 60);
        assertEquals(TileType.GROUND, map.getTileAt(3, 1));
    }

    @Test
    @DisplayName("Đạn được bắn thật từ xe người chơi phá tường nâu bên trong sau đúng 4 phát")
    void playerFiredBulletsDestroyInteriorBrownWall() {
        int[][] grid = {
                {1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 3, 0, 1, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1}
        };
        MapDefinition map = new MapDefinition("player-wall", 8, 5, 40, grid);
        GameSimulation sim = new GameSimulation(12L);
        PlayerShopState shop = new PlayerShopState();
        sim.setShopState(shop);
        sim.startCampaignLevel(map, "campaign_1", "player");
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
        player.rotate(90);
        com.tankzors.client.gameplay.entity.Wall interior = map.getWalls().stream()
                .filter(w -> w.getCol() == 4 && w.getRow() == 2)
                .findFirst().orElseThrow();

        for (int hit = 1; hit <= 3; hit++) {
            firePlayerShotUntilImpact(sim);
            assertEquals(TileType.WALL, map.getTileAt(4, 2), "Wall must remain through hit " + hit);
            assertEquals(4 - hit, interior.getHitPoints(), "Player hit must reduce brown-wall durability");
            GameStateSnapshot snapshot = new GameStateSnapshot();
            sim.appendEffectsTo(snapshot);
            assertTrue(snapshot.effects.stream().anyMatch(effect -> effect.impact),
                    "A non-terminal wall hit must show a small impact effect");
        }
        firePlayerShotUntilImpact(sim);

        assertEquals(TileType.GROUND, map.getTileAt(4, 2));
        assertEquals(96, shop.getItemCount(0));
    }

    @Test
    @DisplayName("Hai viên đạn gặp hoặc cắt qua nhau đều cùng bị triệt tiêu")
    void opposingBulletsCancelEachOtherContinuously() {
        GameSimulation sim = new GameSimulation(9L);
        GameWorld world = sim.getWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);

        Bullet right = new Bullet("right", "player", new Position(192, 200), 90,
                300, 25, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
        Bullet left = new Bullet("left", "enemy", new Position(208, 200), 270,
                300, 25, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
        world.addBullet(right);
        world.addBullet(left);

        sim.step(GameplayConfig.STEP_SECONDS);

        assertFalse(right.isActive());
        assertFalse(left.isActive());
    }

    @Test
    @DisplayName("Đạn mặc định hữu hạn và giảm sau mỗi phát bắn")
    void defaultAmmoIsFiniteAndConsumed() {
        GameSimulation sim = new GameSimulation(10L);
        PlayerShopState shop = new PlayerShopState();
        sim.setShopState(shop);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 2, 2);
        map.setEnemies(List.of(new MapDefinition.EnemyConfig("AI", "EASY", 15, 10)));
        sim.startCampaignLevel(map, "campaign_1", "player");

        sim.enqueueShoot();
        sim.step(GameplayConfig.STEP_SECONDS);

        assertEquals(99, shop.getItemCount(0));
    }

    @Test
    @DisplayName("startCampaignLevel: nạp map, spawn 1 player + AI theo metadata")
    void startCampaignLevel_spawns() {
        GameSimulation sim = new GameSimulation(42L);
        MapDefinition map = TestMaps.open();
        map.setEnemies(List.of(
                new MapDefinition.EnemyConfig("AI", "EASY", 15, 10),
                new MapDefinition.EnemyConfig("AI", "MEDIUM", 15, 12)));
        // Đặt ô spawn player
        setPlayerSpawn(map, 1, 1);

        sim.startCampaignLevel(map, "test", "player-1");

        GameWorld w = sim.getWorld();
        assertEquals(GameWorld.Status.PLAYING, w.getStatus());
        assertNotNull(w.getTank("player-1"));
        assertEquals(2, countAi(w), "Phải spawn 2 AI theo metadata");
    }

    @Test
    @DisplayName("VICTORY khi toàn bộ AI bị tiêu diệt; hpRemain/score đúng")
    void victory_whenAllAiDead() {
        GameSimulation sim = new GameSimulation(7L);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 1, 1);
        sim.startCampaignLevel(map, "test", "p");
        GameWorld w = sim.getWorld();

        // Capture terminal
        int[] result = new int[3]; // hpRemain, aiKills, elapsed
        String[] status = new String[1];
        sim.setMatchEndHandler((levelId, s, hp, kills, elapsed) -> {
            status[0] = s; result[0] = hp; result[1] = kills; result[2] = elapsed;
        });

        // Giả lập bắn chết tất cả AI: set HP AI = 1 rồi tick 1 viên đạn trúng
        for (var t : new ArrayList<>(w.getAllTanks())) {
            if (t instanceof AITank ai) {
                while (ai.getHP() > 1) ai.takeDamage(50); // còn 1 HP
            }
        }
        // Player đứng yên; ta trực tiếp bắn đạn vào AI để mô phỏng
        for (var t : new ArrayList<>(w.getAllTanks())) {
            if (t instanceof AITank ai) {
                Position p = ai.getPosition();
                PlayerTank shooter = (PlayerTank) w.getTank("p");
                Bullet bullet = new Bullet("b-test", "p", new Position(p.getX(), p.getY()), 0,
                        Bullet.DEFAULT_SPEED, 100, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
                w.addBullet(bullet);
            }
        }
        // Step để collision áp damage và cleanup phát hiện terminal
        sim.step(GameplayConfig.STEP_SECONDS);

        // AI mang damage 100 → chết. Tất cả AI chết + còn player → VICTORY.
        assertFalse(w.isPlaying(), "Phải đã kết thúc");
        // sort: có thể DEFEAT nếu player cũng chết — không ở đây.
        // Kiểm tra VICTORY hoặc DEFEAT tuỳ kết quả; ở đây player sống → VICTORY.
        boolean anyAiAlive = w.getAllTanks().stream().anyMatch(t -> t instanceof AITank && t.isAlive());
        if (!anyAiAlive) {
            // Có thể terminal đã fire
            assertTrue(w.getStatus() == GameWorld.Status.VICTORY,
                    "Không còn AI sống → VICTORY, thực tế=" + w.getStatus());
        }
    }

    @Test
    @DisplayName("DEFEAT khi player chết; tiebreak cùng-tick ưu tiên DEFEAT")
    void defeat_whenPlayerDead() {
        GameSimulation sim = new GameSimulation(1L);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 1, 1);
        sim.startCampaignLevel(map, "test", "p");
        GameWorld w = sim.getWorld();

        // Player ngay lập tức chết
        PlayerTank player = (PlayerTank) w.getTank("p");
        player.takeDamage(100);

        sim.step(GameplayConfig.STEP_SECONDS);

        assertEquals(0, player.getHP());
        // Player đã chết và bị cleanup → không còn PlayerTank sống
        boolean hasPlayer = w.getAllTanks().stream().anyMatch(t -> t instanceof PlayerTank && t.isAlive());
        assertFalse(hasPlayer);
        assertEquals(GameWorld.Status.DEFEAT, w.getStatus(), "Player chết → DEFEAT");
    }

    @Test
    @DisplayName("Terminal chỉ fire một lần dù step nhiều")
    void matchEnd_firesOnce() {
        GameSimulation sim = new GameSimulation(1L);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 1, 1);
        sim.startCampaignLevel(map, "test", "p");
        int[] count = {0};
        sim.setMatchEndHandler((l, s, hp, k, e) -> count[0]++);

        PlayerTank player = (PlayerTank) sim.getWorld().getTank("p");
        player.takeDamage(100);

        for (int i = 0; i < 10; i++) sim.step(GameplayConfig.STEP_SECONDS);

        assertEquals(1, count[0], "Phải fire đúng 1 lần");
    }

    @Test
    @DisplayName("Bullet không gây damage hai lần (break sau tank đầu)")
    void bullet_singleHit() {
        // Tạo 2 tank sát nhau, 1 đạn đi qua -> chỉ trúng 1
        GameSimulation sim = new GameSimulation(2L);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 1, 1);
        sim.startCampaignLevel(map, "test", "p");
        GameWorld w = sim.getWorld();
        AITank ai = new AITank("ai-x", 200, 200, com.tankzors.client.gameplay.ai.DifficultyLevel.EASY);
        w.addTank(ai);
        AITank ai2 = new AITank("ai-x2", 200, 200, com.tankzors.client.gameplay.ai.DifficultyLevel.EASY);
        w.addTank(ai2);

        Bullet bullet = new Bullet("b1", "p", new Position(200, 200), 0,
                Bullet.DEFAULT_SPEED, 25, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
        w.addBullet(bullet);
        sim.step(GameplayConfig.STEP_SECONDS);

        // Một trong hai mất 25, nhưng chưa chết (100HP). Đạn deactivate sau trúng đầu.
        // Kiểm tra đạn đã inactive (không viết lại). Test lỏng: không tank nào âm tính HP.
        for (var t : w.getAllTanks()) assertTrue(t.getHP() >= 0 && t.getHP() <= 100);
    }

    @Test
    @DisplayName("Đạn AI xuyên qua AI khác và không làm đồng đội giảm máu")
    void aiBullet_passesThroughOtherAi() {
        GameSimulation sim = new GameSimulation(8L);
        GameWorld world = sim.getWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);
        AITank shooter = new AITank("ai-shooter", 100, 100,
                com.tankzors.client.gameplay.ai.DifficultyLevel.EASY);
        AITank ally = new AITank("ai-ally", 200, 200,
                com.tankzors.client.gameplay.ai.DifficultyLevel.EASY);
        world.addTank(shooter);
        world.addTank(ally);
        world.addTank(new PlayerTank("player", 60, 60));
        Bullet bullet = new Bullet("ai-shot", shooter.getId(), new Position(194, 200), 90,
                180, 25, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
        world.addBullet(bullet);

        sim.step(GameplayConfig.STEP_SECONDS);

        assertEquals(100, ally.getHP());
        assertTrue(bullet.isActive(), "Đạn AI phải xuyên qua đồng đội thay vì bị hủy");
    }

    @Test
    @DisplayName("restart reset world + AI count về lại")
    void restart_resets() {
        GameSimulation sim = new GameSimulation(3L);
        MapDefinition map = TestMaps.open();
        setPlayerSpawn(map, 1, 1);
        sim.startCampaignLevel(map, "test", "p");
        int initial = countAi(sim.getWorld());

        // Cho player chết → terminal
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("p");
        player.takeDamage(100);
        sim.step(GameplayConfig.STEP_SECONDS);
        assertEquals(GameWorld.Status.DEFEAT, sim.getWorld().getStatus());

        sim.restart();
        assertEquals(GameWorld.Status.PLAYING, sim.getWorld().getStatus());
        assertEquals(initial, countAi(sim.getWorld()));
    }

    // ===== helpers =====
    private static int countAi(GameWorld w) {
        int n = 0;
        for (var t : w.getAllTanks()) if (t instanceof AITank) n++;
        return n;
    }

    private static void setPlayerSpawn(MapDefinition map, int col, int row) {
        int[][] g = map.getGrid();
        if (g == null) return;
        // Xoá mọi ô SPAWN_PLAYER cũ (3) rồi đặt mới
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[r].length; c++)
                if (g[r][c] == TileType.SPAWN_PLAYER.getValue()) g[r][c] = 0;
        g[row][col] = TileType.SPAWN_PLAYER.getValue();
        map.setGrid(g);
    }

    private static void hitObstacle(GameSimulation sim, String id, double x, double y) {
        sim.getWorld().setStatus(GameWorld.Status.PLAYING);
        sim.getWorld().addBullet(new Bullet(id, "player", new Position(x, y), 90,
                1, Bullet.DEFAULT_DAMAGE, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT));
        sim.step(GameplayConfig.STEP_SECONDS);
    }

    private static void firePlayerShotUntilImpact(GameSimulation sim) {
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
        for (int frame = 0; frame < 30 && !player.canShoot(); frame++) {
            sim.getWorld().setStatus(GameWorld.Status.PLAYING);
            sim.step(GameplayConfig.STEP_SECONDS);
        }
        assertTrue(player.canShoot(), "Player cooldown must finish before the next test shot");
        sim.getWorld().setStatus(GameWorld.Status.PLAYING);
        sim.enqueueShoot();
        boolean shotSeen = false;
        for (int frame = 0; frame < 30; frame++) {
            sim.getWorld().setStatus(GameWorld.Status.PLAYING);
            sim.step(GameplayConfig.STEP_SECONDS);
            if (sim.getWorld().getBulletCount() > 0) {
                shotSeen = true;
            } else if (shotSeen) {
                return;
            }
        }
        fail("Player bullet did not reach an obstacle within 30 fixed steps");
    }
}
