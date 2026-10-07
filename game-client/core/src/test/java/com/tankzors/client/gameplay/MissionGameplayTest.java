package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.ITank;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.MapLoader;
import com.tankzors.client.gameplay.map.MissionConfig;
import com.tankzors.client.gameplay.map.TileType;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ShopItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Kịch bản màn Builds 2/3: đợt địch, đồng minh, ụ súng, vật phẩm, trigger, thuốc nổ. */
class MissionGameplayTest {

    private static final double STEP = GameplayConfig.STEP_SECONDS;

    private static void run(GameSimulation sim, double seconds) {
        int steps = (int) Math.ceil(seconds / STEP);
        for (int i = 0; i < steps; i++) sim.step(STEP);
    }

    private static List<AITank> aiTanks(GameWorld world, boolean ally, boolean turret) {
        List<AITank> result = new ArrayList<>();
        for (ITank t : world.getAllTanks()) {
            if (t instanceof AITank && ((AITank) t).isAlly() == ally && ((AITank) t).isTurret() == turret) {
                result.add((AITank) t);
            }
        }
        return result;
    }

    private static MissionConfig.Waves waves(int maxOnField, MissionConfig.SpawnBase... bases) {
        List<MissionConfig.SpawnBase> list = new ArrayList<>();
        Collections.addAll(list, bases);
        return new MissionConfig.Waves(maxOnField, 1.0, "EASY", false, list);
    }

    /** Ụ súng vô hại ở góc để trận chưa thắng ngay khi test chưa có xe địch. */
    private static MissionConfig keepPlaying(MissionConfig mission, int col, int row) {
        mission.getTurrets().add(new MissionConfig.TurretConfig(col, row, Direction.UP, 1, 8));
        mission.setObjective(MissionConfig.Objective.DESTROY_ALL);
        return mission;
    }

    private static GameSimulation start(MapDefinition map, MissionConfig mission, PlayerShopState shop) {
        map.setMission(mission);
        GameSimulation sim = new GameSimulation(5L);
        sim.setShopState(shop);
        sim.startCampaignLevel(map, "campaign_2", "player");
        return sim;
    }

    private static PlayerTank player(GameSimulation sim) {
        return (PlayerTank) sim.getWorld().getTank("player");
    }

    @Test
    @DisplayName("Địch ra lần lượt theo base, tối đa maxOnField; thắng chỉ khi hạ hết cả xe còn chờ")
    void enemyWavesRespectLimitOrderAndVictory() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#o.......#",
                "#........#",
                "#......x.#",
                "#......x.#",
                "##########");
        MissionConfig mission = new MissionConfig();
        mission.setEnemyWaves(waves(1, new MissionConfig.SpawnBase(7, 4, 2), new MissionConfig.SpawnBase(7, 3, 1)));
        GameSimulation sim = start(map, mission, new PlayerShopState());
        GameWorld world = sim.getWorld();
        player(sim).setMaxHp(999);
        player(sim).setHp(999);

        int guard = 0;
        while (aiTanks(world, false, false).isEmpty() && guard++ < 60) sim.step(STEP);
        // AI có thể đã nhích trong chính bước sinh; chỉ cần nằm trong ô base đầu danh sách.
        assertEquals(4, (int) Math.floor(aiTanks(world, false, false).get(0).getPosition().getY() / 40),
                "Base đầu danh sách sinh trước");
        run(sim, 1.2);
        List<AITank> onField = aiTanks(world, false, false);
        assertEquals(1, onField.size(), "Chỉ một xe địch trên sân cùng lúc");
        assertEquals(3, sim.getEnemyTanksRemaining());

        for (int wave = 0; wave < 3; wave++) {
            List<AITank> alive = aiTanks(world, false, false);
            assertEquals(1, alive.size());
            assertEquals(GameWorld.Status.PLAYING, world.getStatus(), "Còn xe chờ thì chưa thắng");
            alive.get(0).takeDamage(100);
            run(sim, 1.5);
        }
        assertEquals(0, sim.getEnemyTanksRemaining());
        assertEquals(GameWorld.Status.VICTORY, world.getStatus());
    }

    @Test
    @DisplayName("Đồng minh ra khi người chơi rời base, thù địch với phe địch, đạn người chơi không trúng")
    void allySpawnsFromPlayerBaseAndIsFriendly() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#o.......#",
                "#........#",
                "#........#",
                "##########");
        MissionConfig mission = new MissionConfig();
        List<MissionConfig.SpawnBase> allyBase = new ArrayList<>();
        allyBase.add(new MissionConfig.SpawnBase(1, 1, 1));
        mission.setAllies(new MissionConfig.Waves(1, 1.0, "EASY", true, allyBase));
        keepPlaying(mission, 8, 3);
        GameSimulation sim = start(map, mission, new PlayerShopState());
        GameWorld world = sim.getWorld();

        run(sim, 2.0);
        assertTrue(aiTanks(world, true, false).isEmpty(), "Base bị xe người chơi chiếm thì chưa sinh");

        player(sim).getPosition().setX(6 * 40 + 20);
        run(sim, 1.2);
        List<AITank> allies = aiTanks(world, true, false);
        assertEquals(1, allies.size());
        AITank ally = allies.get(0);
        assertTrue(ally.isFollowPlayer());
        assertFalse(ally.isHostileTo(player(sim)));

        AITank enemy = new AITank("ai-test", 300, 140, com.tankzors.client.gameplay.ai.DifficultyLevel.EASY);
        assertTrue(enemy.isHostileTo(ally), "Địch nhắm cả đồng minh");
        assertTrue(ally.isHostileTo(enemy));

        int hp = ally.getHP();
        Bullet friendly = new Bullet("b-player-1", "player", new Position(ally.getPosition().getX() - 30,
                ally.getPosition().getY()), 90, 300, 1, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
        world.addBullet(friendly);
        run(sim, 0.3);
        assertEquals(hp, ally.getHP(), "Đạn người chơi xuyên qua đồng minh");
    }

    @Test
    @DisplayName("Ụ súng chỉ bắn theo hướng nòng và chỉ bắt buộc hạ khi objective DESTROY_ALL")
    void turretFiresAlongFacingAndObjectiveDecidesVictory() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#........#",
                "#........#",
                "#o.......#",
                "##########");
        MissionConfig mission = new MissionConfig();
        mission.getTurrets().add(new MissionConfig.TurretConfig(3, 1, Direction.RIGHT, 4, 8));
        mission.setObjective(MissionConfig.Objective.DESTROY_ALL);
        GameSimulation sim = start(map, mission, new PlayerShopState());
        GameWorld world = sim.getWorld();
        AITank turret = aiTanks(world, false, true).get(0);
        assertEquals(8, turret.getHP());

        run(sim, 1.0);
        assertEquals(0, world.getBulletCount(), "Người chơi không nằm trên đường nòng → không bắn");
        assertEquals(GameWorld.Status.PLAYING, world.getStatus(), "DESTROY_ALL: ụ súng còn sống thì chưa thắng");

        player(sim).getPosition().setX(6 * 40 + 20);
        player(sim).getPosition().setY(1 * 40 + 20);
        sim.step(STEP);
        assertTrue(world.getBulletCount() > 0, "Người chơi trong tầm phía trước nòng → bắn");

        MapDefinition tanksOnly = TestMaps.parse(
                "##########",
                "#........#",
                "#o.......#",
                "##########");
        MissionConfig tanksMission = new MissionConfig();
        tanksMission.getTurrets().add(new MissionConfig.TurretConfig(6, 1, Direction.DOWN, 4, 8));
        GameSimulation other = start(tanksOnly, tanksMission, new PlayerShopState());
        run(other, 0.2);
        assertEquals(GameWorld.Status.VICTORY, other.getWorld().getStatus(),
                "DESTROY_TANKS: không còn xe địch thì thắng dù ụ súng còn");
    }

    @Test
    @DisplayName("Vật phẩm Builds: đóng băng, sao, xu, thùng đồ; bộ sửa ở lại khi đầy máu")
    void buildsPickupsApplyTheirEffects() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#o.......#",
                "##########");
        int[][] grid = map.getGrid();
        grid[1][2] = TileType.REPAIR_PICKUP.getValue();
        grid[1][3] = TileType.FREEZE_PICKUP.getValue();
        grid[1][4] = TileType.STAR_PICKUP.getValue();
        grid[1][5] = TileType.COIN_PICKUP.getValue();
        grid[1][6] = TileType.BOX_PICKUP.getValue();
        map.setGrid(grid);
        MissionConfig mission = new MissionConfig();
        mission.getBoxes().add(new MissionConfig.BoxConfig(6, 1, ShopItem.DYNAMITE.getId(), 3));
        keepPlaying(mission, 8, 1);
        PlayerShopState shop = new PlayerShopState();
        GameSimulation sim = start(map, mission, shop);
        PlayerTank player = player(sim);

        for (int col = 2; col <= 6; col++) {
            player.getPosition().setX(col * 40 + 20);
            sim.step(STEP);
        }
        assertEquals(TileType.REPAIR_PICKUP, map.getTileAt(2, 1), "Đầy máu thì không nhặt bộ sửa");
        assertEquals(GameSimulation.FREEZE_PICKUP_SECONDS, sim.getFreezeRemaining(), 0.2);
        assertEquals(1, shop.getStars());
        assertEquals(GameSimulation.COIN_PICKUP_VALUE, shop.getCoins());
        assertEquals(3, shop.getItemCount(ShopItem.DYNAMITE.getId()));
        assertNotNull(sim.getHintText(), "Mở thùng hiện tên vật phẩm");
        assertTrue(sim.getHintText().contains("(3)"));
    }

    @Test
    @DisplayName("Trigger: TEXT chỉ một lần, PRESS kích trigger khác, ENABLE mở trigger đang tắt")
    void triggersChainLikeBuilds() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#o.......#",
                "##########");
        MissionConfig mission = new MissionConfig();
        List<MissionConfig.TriggerConfig> t = mission.getTriggers();
        t.add(new MissionConfig.TriggerConfig(2, 1, true, MissionConfig.TriggerAction.PRESS, null, 0, 1));
        t.add(new MissionConfig.TriggerConfig(5, 1, false, MissionConfig.TriggerAction.TEXT, "Dynamite", 5, -1));
        t.add(new MissionConfig.TriggerConfig(3, 1, true, MissionConfig.TriggerAction.ENABLE, null, 0, 1));
        keepPlaying(mission, 8, 1);
        GameSimulation sim = start(map, mission, new PlayerShopState());
        PlayerTank player = player(sim);

        player.getPosition().setX(2 * 40 + 20);
        sim.step(STEP);
        assertNull(sim.getHintText(), "Trigger đích đang tắt thì PRESS không có tác dụng");

        player.getPosition().setX(3 * 40 + 20);
        sim.step(STEP);
        player.getPosition().setX(5 * 40 + 20);
        sim.step(STEP);
        assertEquals("Dynamite", sim.getHintText(), "ENABLE xong, đi vào ô trigger thì hiện gợi ý");

        run(sim, 6);
        assertNull(sim.getHintText(), "Gợi ý tự tắt sau thời lượng");
        player.getPosition().setX(4 * 40 + 20);
        sim.step(STEP);
        player.getPosition().setX(5 * 40 + 20);
        sim.step(STEP);
        assertNull(sim.getHintText(), "Trigger không reusable chỉ kích một lần");
    }

    @Test
    @DisplayName("Thuốc nổ: đặt tốn 1 vật phẩm, nổ sau 3s, phá khối đá và gây sát thương gấp đôi")
    void dynamiteBreaksStoneAndHurtsTurrets() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#o.......#",
                "#........#",
                "#........#",
                "##########");
        int[][] grid = map.getGrid();
        grid[2][4] = TileType.STONE_WALL.getValue();
        map.setGrid(grid);
        MissionConfig mission = new MissionConfig();
        mission.getTurrets().add(new MissionConfig.TurretConfig(3, 3, Direction.DOWN, 4, 8));
        mission.setObjective(MissionConfig.Objective.DESTROY_ALL);
        PlayerShopState shop = new PlayerShopState();
        shop.setItemCount(ShopItem.DYNAMITE.getId(), 2);
        shop.selectWeapon(ShopItem.DYNAMITE.getId());
        GameSimulation sim = start(map, mission, shop);
        PlayerTank player = player(sim);
        player.getPosition().setX(3 * 40 + 20);
        player.getPosition().setY(2 * 40 + 20);
        AITank turret = aiTanks(sim.getWorld(), false, true).get(0);

        // Bullet hit cannot break the stone block.
        assertFalse(map.wallAt(4, 2).isDestructible());

        sim.enqueueShoot();
        sim.step(STEP);
        assertEquals(1, shop.getItemCount(ShopItem.DYNAMITE.getId()));
        assertEquals(TileType.DYNAMITE, map.getTileAt(3, 2));
        player.getPosition().setX(8 * 40 + 20);
        player.getPosition().setY(1 * 40 + 20);

        run(sim, GameSimulation.DYNAMITE_FUSE_SECONDS + 0.2);
        assertEquals(TileType.GROUND, map.getTileAt(4, 2), "Vụ nổ xoá khối đá cạnh thuốc nổ");
        assertEquals(TileType.GROUND, map.getTileAt(3, 2));
        // Ụ súng cách 1 ô: sức nổ 7-1=6, nhân đôi = 12 > 8 HP.
        assertFalse(turret.isAlive());
    }

    @Test
    @DisplayName("Nền cát Builds làm xe chậm còn 0.45/0.8, cỏ giữ nguyên")
    void sandSlowsTanks() {
        MapDefinition map = TestMaps.parse("....", "....");
        int[][] visual = {{1, 56, 1, 1}, {1, 1, 1, 1}};
        map.setVisualGrid(visual);
        assertEquals(1.0, map.speedFactorAt(20, 20), 1e-9);
        assertEquals(0.45 / 0.8, map.speedFactorAt(60, 20), 1e-9);
        assertTrue(map.isSandAt(60, 20));
    }

    @Test
    @DisplayName("campaign_2/3 chuyển từ Builds nạp đủ mission, ụ súng và khối đá")
    void convertedCampaignMapsParse() throws Exception {
        MapDefinition walls = MapLoader.parse(Files.readString(
                Path.of("../assets/maps/campaign_2.json"), StandardCharsets.UTF_8));
        assertNotNull(walls);
        MissionConfig m2 = walls.getMission();
        assertEquals(6, m2.getEnemyWaves().total());
        assertEquals(1, m2.getEnemyWaves().maxOnField);
        assertEquals(15, m2.getEnemyWaves().bases.get(0).y, "Base (15,15) xếp hạng 1 nên ra trước");
        assertTrue(m2.getAllies().followPlayer);
        assertEquals(4, m2.getTriggers().size());

        MapDefinition sandbox = MapLoader.parse(Files.readString(
                Path.of("../assets/maps/campaign_3.json"), StandardCharsets.UTF_8));
        assertNotNull(sandbox);
        MissionConfig m3 = sandbox.getMission();
        assertEquals(MissionConfig.Objective.DESTROY_ALL, m3.getObjective());
        assertEquals(7, m3.getEnemyWaves().total());
        assertEquals(2, m3.getEnemyWaves().maxOnField);
        assertEquals(1, m3.getTurrets().size());
        assertEquals(Direction.RIGHT, m3.getTurrets().get(0).direction);
        assertEquals(ShopItem.DYNAMITE.getId(), m3.getBoxes().get(0).itemId);
        assertEquals(TileType.STONE_WALL, sandbox.getTileAt(0, 12));
        boolean hasSand = false;
        for (int row = 0; row < sandbox.getHeight(); row++) {
            for (int col = 0; col < sandbox.getWidth(); col++) {
                if (sandbox.speedFactorAt(col * 40 + 20, row * 40 + 20) < 1.0) hasSand = true;
            }
        }
        assertTrue(hasSand, "Màn 3 có nền cát làm chậm");
    }
}
