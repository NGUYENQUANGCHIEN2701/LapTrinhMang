package com.tankzors.client.gameplay.collision;

import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Rectangle;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.ICollidable;
import com.tankzors.client.gameplay.map.MapDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Parity server engine.CollisionManagerTest. */
class CollisionTest {

    private MapDefinition map;
    private PlayerTank playerTank;

    static class MockBullet implements IBullet, ICollidable {
        private final String ownerId;
        private final Position position;
        private boolean active = true;
        MockBullet(String ownerId, double x, double y) {
            this.ownerId = ownerId; this.position = new Position(x, y);
        }
        @Override public String getOwnerId() { return ownerId; }
        @Override public Position getPosition() { return position; }
        @Override public double getDirection() { return 0; }
        @Override public double getSpeed() { return 300; }
        @Override public int getDamage() { return 25; }
        @Override public boolean isActive() { return active; }
        @Override public void deactivate() { this.active = false; }
        @Override public void update(double dt) {}
        @Override public Rectangle getBoundingBox() { return new Rectangle(position.getX() - 4, position.getY() - 4, 8, 8); }
        @Override public boolean collidesWith(ICollidable other) { return getBoundingBox().intersects(other.getBoundingBox()); }
    }

    @BeforeEach
    void setUp() {
        map = TestMaps.open();
        playerTank = new PlayerTank("player-1", 100, 100);
    }

    @Test
    @DisplayName("Vùng trống không va chạm")
    void freeSpace_noCollision() {
        assertFalse(Collision.checkTankWallCollision(playerTank, map, new Position(100, 100)));
    }

    @Test
    @DisplayName("Đâm biên/tường → true")
    void boundaryWall_collision() {
        // Box 32x32 inset, tâm 50 → box 34.. = chạm viền 0-40
        assertTrue(Collision.checkTankWallCollision(playerTank, map, new Position(50, 100)));
        assertTrue(Collision.checkTankWallCollision(playerTank, map, new Position(-10, 100)));
        assertTrue(Collision.checkTankWallCollision(playerTank, map, new Position(820, 100)));
    }

    @Test
    @DisplayName("resolveCollision: vùng trống → trả target")
    void resolve_free() {
        Position resolved = Collision.resolveCollision(playerTank, new Position(105, 100), map);
        assertEquals(105.0, resolved.getX(), 0.001);
        assertEquals(100.0, resolved.getY(), 0.001);
    }

    @Test
    @DisplayName("resolveCollision: đâm thẳng tường → giữ cũ")
    void resolve_headOnWall_stays() {
        playerTank = new PlayerTank("player-1", 61, 100);
        Position resolved = Collision.resolveCollision(playerTank, new Position(50, 100), map);
        assertEquals(61.0, resolved.getX(), 0.001);
        assertEquals(100.0, resolved.getY(), 0.001);
    }

    @Test
    @DisplayName("resolveCollision: trượt tường (wall slide)")
    void resolve_wallSlide() {
        playerTank = new PlayerTank("player-1", 61, 100);
        Position resolved = Collision.resolveCollision(playerTank, new Position(50, 120), map);
        assertEquals(61.0, resolved.getX(), 0.001, "X giữ nguyên");
        assertEquals(120.0, resolved.getY(), 0.001, "Y trượt mượt");
    }

    @Test
    @DisplayName("checkBulletWallCollision đúng hành vi")
    void bulletWall() {
        assertFalse(Collision.checkBulletWallCollision(new MockBullet("p", 100, 100), map));
        // Tường viền trên (y=10 trong ô 0-40)
        assertTrue(Collision.checkBulletWallCollision(new MockBullet("p", 100, 10), map));
        assertTrue(Collision.checkBulletWallCollision(new MockBullet("p", -5, 100), map));
    }

    @Test
    @DisplayName("checkBulletTankCollision: trúng địch true, tự bắn false, xa false")
    void bulletTank() {
        PlayerTank enemy = new PlayerTank("enemy", 200, 200);
        assertTrue(Collision.checkBulletTankCollision(new MockBullet("player-1", 200, 200), enemy));
        assertFalse(Collision.checkBulletTankCollision(new MockBullet("player-1", 100, 100), playerTank));
        assertFalse(Collision.checkBulletTankCollision(new MockBullet("player-1", 300, 300), enemy));
    }

    @Test
    @DisplayName("Hành lang 1 ô lệch tâm không kẹt (corner nudge)")
    void narrowCorridor_nudge() {
        // Hành lang dọc cột 5 (x 200-240): cột 4 và cột 6 là tường, cột 5 trống.
        // Tank lệch tâm nhẹ ở x=222 (tâm hành lang 220), đi xuống y=100→105.
        MapDefinition corridor = TestMaps.parse(
                "####################",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "#.....#............#",
                "####################"
        );
        PlayerTank t = new PlayerTank("t", 222.0, 100.0);
        Position down = Collision.resolveCollision(t, new Position(222.0, 105.0), corridor);
        assertTrue(down.getY() > 100.0, "Phải đi xuống dù lệch tâm, thực=" + down.getY());
    }
}
