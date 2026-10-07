package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.map.MapDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Hành vi port từ game.Bot / game.Tank của Builds, đo theo frame 60 Hz. */
class BuildsBotBrainTest {

    private static final double FRAME = BuildsBotBrain.FRAME_SECONDS;

    private static GameWorld world(MapDefinition map) {
        GameWorld world = new GameWorld();
        world.setCurrentMap(map);
        world.setStatus(GameWorld.Status.PLAYING);
        return world;
    }

    private static AITank bot(GameWorld world, int col, int row, double rotation) {
        AITank tank = AITank.builds("ai-" + col + "-" + row, col * 40 + 20, row * 40 + 20, 0, 1);
        tank.rotate(rotation);
        tank.getController().seed(1L, owner -> "b-" + owner);
        world.addTank(tank);
        return tank;
    }

    private static void frames(AITank tank, GameWorld world, int count) {
        for (int i = 0; i < count; i++) tank.update(world, FRAME);
    }

    private static int bulletsFrom(GameWorld world, AITank tank) {
        int n = 0;
        for (IBullet b : world.getAllBullets()) if (tank.getId().equals(b.getOwnerId())) n++;
        return n;
    }

    @Test
    @DisplayName("Xe nhẹ cấp 1 trên cỏ đi một ô sau floor(100 / (3 × 0.8)) = 41 frame")
    void movesCellByCellAtBuildsSpeed() {
        MapDefinition map = TestMaps.parse(
                "#######",
                "#.....#",
                "#.....#",
                "#.....#",
                "#.....#",
                "#######");
        map.setVisualGrid(new int[][]{
                {1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1}});
        GameWorld world = world(map);
        AITank tank = bot(world, 1, 4, 0);
        BuildsBotBrain brain = tank.getController().brain();

        frames(tank, world, 1);
        assertEquals(BuildsBotBrain.Motion.MOVING, brain.motion(), "Đường thông thì đi thẳng ngay");
        frames(tank, world, 40);
        assertEquals(4, brain.row(), "Mới 40 bước di chuyển thì vẫn thuộc ô cũ");
        frames(tank, world, 1);
        assertEquals(3, brain.row());
        assertEquals(3 * 40 + 20, tank.getPosition().getY(), 1e-6, "Tới ô thì khớp đúng tâm ô");
    }

    @Test
    @DisplayName("Thấy địch trên tia ngang: quay 22.5° mỗi 4 frame rồi bắn phát đầu ở frame 20, sau đó mỗi 60 frame")
    void rotatesTowardSightLineAndFiresOnBuildsCadence() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#........#",
                "##########");
        GameWorld world = world(map);
        AITank tank = bot(world, 1, 1, 0);
        world.addTank(new PlayerTank("player", 6 * 40 + 20, 1 * 40 + 20));

        frames(tank, world, 1);
        assertEquals(BuildsBotBrain.AiState.ATTACK, tank.getController().brain().aiState());
        frames(tank, world, 12);
        assertTrue(tank.getRotation() > 0 && tank.getRotation() < 90, "Đang quay dần, chưa tới 90°");
        frames(tank, world, 8);
        assertEquals(90.0, tank.getRotation(), 1e-9);
        assertEquals(1, bulletsFrom(world, tank), "Phát đầu sau 20 frame kể từ lúc phát hiện");
        frames(tank, world, 59);
        assertEquals(1, bulletsFrom(world, tank));
        frames(tank, world, 1);
        assertEquals(2, bulletsFrom(world, tank), "Nạp đạn 60 frame");
    }

    @Test
    @DisplayName("Tia nhìn bị tường che, và tia bên hông chỉ dài 6 ô (10 ô phía trước)")
    void sightLinesAreBlockedAndShorterSideways() {
        MapDefinition walled = TestMaps.parse(
                "##########",
                "#...#....#",
                "##########");
        GameWorld world = world(walled);
        AITank tank = bot(world, 1, 1, 0);
        world.addTank(new PlayerTank("player", 7 * 40 + 20, 1 * 40 + 20));
        frames(tank, world, 1);
        assertNotEquals(BuildsBotBrain.AiState.ATTACK, tank.getController().brain().aiState(),
                "Gạch che tầm nhìn");

        MapDefinition corridor = TestMaps.parse(
                "############",
                "#..........#",
                "############");
        GameWorld far = world(corridor);
        AITank side = bot(far, 1, 1, 0);
        far.addTank(new PlayerTank("player", 8 * 40 + 20, 1 * 40 + 20));
        frames(side, far, 1);
        assertNotEquals(BuildsBotBrain.AiState.ATTACK, side.getController().brain().aiState(),
                "Cách 7 ô bên hông: ngoài tia 6 ô");

        GameWorld ahead = world(corridor);
        AITank facing = bot(ahead, 1, 1, 90);
        ahead.addTank(new PlayerTank("player", 8 * 40 + 20, 1 * 40 + 20));
        frames(facing, ahead, 1);
        assertEquals(BuildsBotBrain.AiState.ATTACK, facing.getController().brain().aiState(),
                "Cùng khoảng cách nhưng nằm phía trước: thấy");
    }

    @Test
    @DisplayName("Địch sau gạch trong vùng 5 ô: tìm đường xuyên gạch và bắn phá gạch")
    void plansThroughBricksAndShootsThem() {
        MapDefinition map = TestMaps.parse(
                "#######",
                "#.....#",
                "#.###.#",
                "#.#.#.#",
                "#######");
        GameWorld world = world(map);
        // Player kẹt trong ô (3,3); bot đứng trên ô (3,1), giữa là gạch (3,2).
        AITank tank = bot(world, 3, 1, 180);
        world.addTank(new PlayerTank("player", 3 * 40 + 20, 3 * 40 + 20));
        frames(tank, world, 1);
        assertTrue(tank.getController().brain().pathLength() > 0, "Lập đường tới player");
        frames(tank, world, 30);
        assertTrue(bulletsFrom(world, tank) > 0, "Gạch chặn đường → bắn phá");
    }

    @Test
    @DisplayName("Bị bắn từ bên hông khi chưa giao chiến thì quay về phía phát đạn")
    void turnsTowardAttacker() {
        // Ô kín: không có đường rẽ nên xe chỉ quay khi bị bắn.
        MapDefinition map = TestMaps.parse(
                "###",
                "#.#",
                "###");
        GameWorld world = world(map);
        AITank tank = bot(world, 1, 1, 0);
        frames(tank, world, 1);
        // Đạn bay sang trái (270°) trúng xe → kẻ bắn ở bên phải.
        tank.getController().onAttacked(270);
        frames(tank, world, 30);
        assertEquals(90.0, tank.getRotation(), 1e-9);
    }

    @Test
    @DisplayName("Đồng minh FollowPlayer lập đường tới ô người chơi")
    void allyFollowsPlayer() {
        MapDefinition map = TestMaps.parse(
                "##########",
                "#........#",
                "#........#",
                "##########");
        GameWorld world = world(map);
        AITank ally = bot(world, 1, 1, 180);
        ally.setTeam(AITank.Team.ALLY);
        ally.setFollowPlayer(true);
        world.addTank(new PlayerTank("player", 8 * 40 + 20, 2 * 40 + 20));
        frames(ally, world, 300);
        assertTrue(ally.getPosition().getX() > 4 * 40, "Đồng minh đã đi về phía người chơi");
    }

    @Test
    @DisplayName("Chỉ số Builds: loại 0 cấp 1 có 3 máu, loại 1 cấp 1 có 5 máu, đạn thường")
    void buildsStatsTable() {
        assertEquals(3, AITank.builds("a", 0, 0, 0, 1).getHP());
        assertEquals(5, AITank.builds("b", 0, 0, 1, 1).getHP());
        assertEquals(3.0, BuildsTankStats.speed(0, 1));
        assertEquals(0, BuildsTankStats.bulletType(1, 1));
        assertEquals(4, BuildsTankStats.rotationDelay(0));
        assertEquals(Bullet.DEFAULT_SPEED, com.tankzors.client.gameplay.WeaponProfile.fromItemId(0).speedPixelsPerSecond());
    }
}
