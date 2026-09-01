package com.tankzors.engine;

import com.tankzors.entity.Bullet;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.IMap;
import com.tankzors.interfaces.ITank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý vòng đời tất cả các viên đạn trong game (Bullet Manager).
 *
 * <p>Nhiệm vụ:
 * <ul>
 *   <li>Tạo đạn mới khi tank bắn (kiểm tra cooldown 0.5s và giới hạn tối đa 5 đạn/tank).</li>
 *   <li>Cập nhật vị trí tất cả đạn active mỗi tick.</li>
 *   <li>Kiểm tra va chạm với tường bản đồ và các xe tăng khác.</li>
 *   <li>Xoá bỏ các đạn đã bị deactive.</li>
 * </ul>
 * </p>
 */
public class BulletManager {

    private static final Logger logger = LoggerFactory.getLogger(BulletManager.class);

    public static final int MAX_BULLETS_PER_TANK = 5;
    public static final double SHOOT_COOLDOWN_SECONDS = 0.5;

    // Danh sách đạn đang quản lý (thread-safe)
    private final List<IBullet> bullets = new CopyOnWriteArrayList<>();

    // Map lưu thời điểm bắn gần nhất của mỗi tank (tính theo giây hệ thống hoặc game time)
    private final Map<String, Double> lastShootTimes = new ConcurrentHashMap<>();

    // Map tích lũy game time (giây) phục vụ tính cooldown chuẩn xác
    private double gameTime = 0.0;

    public BulletManager() {
    }

    /**
     * Tạo một viên đạn mới nếu hợp lệ (hết cooldown và số đạn active < 5).
     *
     * @param ownerId   ID xe tăng bắn
     * @param position  vị trí bắn (tâm xe tăng)
     * @param direction hướng bay (độ)
     * @return viên đạn mới khởi tạo, hoặc null nếu không thể bắn
     */
    public IBullet createBullet(String ownerId, Position position, double direction) {
        if (ownerId == null || position == null) {
            return null;
        }

        // 1. Kiểm tra số đạn đang active của tank này
        long activeCount = getActiveBulletCount(ownerId);
        if (activeCount >= MAX_BULLETS_PER_TANK) {
            logger.debug("[BulletManager] Tank {} đã đạt giới hạn tối đa {} đạn!", ownerId, MAX_BULLETS_PER_TANK);
            return null;
        }

        // 2. Kiểm tra cooldown 0.5s giữa 2 lần bắn
        Double lastTime = lastShootTimes.get(ownerId);
        if (lastTime != null && (gameTime - lastTime) < SHOOT_COOLDOWN_SECONDS - 1e-6) {
            logger.debug("[BulletManager] Tank {} chưa hết cooldown (chờ thêm {}s)", ownerId, SHOOT_COOLDOWN_SECONDS - (gameTime - lastTime));
            return null;
        }

        // 3. Tạo viên đạn mới
        IBullet bullet = new Bullet(ownerId, position, direction);
        bullets.add(bullet);
        lastShootTimes.put(ownerId, gameTime);

        logger.debug("💥 [BulletManager] Tank {} bắn đạn mới tại pos={}, dir={}°", ownerId, position, direction);
        return bullet;
    }

    /**
     * Cập nhật vị trí và xử lý va chạm cho tất cả viên đạn đang active.
     *
     * @param deltaTime thời gian trôi qua từ tick trước (giây)
     * @param map       bản đồ game (kiểm tra va chạm tường)
     * @param tanks     danh sách các xe tăng (kiểm tra trúng đạn)
     */
    public void updateAll(double deltaTime, IMap map, List<ITank> tanks) {
        if (deltaTime <= 0) {
            return;
        }

        // Tích lũy thời gian game
        gameTime += deltaTime;

        List<ITank> safeTanks = tanks != null ? tanks : Collections.emptyList();

        for (IBullet bullet : bullets) {
            if (!bullet.isActive()) {
                continue;
            }

            // 1. Cập nhật vị trí đạn
            bullet.update(deltaTime);

            // 2. Kiểm tra va chạm với tường hoặc ngoài biên bản đồ
            if (map != null && CollisionManager.checkBulletWallCollision(bullet, map)) {
                bullet.deactivate();
                logger.debug("🧱 [BulletManager] Đạn {} chạm tường -> bị hủy", bullet.getOwnerId());
                continue;
            }

            // 3. Kiểm tra va chạm với các xe tăng khác
            for (ITank tank : safeTanks) {
                if (tank == null || !tank.isAlive()) {
                    continue;
                }

                if (CollisionManager.checkBulletTankCollision(bullet, tank)) {
                    // Trừ 25 HP của xe tăng bị trúng
                    tank.takeDamage(bullet.getDamage());
                    bullet.deactivate();
                    logger.info("🎯 [BulletManager] Đạn của {} bắn trúng tank {} (HP còn: {}/{})",
                            bullet.getOwnerId(), tank.getId(), tank.getHP(), tank.getMaxHP());
                    break; // Đạn nổ sau khi trúng 1 tank
                }
            }
        }

        // Dọn dẹp đạn hỏng
        removeInactive();
    }

    /**
     * Xoá bỏ tất cả các viên đạn đã bị deactive khỏi danh sách quản lý.
     */
    public void removeInactive() {
        bullets.removeIf(bullet -> !bullet.isActive());
    }

    /**
     * @return danh sách các viên đạn đang active.
     */
    public List<IBullet> getBullets() {
        List<IBullet> activeBullets = new ArrayList<>();
        for (IBullet bullet : bullets) {
            if (bullet.isActive()) {
                activeBullets.add(bullet);
            }
        }
        return Collections.unmodifiableList(activeBullets);
    }

    /**
     * Đếm số lượng đạn đang active của một xe tăng.
     *
     * @param ownerId ID xe tăng
     * @return số đạn active
     */
    public long getActiveBulletCount(String ownerId) {
        if (ownerId == null) return 0;
        return bullets.stream()
                .filter(b -> b.isActive() && ownerId.equals(b.getOwnerId()))
                .count();
    }

    /**
     * Xoá sạch toàn bộ dữ liệu đạn và cooldown (dùng cho reset game / test).
     */
    public void clear() {
        bullets.clear();
        lastShootTimes.clear();
        gameTime = 0.0;
    }

    public double getGameTime() {
        return gameTime;
    }
}
