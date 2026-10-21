package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.collision.Collision;
import com.tankzors.client.gameplay.WeaponProfile;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IMap;

/**
 * Xe tăng người chơi. Parity server entity.PlayerTank.
 * Speed 60, HP 5 (reference build), 40x40, cooldown 0.5s. Người chơi chỉ đi theo bốn
 * hướng chính; mỗi lần bấm đi một ô, giữ phím tiếp tục sang ô kế tiếp.
 * Substep ≤5px, wall-slide qua Collision.resolveCollision.
 */
public class PlayerTank extends Tank {

    public static final double DEFAULT_SPEED = 60.0;
    public static final int DEFAULT_HP = 5;
    public static final int DEFAULT_MAX_HP = 5;
    public static final double DEFAULT_WIDTH = 40.0;
    public static final double DEFAULT_HEIGHT = 40.0;

    private double shootCooldown = 0.5;
    private double timeSinceLastShot = 0.5; // được bắn ngay tick đầu

    /** Giữ tối đa một bước chờ; đổi hướng tại tâm ô, không thêm thời gian chờ. */
    private Direction heldDirection;
    private Direction requestedDirection;
    private Direction movingDirection;
    private boolean reverseHeld;
    private boolean requestedReverse;
    private double remainingTileDistance;
    private double tileTargetX;
    private double tileTargetY;

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
    public void revive(double x, double y) {
        position.setX(x);
        position.setY(y);
        hp = maxHp;
        clearShield();
    }

    public void setBulletIdSource(BulletIdSource source) { this.bulletIdSource = source; }

    public void setMoving(Direction direction, boolean moving) {
        if (direction == null) return;
        if (moving) {
            heldDirection = direction;
            requestedDirection = direction;
        } else if (direction == heldDirection) {
            heldDirection = null;
        }
    }

    public void stopMoving() {
        // Nhả hết phím: hoàn tất bước đã bấm, nhưng không tự lặp thêm bước.
        heldDirection = null;
        reverseHeld = false;
    }

    /** Giữ X để lùi theo hướng ngược đầu xe mà không xoay sprite. */
    public void setReversing(boolean reversing) {
        reverseHeld = reversing;
        if (reversing) requestedReverse = true;
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
        updateMovement(dt, map, null);
    }

    /** {@code blocker} chặn ô đang có xe khác đứng hoặc giữ chỗ (không đi xuyên xe). */
    public void updateMovement(double dt, IMap map, TileBlocker blocker) {
        if (!isAlive() || !Double.isFinite(dt) || dt <= 0 || map == null) return;
        double timeRemaining = dt;
        while (timeRemaining > 1e-9) {
            if (movingDirection == null && !beginTileMovement(map, blocker)) return;
            double dx = movingDirection == Direction.RIGHT ? 1 : movingDirection == Direction.LEFT ? -1 : 0;
            double dy = movingDirection == Direction.DOWN ? 1 : movingDirection == Direction.UP ? -1 : 0;
            // Dùng thời gian còn lại ở ranh giới ô để không khựng/mất tốc độ.
            double moveSpeed = speed * map.speedFactorAt(position.getX(), position.getY());
            if (!Double.isFinite(moveSpeed) || moveSpeed <= 0) return;
            double step = Math.min(5, Math.min(moveSpeed * timeRemaining, remainingTileDistance));
            Position target = new Position(position.getX() + dx * step, position.getY() + dy * step);
            Position valid = Collision.resolveCollision(this, target, map);
            if (valid.getX() == position.getX() && valid.getY() == position.getY()) {
                movingDirection = null;
                remainingTileDistance = 0;
                return;
            }
            position.setX(valid.getX()); position.setY(valid.getY());
            timeRemaining -= step / moveSpeed;
            remainingTileDistance -= step;
            if (remainingTileDistance <= 1e-9) {
                position.setX(tileTargetX);
                position.setY(tileTargetY);
                movingDirection = null;
                remainingTileDistance = 0;
            }
        }
    }

    private boolean beginTileMovement(IMap map, TileBlocker blocker) {
        Direction next = requestedDirection != null ? requestedDirection : heldDirection;
        requestedDirection = null;
        if (next != null) {
            rotate(rotationFor(next));
        } else if (requestedReverse || reverseHeld) {
            next = opposite(directionForRotation(rotation));
        }
        requestedReverse = false;
        if (next == null) return false;

        double dx = next == Direction.RIGHT ? 1 : next == Direction.LEFT ? -1 : 0;
        double dy = next == Direction.DOWN ? 1 : next == Direction.UP ? -1 : 0;
        tileTargetX = position.getX() + dx * map.getTileSize();
        tileTargetY = position.getY() + dy * map.getTileSize();
        // Ô bị chặn: đứng ở tâm ô hiện tại thay vì dừng lệch lưới sát tường.
        if (Collision.checkTankWallCollision(this, map, new Position(tileTargetX, tileTargetY))) return false;
        int ts = map.getTileSize();
        if (blocker != null && blocker.isBlocked(this,
                (int) Math.floor(tileTargetX / ts), (int) Math.floor(tileTargetY / ts))) return false;
        movingDirection = next;
        remainingTileDistance = map.getTileSize();
        return true;
    }

    /** Ô xe đang chiếm hoặc đã giữ chỗ để đi tới; xe khác không được vào. */
    public boolean occupies(int col, int row, int tileSize) {
        if (!isAlive() || tileSize <= 0) return false;
        Rectangle cell = new Rectangle(col * tileSize + 1, row * tileSize + 1, tileSize - 2, tileSize - 2);
        if (getBoundingBox().intersects(cell)) return true;
        return movingDirection != null
                && col == (int) Math.floor(tileTargetX / tileSize)
                && row == (int) Math.floor(tileTargetY / tileSize);
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

        WeaponProfile weapon = WeaponProfile.fromItemId(activeWeaponId);
        double direction = cardinalRotation(rotation);
        // Builds uses a 4x8 projectile hitbox, rotated on horizontal shots.
        boolean horizontal = direction == 90 || direction == 270;
        double width = horizontal ? 8 : 4;
        double height = horizontal ? 4 : 8;
        return new Bullet(bulletId, id, new Position(position.getX(), position.getY()), direction,
                weapon.speedPixelsPerSecond(), weapon.getDamage(), width, height, weapon.getItemId());
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

    /** Kiểm tra ô đích có bị xe khác chiếm không. */
    public interface TileBlocker {
        boolean isBlocked(PlayerTank mover, int col, int row);
    }

    /** Nguồn id deterministic cho bullet — thay UUID.randomUUID (GWT không emulate). */
    public interface BulletIdSource {
        String nextId();
    }
}
