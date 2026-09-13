package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.entity.Bullet;
import com.tankzors.entity.PlayerTank;
import com.tankzors.entity.Position;
import com.tankzors.entity.Wall;
import com.tankzors.map.GameMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class ShootBehaviorTest {

    private GameMap openMap;
    private GameState state;

    @BeforeEach
    public void setUp() {
        openMap = AIMaps.open();
        state = new GameState();
        state.setCurrentMap(openMap);
        state.setStatus(GameState.Status.PLAYING);
    }

    @Test
    @DisplayName("AI bắn đạn về phía player khi trong tầm bắn (<250px) và không có tường")
    public void testShootWhenInRangeAndClearSight() {
        // AI tại (200, 200), Player tại (300, 200) -> khoảng cách 100px < 250px
        AITank ai = new AITank("ai", 200, 200, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 300, 200);

        state.addTank(ai);
        state.addTank(player);

        assertEquals(0, state.getAllBullets().size());

        // Cập nhật 1 tick (0.033s)
        ai.update(state, 0.033);

        assertEquals(1, state.getAllBullets().size(), "AI phải bắn ra 1 viên đạn khi trong tầm và thông thoáng");
        var bullet = state.getAllBullets().get(0);
        assertEquals("ai", bullet.getOwnerId());

        // Player ở bên phải AI (dx > 0, dy = 0) -> góc xoay ngắm là 90° (RIGHT)
        assertEquals(90.0, ai.getRotation(), 1e-3, "AI phải xoay súng về phía player (90 độ - RIGHT)");
    }

    @Test
    @DisplayName("AI không bắn khi player ngoài tầm bắn (>=250px)")
    public void testNoShootWhenOutOfRange() {
        // AI tại (100, 100), Player tại (400, 100) -> khoảng cách 300px >= 250px
        AITank ai = new AITank("ai", 100, 100, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 400, 100);

        state.addTank(ai);
        state.addTank(player);

        ai.update(state, 0.033);

        assertEquals(0, state.getAllBullets().size(), "AI không được bắn khi ngoài tầm 250px");
    }

    @Test
    @DisplayName("AI không bắn khi có tường chắn giữa AI và Player")
    public void testNoShootWhenBlockedByWall() {
        // Tạo map có tường chắn ở giữa AI (row 5, col 5) và Player (row 5, col 8)
        GameMap wallMap = AIMaps.parse(
                "####################",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#.....#............#", // Tường tại col 6, row 5
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "####################"
        );
        state.setCurrentMap(wallMap);

        // AI tại col 4 (x=180, y=220), Player tại col 8 (x=340, y=220). Tường tại col 6 (x=240..280).
        AITank ai = new AITank("ai", 180, 220, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 340, 220);

        state.addTank(ai);
        state.addTank(player);

        ai.update(state, 0.033);

        assertEquals(0, state.getAllBullets().size(), "AI không được bắn khi có tường che khuất");
    }

    @Test
    @DisplayName("AI tuân thủ Cooldown theo độ khó: EASY (2s), MEDIUM (2.5s), HARD (2s)")
    public void testShootCooldownByDifficulty() {
        // Test EASY: 2.0 giây/lần
        testCooldownForDifficulty(DifficultyLevel.EASY, DifficultyLevel.EASY.getShootCooldown());

        // Test MEDIUM: 2.5 giây/lần
        testCooldownForDifficulty(DifficultyLevel.MEDIUM, DifficultyLevel.MEDIUM.getShootCooldown());

        // Test HARD: 2.0 giây/lần
        testCooldownForDifficulty(DifficultyLevel.HARD, DifficultyLevel.HARD.getShootCooldown());
    }

    private void testCooldownForDifficulty(DifficultyLevel difficulty, double expectedCooldown) {
        GameState testState = new GameState();
        testState.setCurrentMap(openMap);
        testState.setStatus(GameState.Status.PLAYING);

        AITank ai = new AITank("ai-" + difficulty.name(), 200, 200, difficulty);
        PlayerTank player = new PlayerTank("player", 300, 200);
        testState.addTank(ai);
        testState.addTank(player);

        // Phát bắn 1: ngay lập tức ở tick đầu tiên
        ai.update(testState, 0.033);
        assertEquals(1, testState.getAllBullets().size(), difficulty.name() + " phải bắn phát đầu tiên");

        // Cập nhật thời gian gần bằng cooldown nhưng chưa đủ (cooldown - 0.1s)
        double elapsed = 0.033;
        while (elapsed < expectedCooldown - 0.1) {
            ai.update(testState, 0.1);
            elapsed += 0.1;
        }
        assertEquals(1, testState.getAllBullets().size(), difficulty.name() + " không được bắn khi chưa hết cooldown");

        // Cập nhật thêm vượt qua mốc cooldown
        ai.update(testState, 0.2);
        assertEquals(2, testState.getAllBullets().size(), difficulty.name() + " phải bắn phát thứ 2 sau cooldown " + expectedCooldown + "s");
    }

    @Test
    @DisplayName("Độ chính xác và độ lệch góc bắn: EASY (+/-15°), MEDIUM (+/-8°), HARD (+/-3°)")
    public void testAccuracySpread() {
        // Player ở bên phải AI: góc ngắm chuẩn là 90°
        Position aiPos = new Position(200, 200);
        Position playerPos = new Position(300, 200);

        for (DifficultyLevel diff : DifficultyLevel.values()) {
            double maxSpread = diff.getAccuracySpread();

            for (int i = 0; i < 50; i++) {
                GameState testState = new GameState();
                testState.setCurrentMap(openMap);
                testState.setStatus(GameState.Status.PLAYING);

                AITank ai = new AITank("ai", aiPos.getX(), aiPos.getY(), diff);
                PlayerTank player = new PlayerTank("player", playerPos.getX(), playerPos.getY());
                testState.addTank(ai);
                testState.addTank(player);

                ai.update(testState, 0.033);

                assertEquals(1, testState.getAllBullets().size());
                double bulletDir = testState.getAllBullets().get(0).getDirection();

                // Độ lệch so với góc chuẩn 90°
                double diffAngle = Math.abs(bulletDir - 90.0);
                if (diffAngle > 180) diffAngle = 360 - diffAngle;

                assertTrue(diffAngle <= maxSpread + 1e-6,
                        String.format("%s độ lệch %.2f° phải <= %.2f°", diff.name(), diffAngle, maxSpread));
            }
        }
    }

    @Test
    @DisplayName("AI không bắn khi Player đã chết")
    public void testNoShootWhenPlayerDead() {
        AITank ai = new AITank("ai", 200, 200, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 300, 200);
        player.takeDamage(100); // Player chết

        state.addTank(ai);
        state.addTank(player);

        ai.update(state, 0.033);

        assertEquals(0, state.getAllBullets().size(), "AI không bắn người chơi đã chết");
    }
}
