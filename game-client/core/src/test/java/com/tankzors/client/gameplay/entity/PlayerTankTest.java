package com.tankzors.client.gameplay.entity;

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
    @DisplayName("move(RIGHT, 0.033) → x = 104.95")
    void moveRight_correctPosition() {
        tank.move(Direction.RIGHT, 0.033);
        assertEquals(104.95, tank.getPosition().getX(), 0.001);
        assertEquals(100.0, tank.getPosition().getY(), 0.001);
        assertEquals(90, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(UP) → y giảm, rotation=0")
    void moveUp_yDecreases() {
        tank.move(Direction.UP, 0.033);
        assertEquals(95.05, tank.getPosition().getY(), 0.001);
        assertEquals(0, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(DOWN) → y tăng, rotation=180; move(LEFT) → rotation=270")
    void moveOtherDirections() {
        tank.move(Direction.DOWN, 0.033);
        assertEquals(104.95, tank.getPosition().getY(), 0.001);
        assertEquals(180, tank.getRotation(), 0.001);

        tank.move(Direction.LEFT, 0.033);
        assertEquals(95.05, tank.getPosition().getX(), 0.001);
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
        assertEquals(150.0, tank.getSpeed(), 0.001);
        assertEquals(100, tank.getMaxHP());
        assertEquals(40.0, tank.getWidth(), 0.001);
        assertEquals(40.0, tank.getHeight(), 0.001);
        assertEquals("player-1", tank.getId());
    }
}
