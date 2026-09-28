package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IMap;

/**
 * Xe tăng người chơi. Parity server entity.PlayerTank.
 * Speed 60, HP 100, 40x40, cooldown 0.5s. Người chơi chỉ đi theo bốn
 * hướng chính; phím được nhấn gần nhất xác định hướng hiện hành.
 * Substep ≤5px, wall-slide qua Collision.resolveCollision.
 */
public class PlayerTank extends Tank {

    public static final double DEFAULT_SPEED = 60.0;
    public static final int DEFAULT_HP = 100;
    public static final int DEFAULT_MAX_HP = 100;
    public static final double DEFAULT_WIDTH = 40.0;
    public static final double DEFAULT_HEIGHT = 40.0;
    /** Phân biệt một cú bấm đổi hướng với thao tác chủ ý giữ để chạy. */
    static final double TURN_HOLD_DELAY_SECONDS = 0.20;

    private double shootCooldown = 0.5;
    private double timeSinceLastShot = 0.5; // được bắn ngay tick đầu

    /** Hướng đang được giữ, bước kế tiếp và bước đang chạy; mỗi bước đúng một ô map. */
    private Direction heldDirection;
    private Direction requestedDirection;
    private Direction movingDirection;
    private Direction turnHoldDirection;
    private double turnHoldElapsed;
    private boolean reverseHeld;
    private boolean requestedReverse;
    private double remainingTileDistance;

    /** Mỗi match cấp cho PlayerTank một idGenerator để tạo bullet id deterministic (thay UUID). */
    private transient BulletIdSource bulletIdSource;

    public PlayerTank(String id, double x, double y) {
        super(id, x, y, DEFAULT_SPEED, DEFAULT_HP, DEFAULT_MAX_HP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public PlayerTank(String id, double x, double y, double speed) {
        super(id, x, y, speed, DEFAULT_HP, DEFAULT_MAX_HP, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public PlayerTank(String id, double x, double y, double speed, int hp, int maxHp) {
        super(id, x, y, speed, hp, maxHp, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void setHp(int hp) { this.hp = hp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public void setBulletIdSource(BulletIdSource source) { this.bulletIdSource = source; }

    public void setMoving(Direction direction, boolean moving) {
        if (direction == null) return;
        if (moving) {
            heldDirection = direction;
            // Nếu hai phím đến trong cùng một frame, lấy phím nhấn sau cùng.
            // Khi đang hoàn tất một ô, bỏ qua input mới để không cắt ngang bước.
            if (movingDirection == null) requestedDirection = direction;
        } else if (direction == heldDirection) {
            heldDirection = null;
            if (direction == turnHoldDirection) {
                turnHoldDirection = null;
                turnHoldElapsed = 0;
            }
        }
    }

    public void stopMoving() {
        // Không hủy bước đã nhận: xe hoàn tất ô đang đi rồi dừng.
        heldDirection = null;
        requestedDirection = null;
        turnHoldDirection = null;
        turnHoldElapsed = 0;
        reverseHeld = false;
        requestedReverse = false;
    }

    /** Giữ X để lùi theo hướng ngược đầu xe mà không xoay sprite. */
    public void setReversing(boolean reversing) {
        reverseHeld = reversing;
        if (reversing && movingDirection == null) requestedReverse = true;
        if (!reversing) requestedReverse = false;
    }

    private static double rotationFor(Direction direction) {
        switch (direction) {
            case UP: return 0;
            case RIGHT: return 90;
            case DOWN: return 180;
            case LEFT: return 270;
            default: throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    public void updateMovement(double dt, IMap map) {
        if (!isAlive() || !Double.isFinite(dt) || dt <= 0 || map == null) return;
        if (movingDirection == null && (requestedDirection != null || heldDirection != null
                || requestedReverse || reverseHeld)) {
            if (requestedDirection != null || heldDirection != null) {
                Direction nextDirection = requestedDirection != null ? requestedDirection : heldDirection;
                requestedDirection = null;
                double nextRotation = rotationFor(nextDirection);
                if (cardinalRotation(rotation) != nextRotation) {
                    // Một cú bấm hướng mới chỉ xoay tại chỗ. Chỉ khi phím vẫn
                    // được giữ đủ lâu mới coi là ý định chạy liên tục.
                    rotate(nextRotation);
                    turnHoldDirection = heldDirection == nextDirection ? nextDirection : null;
                    turnHoldElapsed = 0;
                    return;
                }
                if (turnHoldDirection == nextDirection) {
                    if (heldDirection != nextDirection) {
                        turnHoldDirection = null;
                        turnHoldElapsed = 0;
                        return;
                    }
                    turnHoldElapsed += dt;
                    if (turnHoldElapsed + 1e-9 < TURN_HOLD_DELAY_SECONDS) return;
                    turnHoldDirection = null;
                    turnHoldElapsed = 0;
                }
                movingDirection = nextDirection;
            } else {
                movingDirection = opposite(directionForRotation(rotation));
                requestedReverse = false;
            }
            remainingTileDistance = map.getTileSize();
        }
        if (movingDirection == null) return;
        double dx;
        double dy;
        switch (movingDirection) {
            case UP: dx = 0; dy = -1; break;
            case DOWN: dx = 0; dy = 1; break;
            case LEFT: dx = -1; dy = 0; break;
            case RIGHT: dx = 1; dy = 0; break;
            default: return;
        }
        double travelBudget = Math.min(speed * dt, remainingTileDistance);
        while (travelBudget > 1e-9) {
            double step = Math.min(5, travelBudget);
            Position target = new Position(position.getX() + dx * step, position.getY() + dy * step);
            Position valid = Collision.resolveCollision(this, target, map);
            if (valid.getX() == position.getX() && valid.getY() == position.getY()) {
                remainingTileDistance = 0;
                break;
            }
            position.setX(valid.getX()); position.setY(valid.getY());
            travelBudget -= step;
            remainingTileDistance -= step;
        }
        if (remainingTileDistance <= 1e-9) {
            movingDirection = null;
        }
    }

    private static Direction directionForRotation(double angle) {
        double cardinal = cardinalRotation(angle);
        if (cardinal == 90) return Direction.RIGHT;
        if (cardinal == 180) return Direction.DOWN;
        if (cardinal == 270) return Direction.LEFT;
        return Direction.UP;
    }

    private static Direction opposite(Direction direction) {
        switch (direction) {
            case UP: return Direction.DOWN;
            case RIGHT: return Direction.LEFT;
            case DOWN: return Direction.UP;
            case LEFT: return Direction.RIGHT;
            default: throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    private int activeWeaponId = 0;

    public int getActiveWeaponId() { return activeWeaponId; }
    public void setActiveWeaponId(int id) { this.activeWeaponId = id; }

    @Override
    public IBullet shoot() {
        if (!isAlive() || timeSinceLastShot < shootCooldown) return null;
        timeSinceLastShot = 0;
        String bulletId = bulletIdSource != null ? bulletIdSource.nextId() : "b-0";

        double speed = Bullet.DEFAULT_SPEED;
        int damage = Bullet.DEFAULT_DAMAGE;
        double width = Bullet.DEFAULT_WIDTH;
        double height = Bullet.DEFAULT_HEIGHT;

        if (activeWeaponId == 1) { // Plasma
            speed = Bullet.DEFAULT_SPEED * 1.35;
            damage = 35;
            width = 10;
            height = 10;
        } else if (activeWeaponId == 2 || activeWeaponId == 3) { // Double Shell / Double Plasma
            speed = Bullet.DEFAULT_SPEED * 1.1;
            damage = 45;
            width = 12;
            height = 12;
        } else if (activeWeaponId == 4 || activeWeaponId == 5) { // Armor Piercing
            speed = Bullet.DEFAULT_SPEED * 1.2;
            damage = 60;
            width = 10;
            height = 10;
        }

        return new Bullet(bulletId, id, new Position(position.getX(), position.getY()), cardinalRotation(rotation),
                speed, damage, width, height);
    }

    /** Làm tròn hướng để đạn người chơi luôn bay thẳng theo một trục. */
    private static double cardinalRotation(double angle) {
        double normalized = ((angle % 360) + 360) % 360;
        return (Math.floor((normalized + 45) / 90) * 90) % 360;
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
