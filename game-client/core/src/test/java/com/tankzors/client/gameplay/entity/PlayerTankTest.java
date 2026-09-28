package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.interfaces.IBullet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Parity server entity.PlayerTankTest. Kiểm tra movement/rotation/bbox/hp. */
class PlayerTankTest {

    private PlayerTank tank;

    @BeforeEach
    void setUp() { tank = new PlayerTank("player-1", 100, 100); }

    @Test
    @DisplayName("move(RIGHT, 0.033) dùng tốc độ chậm 60 px/s")
    void moveRight_correctPosition() {
        tank.move(Direction.RIGHT, 0.033);
        assertEquals(101.98, tank.getPosition().getX(), 0.001);
        assertEquals(100.0, tank.getPosition().getY(), 0.001);
        assertEquals(90, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(UP) → y giảm, rotation=0")
    void moveUp_yDecreases() {
        tank.move(Direction.UP, 0.033);
        assertEquals(98.02, tank.getPosition().getY(), 0.001);
        assertEquals(0, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(DOWN) → y tăng, rotation=180; move(LEFT) → rotation=270")
    void moveOtherDirections() {
        tank.move(Direction.DOWN, 0.033);
        assertEquals(101.98, tank.getPosition().getY(), 0.001);
        assertEquals(180, tank.getRotation(), 0.001);

        tank.move(Direction.LEFT, 0.033);
        assertEquals(98.02, tank.getPosition().getX(), 0.001);
        assertEquals(270, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("getBoundingBox: 40x40, tâm position")
    void boundingBox_centered() {
        Rectangle box = tank.getBoundingBox();
        assertEquals(40.0, box.getWidth(), 0.001);
        assertEquals(40.0, box.getHeight(), 0.001);
        assertEquals(80.0, box.getX(), 0.001);
        assertEquals(80.0, box.getY(), 0.001);
    }

    @Test
    @DisplayName("takeDamage giảm HP, clamp ở 0, isAlive cập nhật")
    void takeDamage_clamped() {
        assertEquals(100, tank.getHP());
        assertTrue(tank.isAlive());
        tank.takeDamage(30);
        assertEquals(70, tank.getHP());
        tank.takeDamage(70);
        assertEquals(0, tank.getHP());
        assertFalse(tank.isAlive());
        tank.takeDamage(999);
        assertEquals(0, tank.getHP());
    }

    @Test
    @DisplayName("collidesWith: gần → true, xa → false")
    void collidesWith_range() {
        PlayerTank other = new PlayerTank("p2", 120, 100);
        assertTrue(tank.collidesWith(other));
        PlayerTank far = new PlayerTank("p3", 200, 200);
        assertFalse(tank.collidesWith(far));
    }

    @Test
    @DisplayName("Thông số mặc định parity server")
    void defaultStats() {
        assertEquals(60.0, tank.getSpeed(), 0.001);
        assertEquals(100, tank.getMaxHP());
        assertEquals(40.0, tank.getWidth(), 0.001);
        assertEquals(40.0, tank.getHeight(), 0.001);
        assertEquals("player-1", tank.getId());
    }

    @Test
    @DisplayName("Bấm nhanh hướng mới chỉ xoay tại chỗ; bấm lại mới đi")
    void quickTapOnNewDirectionOnlyTurnsInPlace() {
        tank.setMoving(Direction.DOWN, true);

        tank.updateMovement(0.05, TestMaps.open());

        assertEquals(100.0, tank.getPosition().getX(), 0.001);
        assertEquals(100.0, tank.getPosition().getY(), 0.001);
        assertEquals(180.0, tank.getRotation(), 0.001);

        // Một lần bấm bình thường kéo dài qua vài frame nhưng chưa phải giữ phím.
        tank.updateMovement(0.05, TestMaps.open());
        tank.updateMovement(0.05, TestMaps.open());
        tank.updateMovement(0.05, TestMaps.open());
        tank.setMoving(Direction.DOWN, false);

        assertEquals(100.0, tank.getPosition().getX(), 0.001);
        assertEquals(100.0, tank.getPosition().getY(), 0.001);

        tank.setMoving(Direction.DOWN, true);
        tank.updateMovement(1.0, TestMaps.open());

        assertEquals(100.0, tank.getPosition().getX(), 0.001);
        assertEquals(140.0, tank.getPosition().getY(), 0.001);
        assertEquals(180.0, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("Giữ hướng mới đủ lâu thì bắt đầu đi liên tục")
    void heldNewDirection_startsContinuousMovementAfterHoldDelay() {
        tank.setMoving(Direction.RIGHT, true);
        tank.updateMovement(0.05, TestMaps.open());
        tank.updateMovement(0.05, TestMaps.open());
        tank.updateMovement(0.05, TestMaps.open());
        tank.updateMovement(0.05, TestMaps.open());
        assertEquals(100.0, tank.getPosition().getX(), 0.001);

        tank.updateMovement(0.10, TestMaps.open());
        assertTrue(tank.getPosition().getX() > 100.0,
                "Giữ phím phải bắt đầu di chuyển sau thời gian nhận biết giữ");

        for (int i = 0; i < 20; i++) tank.updateMovement(0.05, TestMaps.open());
        assertTrue(tank.getPosition().getX() > 140.0,
                "Sau khi nhận biết giữ, xe phải tiếp tục đi qua nhiều ô");

        tank.setMoving(Direction.RIGHT, false);
        double releasedAt = tank.getPosition().getX();
        tank.updateMovement(1.0, TestMaps.open());
        assertTrue(tank.getPosition().getX() >= releasedAt,
                "Nhả phím vẫn được hoàn tất ô đang đi nhưng không đảo hướng");
    }

    @Test
    @DisplayName("Đạn người chơi được làm tròn về hướng chính và bay thẳng")
    void shoot_isAlwaysCardinalAndStraight() {
        tank.rotate(44.0);
        IBullet bullet = tank.shoot();

        assertNotNull(bullet);
        assertEquals(0.0, bullet.getDirection(), 0.001);
        bullet.update(0.1);
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
        assertEquals(80.0, bullet.getPosition().getY(), 0.001);
    }
}
