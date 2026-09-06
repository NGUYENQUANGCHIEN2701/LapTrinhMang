package com.tankzors.engine;

import com.tankzors.entity.PlayerTank;
import com.tankzors.entity.Position;
import com.tankzors.entity.Rectangle;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Task 1.9: CollisionManager & AABB Collision.
 */
class CollisionManagerTest {

    private GameMap map;
    private PlayerTank playerTank;

    // Dummy bullet phục vụ kiểm thử va chạm đạn
    static class MockBullet implements IBullet, ICollidable {
        private final String ownerId;
        private Position position;
        private boolean active = true;

        public MockBullet(String ownerId, double x, double y) {
            this.ownerId = ownerId;
            this.position = new Position(x, y);
        }

        @Override public String getOwnerId() { return ownerId; }
        @Override public Position getPosition() { return position; }
        @Override public double getDirection() { return 0; }
        @Override public double getSpeed() { return 300; }
        @Override public int getDamage() { return 25; }
        @Override public boolean isActive() { return active; }
        @Override public void deactivate() { this.active = false; }
        @Override public void update(double deltaTime) {}
        @Override public Rectangle getBoundingBox() {
            return new Rectangle(position.getX() - 4, position.getY() - 4, 8, 8);
        }
        @Override public boolean collidesWith(ICollidable other) {
            return getBoundingBox().intersects(other.getBoundingBox());
        }
    }

    @BeforeEach
    void setUp() {
        map = MapLoader.load("campaign_1");
        assertNotNull(map);
        // Đặt tank tại khu vực đất trống (100, 100)
        playerTank = new PlayerTank("player-1", 100, 100);
    }

    @Test
    @DisplayName("[AC] Xe tăng trong vùng trống không bị va chạm")
    void checkTankWallCollision_freeSpace_returnsFalse() {
        // Vị trí (100, 100) cách xa tường
        assertFalse(CollisionManager.checkTankWallCollision(playerTank, map, new Position(100, 100)));
        assertFalse(CollisionManager.checkTankWallCollision(playerTank, map, new Position(120, 100)));
    }

    @Test
    @DisplayName("[AC] Xe tăng đâm vào tường ngoài biên -> phát hiện va chạm")
    void checkTankWallCollision_boundaryWall_returnsTrue() {
        // Tường trái nằm từ X=0 đến X=40. Xe tăng 40x40 ở tâm X=50 sẽ có box từ 30 đến 70 (chạm tường 0-40)
        assertTrue(CollisionManager.checkTankWallCollision(playerTank, map, new Position(50, 100)));

        // Vượt ra ngoài biên âm
        assertTrue(CollisionManager.checkTankWallCollision(playerTank, map, new Position(-10, 100)));

        // Vượt ra ngoài biên phải (> 800px)
        assertTrue(CollisionManager.checkTankWallCollision(playerTank, map, new Position(820, 100)));
    }

    @Test
    @DisplayName("[AC] resolveCollision: Đi vào vùng trống -> trả về đúng vị trí mới")
    void resolveCollision_freeMove_returnsTargetPos() {
        Position target = new Position(105, 100);
        Position resolved = CollisionManager.resolveCollision(playerTank, target, map);

        assertEquals(105.0, resolved.getX(), 0.001);
        assertEquals(100.0, resolved.getY(), 0.001);
    }

    @Test
    @DisplayName("[AC] resolveCollision: Đâm thẳng vào tường -> dừng lại, giữ vị trí cũ")
    void resolveCollision_headOnWall_staysAtCurrentPos() {
        // Đặt tank sát mép tường trái: center = 61 (left box = 41, tường 0-40)
        playerTank = new PlayerTank("player-1", 61, 100);

        // Cố tình đi tiếp sang trái vào tường (target = 50, left box = 30 chạm tường 0-40)
        Position target = new Position(50, 100);
        Position resolved = CollisionManager.resolveCollision(playerTank, target, map);

        // Không đi được sang trái -> giữ nguyên vị trí cũ 61
        assertEquals(61.0, resolved.getX(), 0.001);
        assertEquals(100.0, resolved.getY(), 0.001);
    }

    @Test
    @DisplayName("[AC] resolveCollision: Cơ chế trượt tường (Wall Sliding)")
    void resolveCollision_diagonalMoveNearWall_slidesAlongWall() {
        // Đặt tank gần tường trái (center X = 61), muốn đi chéo (X giảm thành 50, Y tăng thành 120)
        playerTank = new PlayerTank("player-1", 61, 100);

        Position target = new Position(50, 120); // Đi chéo vào tường
        Position resolved = CollisionManager.resolveCollision(playerTank, target, map);

        // Trục X bị chặn ở 61, nhưng trục Y được phép trượt xuống 120
        assertEquals(61.0, resolved.getX(), 0.001, "Trục X phải giữ nguyên để không xuyên tường");
        assertEquals(120.0, resolved.getY(), 0.001, "Trục Y phải trượt mượt mà");
    }

    @Test
    @DisplayName("[AC] checkBulletWallCollision: Đạn chạm tường bị huỷ, đạn trong vùng trống an toàn")
    void checkBulletWallCollision_correctBehavior() {
        // Đạn ở vùng trống (100, 100)
        MockBullet openBullet = new MockBullet("player-1", 100, 100);
        assertFalse(CollisionManager.checkBulletWallCollision(openBullet, map));

        // Đạn chạm tường trên (Y = 10, nằm trong ô tường Y từ 0 đến 40)
        MockBullet hitWallBullet = new MockBullet("player-1", 100, 10);
        assertTrue(CollisionManager.checkBulletWallCollision(hitWallBullet, map));

        // Đạn bay ra ngoài map (X = -5)
        MockBullet outOfBoundsBullet = new MockBullet("player-1", -5, 100);
        assertTrue(CollisionManager.checkBulletWallCollision(outOfBoundsBullet, map));
    }

    @Test
    @DisplayName("[AC] checkBulletTankCollision: Đạn trúng địch -> true, tự bắn mình -> false")
    void checkBulletTankCollision_hitEnemy_ignoresSelf() {
        PlayerTank enemyTank = new PlayerTank("enemy-1", 200, 200);

        // Đạn của player-1 tại toạ độ của enemy (200, 200) -> trúng địch
        MockBullet bulletHitEnemy = new MockBullet("player-1", 200, 200);
        assertTrue(CollisionManager.checkBulletTankCollision(bulletHitEnemy, enemyTank));

        // Đạn của player-1 tại toạ độ của player-1 (100, 100) -> không tự bắn mình
        MockBullet bulletHitSelf = new MockBullet("player-1", 100, 100);
        assertFalse(CollisionManager.checkBulletTankCollision(bulletHitSelf, playerTank));

        // Đạn ở xa (300, 300) -> không trúng
        MockBullet farBullet = new MockBullet("player-1", 300, 300);
        assertFalse(CollisionManager.checkBulletTankCollision(farBullet, enemyTank));
    }

    @Test
    @DisplayName("[AC] Xe tăng di chuyển qua hành lang 1 ô (40px) có lệch tâm nhẹ không bị kẹt")
    void resolveCollision_narrowCorridorWithOffset_canMove() {
        GameMap map2 = MapLoader.load("campaign_2");
        assertNotNull(map2);

        // Trong map 2, cột 5 là hành lang thông thoáng giữa cột 4 và cột 6 (tường).
        // Tâm hành lang là X = (5 + 0.5) * 40 = 220.0
        // Đặt xe hơi lệch tâm (X = 222.0, Y = 300.0)
        PlayerTank tankInCorridor = new PlayerTank("player-corridor", 222.0, 300.0);

        // Xe muốn đi xuống: target Y = 305.0
        Position targetDown = new Position(222.0, 305.0);
        Position resolvedDown = CollisionManager.resolveCollision(tankInCorridor, targetDown, map2);

        // Xe phải đi tiếp được (Y phải thay đổi), không bị đóng băng ở 300.0!
        assertTrue(resolvedDown.getY() > 300.0, "Xe phải di chuyển được xuống dọc hành lang dù hơi lệch tâm");

        // Xe muốn đi lên: target Y = 295.0
        Position targetUp = new Position(222.0, 295.0);
        Position resolvedUp = CollisionManager.resolveCollision(tankInCorridor, targetUp, map2);

        assertTrue(resolvedUp.getY() < 300.0, "Xe phải di chuyển được lên dọc hành lang dù hơi lệch tâm");
    }
}
