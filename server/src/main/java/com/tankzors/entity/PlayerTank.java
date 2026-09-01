package com.tankzors.entity;

import com.tankzors.interfaces.IBullet;

/**
 * Xe tăng do người chơi điều khiển.
 *
 * <p>Thông số mặc định:
 * <ul>
 *   <li>Speed: 150 pixels/giây</li>
 *   <li>HP: 100 / MaxHP: 100</li>
 *   <li>Kích thước: 40x40 pixels</li>
 * </ul>
 * </p>
 */
public class PlayerTank extends Tank {

    // ========== CONSTANTS ==========
    public static final double DEFAULT_SPEED = 150.0;  // pixels/giây
    public static final int DEFAULT_HP = 100;
    public static final int DEFAULT_MAX_HP = 100;
    public static final double DEFAULT_WIDTH = 40.0;   // pixels
    public static final double DEFAULT_HEIGHT = 40.0;  // pixels

    // ========== COOLDOWN bắn ==========
    private double shootCooldown = 0.5; // giây giữa 2 lần bắn
    private double timeSinceLastShot = 0.5; // cho phép bắn ngay từ đầu

    // ========== CONSTRUCTORS ==========

    /**
     * Tạo PlayerTank với thông số mặc định tại vị trí (x, y).
     *
     * @param id ID duy nhất (thường là sessionId)
     * @param x  vị trí X ban đầu
     * @param y  vị trí Y ban đầu
     */
    public PlayerTank(String id, double x, double y) {
        super(id, x, y, DEFAULT_SPEED, DEFAULT_HP, DEFAULT_MAX_HP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Tạo PlayerTank với tốc độ tùy chỉnh.
     */
    public PlayerTank(String id, double x, double y, double speed) {
        super(id, x, y, speed, DEFAULT_HP, DEFAULT_MAX_HP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    // ========== SHOOT ==========

    /**
     * Bắn một viên đạn từ vị trí và hướng hiện tại.
     *
     * <p>Trả về null nếu đang trong thời gian hồi (cooldown).
     * Viên đạn sẽ được implement đầy đủ khi Bullet entity hoàn thiện.</p>
     *
     * @return viên đạn mới hoặc null nếu chưa hết cooldown
     */
    @Override
    public IBullet shoot() {
        if (timeSinceLastShot < shootCooldown) {
            return null; // Chưa hết cooldown
        }
        timeSinceLastShot = 0;
        return new Bullet(id, new Position(position.getX(), position.getY()), rotation);
    }

    /**
     * Cập nhật cooldown bắn mỗi tick.
     * Được gọi từ GameLoop.updateEntities().
     *
     * @param deltaTime thời gian tick (giây)
     */
    public void updateCooldown(double deltaTime) {
        if (timeSinceLastShot < shootCooldown) {
            timeSinceLastShot += deltaTime;
        }
    }

    // ========== GETTERS ==========

    public double getShootCooldown() {
        return shootCooldown;
    }

    public void setShootCooldown(double cooldown) {
        this.shootCooldown = cooldown;
    }

    public boolean canShoot() {
        return timeSinceLastShot >= shootCooldown;
    }
}
