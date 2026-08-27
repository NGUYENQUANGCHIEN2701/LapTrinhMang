package com.tankzors.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Tank/PlayerTank: di chuyển 4 hướng, xoay, bounding box.
 */
class PlayerTankTest {

    private PlayerTank tank;

    @BeforeEach
    void setUp() {
        // Tạo PlayerTank tại vị trí (100, 100)
        tank = new PlayerTank("player-1", 100, 100);
    }

    // ========== ACCEPTANCE CRITERIA TESTS ==========

    @Test
    @DisplayName("[AC] move(RIGHT, 0.033) từ (100,100) → x = 104.95")
    void moveRight_fromOrigin_correctPosition() {
        tank.move(Direction.RIGHT, 0.033);

        // 150 * 0.033 = 4.95
        assertEquals(104.95, tank.getPosition().getX(), 0.001);
        assertEquals(100.0, tank.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("[AC] move(UP, 0.033) → rotation = 0")
    void moveUp_rotationIsZero() {
        tank.move(Direction.UP, 0.033);
        assertEquals(0, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("[AC] getBoundingBox() trả về Rectangle đúng kích thước 40x40")
    void getBoundingBox_correctSize() {
        Rectangle box = tank.getBoundingBox();

        assertEquals(40.0, box.getWidth(), 0.001);
        assertEquals(40.0, box.getHeight(), 0.001);
        // Position là tâm → box.x = 100 - 20 = 80, box.y = 100 - 20 = 80
        assertEquals(80.0, box.getX(), 0.001);
        assertEquals(80.0, box.getY(), 0.001);
    }

    // ========== MOVEMENT 4 DIRECTIONS ==========

    @Test
    @DisplayName("move(UP) → y giảm, rotation = 0")
    void moveUp_yDecreases() {
        tank.move(Direction.UP, 0.033);

        assertEquals(100.0, tank.getPosition().getX(), 0.001);
        assertEquals(95.05, tank.getPosition().getY(), 0.001); // 100 - 4.95
        assertEquals(0, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(DOWN) → y tăng, rotation = 180")
    void moveDown_yIncreases() {
        tank.move(Direction.DOWN, 0.033);

        assertEquals(100.0, tank.getPosition().getX(), 0.001);
        assertEquals(104.95, tank.getPosition().getY(), 0.001); // 100 + 4.95
        assertEquals(180, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(LEFT) → x giảm, rotation = 270")
    void moveLeft_xDecreases() {
        tank.move(Direction.LEFT, 0.033);

        assertEquals(95.05, tank.getPosition().getX(), 0.001); // 100 - 4.95
        assertEquals(100.0, tank.getPosition().getY(), 0.001);
        assertEquals(270, tank.getRotation(), 0.001);
    }

    @Test
    @DisplayName("move(RIGHT) → x tăng, rotation = 90")
    void moveRight_xIncreases() {
        tank.move(Direction.RIGHT, 0.033);

        assertEquals(104.95, tank.getPosition().getX(), 0.001); // 100 + 4.95
        assertEquals(100.0, tank.getPosition().getY(), 0.001);
        assertEquals(90, tank.getRotation(), 0.001);
    }

    // ========== MULTIPLE MOVES ==========

    @Test
    @DisplayName("Di chuyển liên tục nhiều tick tích lũy đúng")
    void multipleMovesAccumulate() {
        // 30 tick sang phải = 1 giây = 150 pixels
        for (int i = 0; i < 30; i++) {
            tank.move(Direction.RIGHT, 0.033);
        }
        // 150 * 0.033 * 30 = 148.5 (do 0.033 không chính xác bằng 1/30)
        double expected = 100 + (150.0 * 0.033 * 30);
        assertEquals(expected, tank.getPosition().getX(), 0.01);
    }

    // ========== HEALTH ==========

    @Test
    @DisplayName("takeDamage giảm HP, isAlive cập nhật đúng")
    void takeDamage_reducesHP() {
        assertEquals(100, tank.getHP());
        assertTrue(tank.isAlive());

        tank.takeDamage(30);
        assertEquals(70, tank.getHP());
        assertTrue(tank.isAlive());

        tank.takeDamage(70);
        assertEquals(0, tank.getHP());
        assertFalse(tank.isAlive());
    }

    @Test
    @DisplayName("takeDamage không giảm dưới 0")
    void takeDamage_doesNotGoBelowZero() {
        tank.takeDamage(999);
        assertEquals(0, tank.getHP());
        assertFalse(tank.isAlive());
    }

    // ========== BOUNDING BOX AFTER MOVE ==========

    @Test
    @DisplayName("getBoundingBox() cập nhật sau khi di chuyển")
    void boundingBox_updatesAfterMove() {
        tank.move(Direction.RIGHT, 1.0); // +150 pixels

        Rectangle box = tank.getBoundingBox();
        // Tâm mới: (250, 100) → box: (230, 80, 40, 40)
        assertEquals(230.0, box.getX(), 0.001);
        assertEquals(80.0, box.getY(), 0.001);
        assertEquals(40.0, box.getWidth(), 0.001);
        assertEquals(40.0, box.getHeight(), 0.001);
    }

    // ========== COLLISION ==========

    @Test
    @DisplayName("Hai tank gần nhau → collidesWith = true")
    void collidesWith_overlapping_returnsTrue() {
        PlayerTank other = new PlayerTank("player-2", 120, 100); // cách 20px < 40px
        assertTrue(tank.collidesWith(other));
    }

    @Test
    @DisplayName("Hai tank xa nhau → collidesWith = false")
    void collidesWith_farApart_returnsFalse() {
        PlayerTank other = new PlayerTank("player-2", 200, 200); // cách xa
        assertFalse(tank.collidesWith(other));
    }

    // ========== CONSTANTS ==========

    @Test
    @DisplayName("PlayerTank có đúng thông số mặc định")
    void defaultStats_correct() {
        assertEquals(150.0, tank.getSpeed(), 0.001);
        assertEquals(100, tank.getMaxHP());
        assertEquals(100, tank.getHP());
        assertEquals(40.0, tank.getWidth(), 0.001);
        assertEquals(40.0, tank.getHeight(), 0.001);
        assertEquals("player-1", tank.getId());
    }
}
