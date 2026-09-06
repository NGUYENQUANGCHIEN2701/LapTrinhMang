package com.tankzors.entity;

import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.IMap;
import com.tankzors.engine.CollisionManager;
import java.util.EnumSet;

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

    private final EnumSet<Direction> heldDirections = EnumSet.noneOf(Direction.class);

    public void setMoving(Direction direction, boolean moving) {
        if (direction == null) return;
        if (moving) heldDirections.add(direction); else heldDirections.remove(direction);
        double dx = horizontalInput(), dy = verticalInput();
        if (dx != 0 || dy != 0) rotate((Math.toDegrees(Math.atan2(dy, dx)) + 450) % 360);
    }

    public void stopMoving() { heldDirections.clear(); }

    private int horizontalInput() {
        return (heldDirections.contains(Direction.RIGHT) ? 1 : 0) - (heldDirections.contains(Direction.LEFT) ? 1 : 0);
    }

    private int verticalInput() {
        return (heldDirections.contains(Direction.DOWN) ? 1 : 0) - (heldDirections.contains(Direction.UP) ? 1 : 0);
    }

    /** Held input is simulated here, independent of keyboard repeat/network frequency. */
    public void updateMovement(double dt, IMap map) {
        if (!isAlive() || !Double.isFinite(dt) || dt <= 0 || map == null) return;
        double dx = horizontalInput(), dy = verticalInput();
        double length = Math.hypot(dx, dy);
        if (length == 0) return;
        dx /= length; dy /= length; // Diagonal speed equals cardinal speed.
        double remaining = speed * dt;
        while (remaining > 1e-9) {
            double step = Math.min(5, remaining);
            Position target = new Position(position.getX() + dx * step, position.getY() + dy * step);
            Position valid = CollisionManager.resolveCollision(this, target, map);
            if (valid.getX() == position.getX() && valid.getY() == position.getY()) break;
            position.setX(valid.getX()); position.setY(valid.getY());
            remaining -= step;
        }
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
        if (!isAlive() || timeSinceLastShot < shootCooldown) {
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
