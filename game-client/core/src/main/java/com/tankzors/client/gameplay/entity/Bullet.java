package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.ICollidable;

/**
 * Viên đạn. Parity server entity.Bullet, nhưng:
 * - KHÔNG dùng UUID.randomUUID (GWT không emulate). ID do caller cung cấp.
 * - update(): dx=sin(rad), dy=-cos(rad) (0°=UP).
 * Mặc định: speed 300, damage 25, hitbox 8x8.
 */
public class Bullet implements IBullet, ICollidable {

    public static final double DEFAULT_SPEED = 300.0;
    public static final int DEFAULT_DAMAGE = 25;
    public static final double DEFAULT_WIDTH = 8.0;
    public static final double DEFAULT_HEIGHT = 8.0;

    private final String id;
    private final String ownerId;
    private final Position position;
    private double direction;
    private double speed;
    private int damage;
    private boolean active;
    private double width;
    private double height;

    /** Caller phải cung cấp id deterministic (counter per-match). */
    public Bullet(String id, String ownerId, Position position, double direction, double speed, int damage, double width, double height) {
        this.id = id != null ? id : "b-0";
        this.ownerId = ownerId;
        this.position = position != null ? new Position(position.getX(), position.getY()) : new Position(0, 0);
        this.direction = direction;
        this.speed = speed > 0 ? speed : DEFAULT_SPEED;
        this.damage = damage >= 0 ? damage : DEFAULT_DAMAGE;
        this.active = true;
        this.width = width > 0 ? width : DEFAULT_WIDTH;
        this.height = height > 0 ? height : DEFAULT_HEIGHT;
    }

    @Override
    public void update(double deltaTime) {
        if (!active || deltaTime <= 0) return;
        double rad = Math.toRadians(direction);
        double dx = Math.sin(rad);
        double dy = -Math.cos(rad);
        position.setX(position.getX() + dx * speed * deltaTime);
        position.setY(position.getY() + dy * speed * deltaTime);
    }

    @Override public Position getPosition() { return position; }
    @Override public double getDirection() { return direction; }
    public void setDirection(double direction) { this.direction = direction; }
    @Override public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    @Override public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    @Override public boolean isActive() { return active; }
    @Override public void deactivate() { this.active = false; }
    @Override public String getOwnerId() { return ownerId; }
    public String getId() { return id; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(position.getX() - width / 2.0, position.getY() - height / 2.0, width, height);
    }

    @Override
    public boolean collidesWith(ICollidable other) {
        if (other == null) return false;
        return this.getBoundingBox().intersects(other.getBoundingBox());
    }

    @Override public String toString() {
        return "Bullet{id='" + id + "', ownerId='" + ownerId + "', pos=" + position
                + ", dir=" + direction + ", active=" + active + "}";
    }
}
