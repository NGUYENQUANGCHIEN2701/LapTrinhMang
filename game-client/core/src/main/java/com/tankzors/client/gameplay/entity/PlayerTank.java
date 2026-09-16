package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IMap;

import java.util.EnumSet;

/**
 * Xe tăng người chơi. Parity server entity.PlayerTank.
 * Speed 150, HP 100, 40x40, cooldown 0.5s. Diagonal = cardinal (normalized).
 * Substep ≤5px, wall-slide qua Collision.resolveCollision.
 */
public class PlayerTank extends Tank {

    public static final double DEFAULT_SPEED = 150.0;
    public static final int DEFAULT_HP = 100;
    public static final int DEFAULT_MAX_HP = 100;
    public static final double DEFAULT_WIDTH = 40.0;
    public static final double DEFAULT_HEIGHT = 40.0;

    private double shootCooldown = 0.5;
    private double timeSinceLastShot = 0.5; // được bắn ngay tick đầu

    private final EnumSet<Direction> heldDirections = EnumSet.noneOf(Direction.class);

    /** Mỗi match cấp cho PlayerTank một idGenerator để tạo bullet id deterministic (thay UUID). */
    private transient BulletIdSource bulletIdSource;

    public PlayerTank(String id, double x, double y) {
        super(id, x, y, DEFAULT_SPEED, DEFAULT_HP, DEFAULT_MAX_HP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public PlayerTank(String id, double x, double y, double speed) {
        super(id, x, y, speed, DEFAULT_HP, DEFAULT_MAX_HP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void setBulletIdSource(BulletIdSource source) { this.bulletIdSource = source; }

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

    public void updateMovement(double dt, IMap map) {
        if (!isAlive() || !Double.isFinite(dt) || dt <= 0 || map == null) return;
        double dx = horizontalInput(), dy = verticalInput();
        double length = Math.hypot(dx, dy);
        if (length == 0) return;
        dx /= length; dy /= length;
        double remaining = speed * dt;
        while (remaining > 1e-9) {
            double step = Math.min(5, remaining);
            Position target = new Position(position.getX() + dx * step, position.getY() + dy * step);
            Position valid = Collision.resolveCollision(this, target, map);
            if (valid.getX() == position.getX() && valid.getY() == position.getY()) break;
            position.setX(valid.getX()); position.setY(valid.getY());
            remaining -= step;
        }
    }

    @Override
    public IBullet shoot() {
        if (!isAlive() || timeSinceLastShot < shootCooldown) return null;
        timeSinceLastShot = 0;
        String bulletId = bulletIdSource != null ? bulletIdSource.nextId() : "b-0";
        return new Bullet(bulletId, id, new Position(position.getX(), position.getY()), rotation,
                Bullet.DEFAULT_SPEED, Bullet.DEFAULT_DAMAGE, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
    }

    public void updateCooldown(double deltaTime) {
        if (timeSinceLastShot < shootCooldown) timeSinceLastShot += deltaTime;
    }

    public double getShootCooldown() { return shootCooldown; }
    public void setShootCooldown(double cooldown) { this.shootCooldown = cooldown; }
    public boolean canShoot() { return timeSinceLastShot >= shootCooldown; }

    /** Nguồn id deterministic cho bullet — thay UUID.randomUUID (GWT không emulate). */
    public interface BulletIdSource {
        String nextId();
    }
}
