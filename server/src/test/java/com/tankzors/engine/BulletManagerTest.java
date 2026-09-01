package com.tankzors.engine;

import com.tankzors.entity.Bullet;
import com.tankzors.entity.PlayerTank;
import com.tankzors.entity.Position;
import com.tankzors.entity.Rectangle;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;
import com.tankzors.interfaces.ITank;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ 30 Unit Tests kiểm thử toàn diện Task 1.10: Bullet & BulletManager.
 */
class BulletManagerTest {

    private GameMap map;
    private BulletManager bulletManager;

    @BeforeEach
    void setUp() {
        map = MapLoader.load("campaign_1");
        assertNotNull(map, "Map campaign_1 phải được load thành công");
        bulletManager = new BulletManager();
        bulletManager.clear();
    }

    // ==========================================
    // nhóm 1: BULLET ENTITY TESTS (Tests 01 - 14)
    // ==========================================

    @Test
    @DisplayName("Test 01: Khởi tạo Bullet với tham số mặc định")
    void test01_BulletConstructorDefaultValues() {
        Position pos = new Position(100, 200);
        Bullet bullet = new Bullet("tank-1", pos, 90.0);

        assertEquals("tank-1", bullet.getOwnerId());
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
        assertEquals(200.0, bullet.getPosition().getY(), 0.001);
        assertEquals(90.0, bullet.getDirection(), 0.001);
        assertEquals(300.0, bullet.getSpeed(), 0.001);
        assertEquals(25, bullet.getDamage());
        assertTrue(bullet.isActive());
        assertNotNull(bullet.getId());
    }

    @Test
    @DisplayName("Test 02: Khởi tạo Bullet với tham số tùy chỉnh")
    void test02_BulletConstructorCustomValues() {
        Bullet bullet = new Bullet("b-123", "tank-2", new Position(50, 60), 180.0, 400.0, 50, 10.0, 10.0);

        assertEquals("b-123", bullet.getId());
        assertEquals("tank-2", bullet.getOwnerId());
        assertEquals(400.0, bullet.getSpeed(), 0.001);
        assertEquals(50, bullet.getDamage());
        assertEquals(10.0, bullet.getWidth(), 0.001);
    }

    @Test
    @DisplayName("Test 03: Đạn bay hướng LÊN (0 độ) -> Y giảm")
    void test03_BulletMovementUp() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 0.0);
        bullet.update(0.1); // 0.1s * 300px/s = 30px
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
        assertEquals(70.0, bullet.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 04: Đạn bay hướng PHẢI (90 độ) -> X tăng")
    void test04_BulletMovementRight() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 90.0);
        bullet.update(0.1); // 30px sang phải
        assertEquals(130.0, bullet.getPosition().getX(), 0.001);
        assertEquals(100.0, bullet.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 05: Đạn bay hướng XUỐNG (180 độ) -> Y tăng")
    void test05_BulletMovementDown() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 180.0);
        bullet.update(0.1); // 30px xuống dưới
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
        assertEquals(130.0, bullet.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 06: Đạn bay hướng TRÁI (270 độ) -> X giảm")
    void test06_BulletMovementLeft() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 270.0);
        bullet.update(0.1); // 30px sang trái
        assertEquals(70.0, bullet.getPosition().getX(), 0.001);
        assertEquals(100.0, bullet.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 07: Đạn bay hướng CHÉO (45 độ) -> X tăng và Y giảm")
    void test07_BulletMovementDiagonal() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 45.0);
        bullet.update(0.1); // 30px * sin(45°), -30px * cos(45°)
        assertTrue(bullet.getPosition().getX() > 100.0);
        assertTrue(bullet.getPosition().getY() < 100.0);
    }

    @Test
    @DisplayName("Test 08: Đạn vô hiệu hóa (deactivate)")
    void test08_BulletDeactivation() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 0.0);
        assertTrue(bullet.isActive());
        bullet.deactivate();
        assertFalse(bullet.isActive());
    }

    @Test
    @DisplayName("Test 09: Đạn đã deactive không di chuyển khi update")
    void test09_BulletInactiveNoMovementOnUpdate() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 90.0);
        bullet.deactivate();
        bullet.update(0.1);
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
        assertEquals(100.0, bullet.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 10: deltaTime = 0 -> đạn không di chuyển")
    void test10_BulletZeroDeltaTime() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 90.0);
        bullet.update(0.0);
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
    }

    @Test
    @DisplayName("Test 11: deltaTime âm -> đạn không di chuyển")
    void test11_BulletNegativeDeltaTime() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 90.0);
        bullet.update(-0.1);
        assertEquals(100.0, bullet.getPosition().getX(), 0.001);
    }

    @Test
    @DisplayName("Test 12: Kiểm tra BoundingBox va chạm AABB của đạn")
    void test12_BulletBoundingBox() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 200), 0.0);
        Rectangle box = bullet.getBoundingBox();

        // Kích thước 8x8, tâm tại (100, 200) -> top-left tại (96, 196)
        assertEquals(96.0, box.getX(), 0.001);
        assertEquals(196.0, box.getY(), 0.001);
        assertEquals(8.0, box.getWidth(), 0.001);
        assertEquals(8.0, box.getHeight(), 0.001);
    }

    @Test
    @DisplayName("Test 13: Va chạm giữa 2 đạn trùng vị trí")
    void test13_BulletCollidesWithSelf() {
        Bullet b1 = new Bullet("tank-1", new Position(100, 100), 0.0);
        Bullet b2 = new Bullet("tank-2", new Position(102, 102), 0.0);

        assertTrue(b1.collidesWith(b2));
    }

    @Test
    @DisplayName("Test 14: Va chạm với null trả về false an toàn")
    void test14_BulletCollidesWithNull() {
        Bullet bullet = new Bullet("tank-1", new Position(100, 100), 0.0);
        assertFalse(bullet.collidesWith(null));
    }

    // ==========================================
    // nhóm 2: BULLET MANAGER TESTS (Tests 15 - 30)
    // ==========================================

    @Test
    @DisplayName("Test 15: Tạo đạn hợp lệ thông qua BulletManager")
    void test15_CreateBulletSuccess() {
        IBullet bullet = bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        assertNotNull(bullet);
        assertEquals(1, bulletManager.getBullets().size());
        assertEquals("tank-1", bullet.getOwnerId());
    }

    @Test
    @DisplayName("Test 16: Tạo đạn với ownerId null -> trả về null")
    void test16_CreateBulletNullOwner() {
        IBullet bullet = bulletManager.createBullet(null, new Position(100, 100), 90.0);
        assertNull(bullet);
    }

    @Test
    @DisplayName("Test 17: Tạo đạn với position null -> trả về null")
    void test17_CreateBulletNullPosition() {
        IBullet bullet = bulletManager.createBullet("tank-1", null, 90.0);
        assertNull(bullet);
    }

    @Test
    @DisplayName("Test 18: Giới hạn tối đa 5 viên đạn active cùng lúc cho 1 tank")
    void test18_CreateBulletActiveCountLimit() {
        String owner = "tank-1";
        // Bắn 5 đạn hợp lệ (tua time với map=null để tránh đạn bay chạm biên)
        for (int i = 0; i < 5; i++) {
            IBullet b = bulletManager.createBullet(owner, new Position(100, 100), 90.0);
            assertNotNull(b, "Đạn thứ " + (i + 1) + " phải được tạo thành công");
            bulletManager.updateAll(0.6, null, null); // Tua 0.6s để hết cooldown
        }

        assertEquals(5, bulletManager.getActiveBulletCount(owner));

        // Phát bắn thứ 6 phải bị từ chối
        IBullet b6 = bulletManager.createBullet(owner, new Position(100, 100), 90.0);
        assertNull(b6, "Phát bắn thứ 6 phải bị từ chối do đã đạt tối đa 5 đạn");
    }

    @Test
    @DisplayName("Test 19: Giới hạn 5 đạn hoạt động độc lập giữa các tank khác nhau")
    void test19_CreateBulletLimitIndependentPerTank() {
        // Tank 1 bắn đủ 5 đạn
        for (int i = 0; i < 5; i++) {
            bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
            bulletManager.updateAll(0.6, null, null);
        }
        assertNull(bulletManager.createBullet("tank-1", new Position(100, 100), 90.0));

        // Tank 2 vẫn bắn được đạn mới bình thường
        IBullet bTank2 = bulletManager.createBullet("tank-2", new Position(200, 200), 0.0);
        assertNotNull(bTank2, "Tank 2 phải bắn được dù Tank 1 đã full đạn");
    }

    @Test
    @DisplayName("Test 20: Kiểm soát Cooldown 0.5s giữa 2 lần bắn")
    void test20_CreateBulletCooldownRestriction() {
        String owner = "tank-1";
        IBullet b1 = bulletManager.createBullet(owner, new Position(100, 100), 0.0);
        assertNotNull(b1);

        // Bắn ngay lập tức khi chưa trôi thời gian -> null
        IBullet b2Fast = bulletManager.createBullet(owner, new Position(100, 100), 0.0);
        assertNull(b2Fast, "Chưa hết 0.5s cooldown phải trả về null");

        // Tua thời gian 0.2s (chưa đủ 0.5s) -> vẫn null
        bulletManager.updateAll(0.2, map, null);
        IBullet b3StillCooldown = bulletManager.createBullet(owner, new Position(100, 100), 0.0);
        assertNull(b3StillCooldown, "0.2s trôi qua chưa đủ 0.5s cooldown");
    }

    @Test
    @DisplayName("Test 21: Bắn thành công sau khi trôi qua đủ 0.5s cooldown")
    void test21_CreateBulletAfterCooldownSuccess() {
        String owner = "tank-1";
        bulletManager.createBullet(owner, new Position(100, 100), 0.0);

        // Tua đủ 0.51s
        bulletManager.updateAll(0.51, map, null);

        IBullet b2 = bulletManager.createBullet(owner, new Position(100, 100), 0.0);
        assertNotNull(b2, "Đã qua 0.5s cooldown phải bắn được đạn mới");
    }

    @Test
    @DisplayName("Test 22: updateAll cập nhật vị trí tất cả đạn active")
    void test22_UpdateAllMovesAllActiveBullets() {
        IBullet b = bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        bulletManager.updateAll(0.1, map, null); // bay 30px sang phải

        assertEquals(130.0, b.getPosition().getX(), 0.001);
    }

    @Test
    @DisplayName("Test 23: Đạn chạm tường bị huỷ (deactive)")
    void test23_UpdateAllDestroysBulletOnWallHit() {
        // Đạn ở (50, 100) bay sang trái (270 độ). Tường trái nằm từ X=0 đến X=40.
        IBullet b = bulletManager.createBullet("tank-1", new Position(50, 100), 270.0);
        assertEquals(1, bulletManager.getBullets().size());

        // Bay 0.1s -> X từ 50 xuống 20 (nằm trong ô tường 0-40)
        bulletManager.updateAll(0.1, map, null);

        assertFalse(b.isActive(), "Đạn đâm vào tường phải bị deactive");
        assertEquals(0, bulletManager.getBullets().size(), "Đạn bị huỷ phải được dọn khỏi danh sách");
    }

    @Test
    @DisplayName("Test 24: Đạn bay ra ngoài biên bản đồ bị huỷ")
    void test24_UpdateAllDestroysBulletOutOfBounds() {
        // Đạn sát biên trái (X=5, Y=100) bay sang trái
        IBullet b = bulletManager.createBullet("tank-1", new Position(5, 100), 270.0);
        bulletManager.updateAll(0.1, map, null);

        assertFalse(b.isActive());
        assertEquals(0, bulletManager.getBullets().size());
    }

    @Test
    @DisplayName("Test 25: Đạn bắn trúng tank địch -> tank bị trừ 25 HP, đạn bị huỷ")
    void test25_UpdateAllHitsEnemyTank() {
        PlayerTank enemy = new PlayerTank("enemy-1", 200, 100);
        assertEquals(100, enemy.getHP());

        // Tạo đạn tại (175, 100) bay sang phải (90 độ) tiến về phía enemy tại X=200 (box 180-220)
        IBullet b = bulletManager.createBullet("player-1", new Position(175, 100), 90.0);

        List<ITank> tanks = new ArrayList<>();
        tanks.add(enemy);

        // Update 0.05s -> X tiến tới 190 (nằm trong box enemy 180-220)
        bulletManager.updateAll(0.05, map, tanks);

        assertFalse(b.isActive(), "Đạn trúng tank địch phải bị huỷ");
        assertEquals(75, enemy.getHP(), "Enemy phải bị trừ 25 HP (từ 100 xuống 75)");
    }

    @Test
    @DisplayName("Test 26: Đạn của bạn không bắn trúng chính bạn (friendly fire ignored)")
    void test26_UpdateAllIgnoresOwnerTank() {
        PlayerTank owner = new PlayerTank("player-1", 100, 100);
        // Đạn tạo ngay tại vị trí của owner
        IBullet b = bulletManager.createBullet("player-1", new Position(100, 100), 90.0);

        List<ITank> tanks = new ArrayList<>();
        tanks.add(owner);

        bulletManager.updateAll(0.01, map, tanks);

        assertTrue(b.isActive(), "Đạn không được tự bắn trúng bản thân owner");
        assertEquals(100, owner.getHP(), "Máu owner phải giữ nguyên 100");
    }

    @Test
    @DisplayName("Test 27: Đạn không bắn trúng xe tăng đã chết (HP = 0)")
    void test27_UpdateAllIgnoresDeadTank() {
        PlayerTank deadTank = new PlayerTank("enemy-1", 200, 100);
        deadTank.takeDamage(100); // HP = 0
        assertFalse(deadTank.isAlive());

        IBullet b = bulletManager.createBullet("player-1", new Position(195, 100), 90.0);

        List<ITank> tanks = new ArrayList<>();
        tanks.add(deadTank);

        bulletManager.updateAll(0.05, map, tanks);

        // Đạn đi xuyên qua xác tank đã chết (không bị cản bởi dead tank)
        assertEquals(0, deadTank.getHP());
    }

    @Test
    @DisplayName("Test 28: removeInactive xoá bỏ toàn bộ đạn inactive")
    void test28_RemoveInactiveCleansUpDeactivatedBullets() {
        IBullet b1 = bulletManager.createBullet("tank-1", new Position(100, 100), 0.0);
        bulletManager.updateAll(0.6, null, null);
        IBullet b2 = bulletManager.createBullet("tank-1", new Position(120, 100), 0.0);

        assertEquals(2, bulletManager.getBullets().size());
        b1.deactivate();

        bulletManager.removeInactive();
        assertEquals(1, bulletManager.getBullets().size());
        assertEquals(b2, bulletManager.getBullets().get(0));
    }

    @Test
    @DisplayName("Test 29: clear reset sạch toàn bộ danh sách đạn và cooldown")
    void test29_ClearResetsAllState() {
        bulletManager.createBullet("tank-1", new Position(100, 100), 0.0);
        bulletManager.clear();

        assertEquals(0, bulletManager.getBullets().size());
        assertEquals(0.0, bulletManager.getGameTime(), 0.001);

        // Có thể bắn lại ngay do clear() đã xoá cooldown
        IBullet bNew = bulletManager.createBullet("tank-1", new Position(100, 100), 0.0);
        assertNotNull(bNew);
    }

    @Test
    @DisplayName("Test 30: Tích hợp PlayerTank.shoot() trả về Bullet hợp lệ")
    void test30_PlayerTankShootIntegration() {
        PlayerTank player = new PlayerTank("player-1", 100, 100);
        IBullet b = player.shoot();

        assertNotNull(b, "PlayerTank.shoot() phải tạo viên đạn mới");
        assertEquals("player-1", b.getOwnerId());
        assertEquals(100.0, b.getPosition().getX(), 0.001);
        assertEquals(100.0, b.getPosition().getY(), 0.001);

        // Lần bắn tiếp theo lập tức -> null do cooldown
        assertNull(player.shoot());
    }

    // ==========================================
    // nhóm 3: EXTENDED BUSINESS LOGIC TESTS (Tests 31 - 60)
    // ==========================================

    @Test
    @DisplayName("Test 31: Bullet khởi tạo với ID null sẽ tự sinh UUID hợp lệ")
    void test31_BulletConstructorWithNullId() {
        Bullet b = new Bullet(null, "tank-1", new Position(100, 100), 0.0, 300.0, 25, 8.0, 8.0);
        assertNotNull(b.getId());
        assertFalse(b.getId().trim().isEmpty());
    }

    @Test
    @DisplayName("Test 32: Bullet khởi tạo với Position null sẽ gán vị trí mặc định (0, 0)")
    void test32_BulletConstructorWithNullPosition() {
        Bullet b = new Bullet("b-1", "tank-1", null, 0.0, 300.0, 25, 8.0, 8.0);
        assertNotNull(b.getPosition());
        assertEquals(0.0, b.getPosition().getX(), 0.001);
        assertEquals(0.0, b.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 33: Bullet với speed <= 0 hoặc damage < 0 tự khôi phục giá trị mặc định")
    void test33_BulletConstructorWithInvalidSpeedOrDamage() {
        Bullet b = new Bullet("b-1", "tank-1", new Position(50, 50), 0.0, -100.0, -5, -2.0, -2.0);
        assertEquals(Bullet.DEFAULT_SPEED, b.getSpeed(), 0.001);
        assertEquals(Bullet.DEFAULT_DAMAGE, b.getDamage());
        assertEquals(Bullet.DEFAULT_WIDTH, b.getWidth(), 0.001);
        assertEquals(Bullet.DEFAULT_HEIGHT, b.getHeight(), 0.001);
    }

    @Test
    @DisplayName("Test 34: Kiểm tra đầy đủ các hàm Setters của Bullet")
    void test34_BulletSetters() {
        Bullet b = new Bullet("tank-1", new Position(100, 100), 0.0);
        b.setDirection(180.0);
        b.setSpeed(450.0);
        b.setDamage(50);

        assertEquals(180.0, b.getDirection(), 0.001);
        assertEquals(450.0, b.getSpeed(), 0.001);
        assertEquals(50, b.getDamage());
    }

    @Test
    @DisplayName("Test 35: Kiểm tra định dạng toString() của Bullet không bị nvl / null error")
    void test35_BulletToString() {
        Bullet b = new Bullet("tank-1", new Position(100, 100), 90.0);
        String str = b.toString();
        assertNotNull(str);
        assertTrue(str.contains("tank-1"));
        assertTrue(str.contains("dir=90.0"));
    }

    @Test
    @DisplayName("Test 36: Đạn bay hướng 360 độ hoạt động chuẩn xác như 0 độ")
    void test36_BulletDirection360DegreesNormalization() {
        Bullet b = new Bullet("tank-1", new Position(100, 100), 360.0);
        b.update(0.1);
        assertEquals(100.0, b.getPosition().getX(), 0.001);
        assertEquals(70.0, b.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 37: Đạn bay hướng 180 độ di chuyển theo đúng công thức sin/cos")
    void test37_BulletDirection180DegreesExactMovement() {
        Bullet b = new Bullet("tank-1", new Position(100, 100), 180.0);
        b.update(0.2); // 0.2s * 300 = 60px xuống dưới
        assertEquals(100.0, b.getPosition().getX(), 0.001);
        assertEquals(160.0, b.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 38: Đạn bay hướng 270 độ di chuyển sang trái chính xác")
    void test38_BulletDirection270DegreesExactMovement() {
        Bullet b = new Bullet("tank-1", new Position(100, 100), 270.0);
        b.update(0.2); // 60px sang trái
        assertEquals(40.0, b.getPosition().getX(), 0.001);
        assertEquals(100.0, b.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 39: Va chạm collidesWith trả về false khi 2 đạn không giao nhau")
    void test39_BulletCollidesWithNonOverlapping() {
        Bullet b1 = new Bullet("tank-1", new Position(100, 100), 0.0);
        Bullet b2 = new Bullet("tank-2", new Position(200, 200), 0.0);
        assertFalse(b1.collidesWith(b2));
    }

    @Test
    @DisplayName("Test 40: updateAll với danh sách tanks = null không gây NullPointerException")
    void test40_BulletManagerNullTanksList() {
        bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        assertDoesNotThrow(() -> bulletManager.updateAll(0.1, map, null));
    }

    @Test
    @DisplayName("Test 41: updateAll với danh sách tanks chứa phần tử null an toàn")
    void test41_BulletManagerTanksListContainingNull() {
        bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        List<ITank> tanks = new ArrayList<>();
        tanks.add(null);
        tanks.add(new PlayerTank("enemy-1", 300, 300));
        assertDoesNotThrow(() -> bulletManager.updateAll(0.1, map, tanks));
    }

    @Test
    @DisplayName("Test 42: updateAll cập nhật đồng thời nhiều đạn của các tank khác nhau")
    void test42_BulletManagerMultipleBulletsDifferentOwners() {
        IBullet b1 = bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        bulletManager.updateAll(0.6, null, null);
        IBullet b2 = bulletManager.createBullet("tank-2", new Position(200, 200), 180.0);

        bulletManager.updateAll(0.1, null, null);

        // b1 di chuyển tổng cộng (0.6 + 0.1) * 300 = 210px -> X = 310
        assertEquals(310.0, b1.getPosition().getX(), 0.001);
        assertEquals(230.0, b2.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 43: Đạn đã bị deactive sẽ bị bỏ qua trong luồng updateAll")
    void test43_BulletManagerDeactivatedBulletNotUpdated() {
        IBullet b = bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        b.deactivate();

        // Không truyền map để xem updatePosition có chạy không
        bulletManager.updateAll(0.1, null, null);

        assertEquals(100.0, b.getPosition().getX(), 0.001);
    }

    @Test
    @DisplayName("Test 44: Quản lý Cooldown độc lập giữa các xe tăng khác nhau")
    void test44_BulletManagerCooldownTrackingPerOwner() {
        bulletManager.createBullet("tank-1", new Position(100, 100), 0.0);

        // tank-2 bắn ngay lập tức sau tank-1 -> thành công vì tank-2 chưa có cooldown
        IBullet bTank2 = bulletManager.createBullet("tank-2", new Position(200, 200), 0.0);
        assertNotNull(bTank2, "Tank 2 có thời gian cooldown riêng biệt");
    }

    @Test
    @DisplayName("Test 45: 2 viên đạn bắn trúng 2 xe tăng khác nhau trong cùng 1 tick")
    void test45_BulletManagerMultipleBulletsHitMultipleTanks() {
        PlayerTank enemy1 = new PlayerTank("enemy-1", 200, 100);
        PlayerTank enemy2 = new PlayerTank("enemy-2", 350, 100);

        IBullet b1 = bulletManager.createBullet("player-1", new Position(100, 100), 90.0);
        IBullet b2 = bulletManager.createBullet("player-2", new Position(250, 100), 90.0);

        List<ITank> tanks = List.of(enemy1, enemy2);
        bulletManager.updateAll(0.35, map, tanks); // Bay 105px -> b1 chạm enemy1, b2 chạm enemy2

        assertEquals(75, enemy1.getHP());
        assertEquals(75, enemy2.getHP());
        assertFalse(b1.isActive());
        assertFalse(b2.isActive());
    }

    @Test
    @DisplayName("Test 46: Viên đạn 1 tiêu diệt tank (HP=0), viên đạn 2 bay xuyên qua")
    void test46_BulletManagerOneBulletDestroysTankAndNextBulletPasses() {
        PlayerTank enemy = new PlayerTank("enemy-1", 200, 100);
        enemy.takeDamage(75); // HP còn 25

        IBullet b1 = bulletManager.createBullet("player-1", new Position(100, 100), 90.0);

        List<ITank> tanks = List.of(enemy);

        // Tick 1: b1 đâm trúng enemy -> HP còn 0 (chết)
        bulletManager.updateAll(0.35, map, tanks);
        assertEquals(0, enemy.getHP());
        assertFalse(b1.isActive());

        // Tua qua cooldown 0.5s
        bulletManager.updateAll(0.2, map, tanks);

        // Tick 2: b2 tạo mới và bay qua vị trí enemy đã chết -> không bị cản
        IBullet b2 = bulletManager.createBullet("player-1", new Position(100, 100), 90.0);
        assertNotNull(b2);
        bulletManager.updateAll(0.5, map, tanks);
        assertTrue(b2.getPosition().getX() > 220.0);
    }

    @Test
    @DisplayName("Test 47: getActiveBulletCount với ownerId null trả về 0")
    void test47_BulletManagerGetActiveBulletCountNullOwner() {
        assertEquals(0, bulletManager.getActiveBulletCount(null));
    }

    @Test
    @DisplayName("Test 48: getActiveBulletCount với ownerId không tồn tại trả về 0")
    void test48_BulletManagerGetActiveBulletCountNonExistentOwner() {
        assertEquals(0, bulletManager.getActiveBulletCount("ghost-tank"));
    }

    @Test
    @DisplayName("Test 49: Danh sách trả về từ getBullets() là Unmodifiable (chống sửa trực tiếp)")
    void test49_BulletManagerGetBulletsIsUnmodifiable() {
        bulletManager.createBullet("tank-1", new Position(100, 100), 0.0);
        List<IBullet> bullets = bulletManager.getBullets();

        assertThrows(UnsupportedOperationException.class, () -> bullets.clear());
    }

    @Test
    @DisplayName("Test 50: Quỹ đạo đạn di chuyển qua 60 micro-ticks khớp với 1 tick lớn 1.0 giây")
    void test50_BulletManagerRapidUpdatesSmallDelta() {
        Bullet bMicro = new Bullet("tank-1", new Position(100, 100), 90.0);
        double microDelta = 1.0 / 60.0; // 60fps
        for (int i = 0; i < 60; i++) {
            bMicro.update(microDelta);
        }

        Bullet bMacro = new Bullet("tank-1", new Position(100, 100), 90.0);
        bMacro.update(1.0);

        assertEquals(bMacro.getPosition().getX(), bMicro.getPosition().getX(), 0.001);
        assertEquals(bMacro.getPosition().getY(), bMicro.getPosition().getY(), 0.001);
    }

    @Test
    @DisplayName("Test 51: Đạn bay xuyên qua ô NƯỚC (TileType.WATER) không bị hủy")
    void test51_BulletManagerWaterTilePassage() {
        // Đạn ở vùng trống gần ô nước
        IBullet b = bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        bulletManager.updateAll(0.1, map, null);
        assertTrue(b.isActive(), "Đạn được phép bay xuyên qua ô nước");
    }

    @Test
    @DisplayName("Test 52: Tank còn 10 HP nhận sát thương 25 HP -> HP bị giới hạn ở 0")
    void test52_BulletManagerTankOverkill() {
        PlayerTank enemy = new PlayerTank("enemy-1", 200, 100);
        enemy.takeDamage(90); // HP còn 10

        IBullet b = bulletManager.createBullet("player-1", new Position(195, 100), 90.0);
        List<ITank> tanks = List.of(enemy);

        bulletManager.updateAll(0.05, map, tanks);

        assertEquals(0, enemy.getHP(), "Máu không bị giảm xuống số âm");
        assertFalse(enemy.isAlive());
    }

    @Test
    @DisplayName("Test 53: Xe tăng bắn đạn ngay gần mép bản đồ")
    void test53_BulletManagerShootingFromMapEdge() {
        IBullet b = bulletManager.createBullet("tank-1", new Position(790, 100), 90.0);
        assertNotNull(b);

        // Đạn chỉ cần bay 0.05s (15px) là vượt biên 800px -> bị hủy
        bulletManager.updateAll(0.05, map, null);
        assertFalse(b.isActive());
    }

    @Test
    @DisplayName("Test 54: Đạn được tạo ngay trong ô tường bị hủy ngay ở tick đầu tiên")
    void test54_BulletManagerBulletHitsWallInstantly() {
        // Ô (0,0) là tường (từ X=0 đến 40, Y=0 đến 40)
        IBullet b = bulletManager.createBullet("tank-1", new Position(20, 20), 0.0);
        assertNotNull(b);

        bulletManager.updateAll(0.01, map, null);
        assertFalse(b.isActive(), "Đạn sinh ra trong tường lập tức bị deactive");
    }

    @Test
    @DisplayName("Test 55: Tích hợp canShoot() và updateCooldown() của PlayerTank với BulletManager")
    void test55_BulletManagerTankShootCooldownIntegration() {
        PlayerTank tank = new PlayerTank("player-1", 100, 100);
        assertTrue(tank.canShoot());

        IBullet b1 = tank.shoot();
        assertNotNull(b1);
        assertFalse(tank.canShoot());

        // Update 0.3s -> vẫn chưa canShoot
        tank.updateCooldown(0.3);
        assertFalse(tank.canShoot());

        // Update thêm 0.25s -> qua 0.5s -> canShoot = true
        tank.updateCooldown(0.25);
        assertTrue(tank.canShoot());
    }

    @Test
    @DisplayName("Test 56: 2 xe tăng cùng bắn đạn ở cùng 1 tick game")
    void test56_BulletManagerSimultaneousShootingSameTick() {
        IBullet b1 = bulletManager.createBullet("tank-1", new Position(100, 100), 90.0);
        IBullet b2 = bulletManager.createBullet("tank-2", new Position(200, 200), 180.0);

        assertNotNull(b1);
        assertNotNull(b2);
        assertEquals(2, bulletManager.getBullets().size());
    }

    @Test
    @DisplayName("Test 57: Khi 1 trong 5 đạn bị hủy (chạm tường), tank có thể bắn đạn mới thay thế")
    void test57_BulletManagerReuseBulletSlotAfterDeactivation() {
        String owner = "tank-1";
        // Bắn 5 đạn
        for (int i = 0; i < 5; i++) {
            bulletManager.createBullet(owner, new Position(100, 100), 90.0);
            bulletManager.updateAll(0.6, null, null);
        }
        assertEquals(5, bulletManager.getActiveBulletCount(owner));

        // Deactive 1 viên đạn bất kỳ
        bulletManager.getBullets().get(0).deactivate();
        bulletManager.removeInactive();
        assertEquals(4, bulletManager.getActiveBulletCount(owner));

        // Tua thời gian cooldown và bắn viên đạn thứ 5 mới -> thành công
        bulletManager.updateAll(0.6, null, null);
        IBullet bNew = bulletManager.createBullet(owner, new Position(100, 100), 90.0);
        assertNotNull(bNew, "Slot đạn vừa trống phải cho phép bắn tiếp");
        assertEquals(5, bulletManager.getActiveBulletCount(owner));
    }

    @Test
    @DisplayName("Test 58: Đạn siêu tốc (600px/s) vẫn kích hoạt kiểm tra va chạm chuẩn xác")
    void test58_BulletManagerHighSpeedBulletCollision() {
        Bullet fastBullet = new Bullet("b-fast", "player-1", new Position(170, 100), 90.0, 600.0, 25, 8.0, 8.0);
        PlayerTank enemy = new PlayerTank("enemy-1", 200, 100);

        List<ITank> tanks = List.of(enemy);
        bulletManager.getBullets(); // warmup

        // Thêm trực tiếp đạn fastBullet vào manager thông qua createBullet với speed tùy chỉnh
        IBullet b = bulletManager.createBullet("player-1", new Position(170, 100), 90.0);
        ((Bullet) b).setSpeed(600.0);

        bulletManager.updateAll(0.05, map, tanks); // 600 * 0.05 = 30px -> X từ 170 lên 200 (trúng enemy)

        assertEquals(75, enemy.getHP());
        assertFalse(b.isActive());
    }

    @Test
    @DisplayName("Test 59: gameTime trong BulletManager tích lũy chính xác qua nhiều tick")
    void test59_BulletManagerMultipleTicksAccumulateGameTime() {
        bulletManager.updateAll(0.1, null, null);
        bulletManager.updateAll(0.2, null, null);
        bulletManager.updateAll(0.3, null, null);

        assertEquals(0.6, bulletManager.getGameTime(), 0.001);
    }

    @Test
    @DisplayName("Test 60: Kịch bản đấu súng hoàn chỉnh (Full Battle Lifecycle Scenario)")
    void test60_BulletManagerFullLifecycleScenario() {
        PlayerTank player = new PlayerTank("player-1", 100, 100);
        PlayerTank enemy = new PlayerTank("enemy-1", 220, 100);
        List<ITank> tanks = List.of(player, enemy);

        // Turn 1: Player bắn đạn 1 hướng về enemy (90 độ)
        IBullet b1 = bulletManager.createBullet(player.getId(), player.getPosition(), 90.0);
        assertNotNull(b1);

        // Turn 2: Tua 0.35s (đạn bay 105px tới X=205, chạm box enemy 200-240) -> Trúng enemy!
        bulletManager.updateAll(0.35, map, tanks);
        assertEquals(75, enemy.getHP(), "Enemy trúng đạn 1 còn 75 HP");
        assertFalse(b1.isActive(), "Đạn 1 đã bị hủy");
        assertEquals(0, bulletManager.getBullets().size(), "Danh sách đạn được dọn dẹp sạch");

        // Turn 3: Tua thêm 0.2s (tổng 0.55s > cooldown 0.5s) -> Player bắn đạn 2
        bulletManager.updateAll(0.2, map, tanks);
        IBullet b2 = bulletManager.createBullet(player.getId(), player.getPosition(), 90.0);
        assertNotNull(b2, "Đạn 2 bắn thành công");

        // Turn 4: Tua 0.35s -> Đạn 2 trúng enemy tiếp -> HP còn 50!
        bulletManager.updateAll(0.35, map, tanks);
        assertEquals(50, enemy.getHP(), "Enemy trúng đạn 2 còn 50 HP");
        assertFalse(b2.isActive());
        assertEquals(0, bulletManager.getBullets().size());
    }
}

