package com.tankzors.entity;

import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;

import java.util.UUID;

/**
 * Thực thể viên đạn trong game.
 *
 * <p>Triển khai {@link IBullet} (logic đạn) và {@link ICollidable} (va chạm AABB).
 * Mặc định: tốc độ 300 px/s, sát thương 25 HP, kích thước va chạm 8x8px.</p>
 */
public class Bullet implements IBullet, ICollidable {

    public static final double DEFAULT_SPEED = 300.0; // pixels/giây
    public static final int DEFAULT_DAMAGE = 25;       // HP sát thương
    public static final double DEFAULT_WIDTH = 8.0;    // pixels
    public static final double DEFAULT_HEIGHT = 8.0;   // pixels

    private final String id;
    private final String ownerId;
    private final Position position;
    private double direction; // Góc quay (độ: 0=UP, 90=RIGHT, 180=DOWN, 270=LEFT)
    private double speed;     // pixels/giây
    private int damage;       // Sát thương khi trúng
    private boolean active;   // Trạng thái hoạt động
    private double width;
    private double height;

    /**
     * Tạo viên đạn với thông số mặc định.
     *
     * @param ownerId   ID xe tăng đã bắn viên đạn này
     * @param position  vị trí ban đầu (tâm đạn)
     * @param direction hướng bay (độ)
     */
    public Bullet(String ownerId, Position position, double direction) {
        this(UUID.randomUUID().toString(), ownerId, position, direction, DEFAULT_SPEED, DEFAULT_DAMAGE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Tạo viên đạn với vị trí toạ độ X, Y.
     *
     * @param ownerId   ID xe tăng đã bắn viên đạn này
     * @param x         toạ độ X ban đầu
     * @param y         toạ độ Y ban đầu
     * @param direction hướng bay (độ)
     */
    public Bullet(String ownerId, double x, double y, double direction) {
        this(UUID.randomUUID().toString(), ownerId, new Position(x, y), direction, DEFAULT_SPEED, DEFAULT_DAMAGE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Tạo viên đạn với thông số tuỳ chỉnh.
     *
     * @param ownerId   ID xe tăng đã bắn
     * @param position  vị trí ban đầu
     * @param direction hướng bay (độ)
     * @param speed     tốc độ bay (px/s)
     * @param damage    sát thương
     */
    public Bullet(String ownerId, Position position, double direction, double speed, int damage) {
        this(UUID.randomUUID().toString(), ownerId, position, direction, speed, damage, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Constructor đầy đủ tham số.
     */
    public Bullet(String id, String ownerId, Position position, double direction, double speed, int damage, double width, double height) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.position = position != null ? new Position(position.getX(), position.getY()) : new Position(0, 0);
        this.direction = direction;
        this.speed = speed > 0 ? speed : DEFAULT_SPEED;
        this.damage = damage >= 0 ? damage : DEFAULT_DAMAGE;
        this.active = true;
        this.width = width > 0 ? width : DEFAULT_WIDTH;
        this.height = height > 0 ? height : DEFAULT_HEIGHT;
    }

    /**
     * Cập nhật vị trí viên đạn theo góc hướng bay và thời gian deltaTime.
     *
     * <p>Quy ước góc:
     * <ul>
     *   <li>0° (UP): dx = 0, dy = -1</li>
     *   <li>90° (RIGHT): dx = 1, dy = 0</li>
     *   <li>180° (DOWN): dx = 0, dy = 1</li>
     *   <li>270° (LEFT): dx = -1, dy = 0</li>
     * </ul>
     * </p>
     *
     * @param deltaTime thời gian đã trôi qua kể từ tick trước (giây)
     */
    @Override
    public void update(double deltaTime) {
        if (!active || deltaTime <= 0) {
            return;
        }

        double rad = Math.toRadians(direction);
        double dx = Math.sin(rad);
        double dy = -Math.cos(rad);

        double newX = position.getX() + dx * speed * deltaTime;
        double newY = position.getY() + dy * speed * deltaTime;

        position.setX(newX);
        position.setY(newY);
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void deactivate() {
        this.active = false;
    }

    @Override
    public String getOwnerId() {
        return ownerId;
    }

    public String getId() {
        return id;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    // ========== ICollidable ==========

    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(
                position.getX() - width / 2.0,
                position.getY() - height / 2.0,
                width,
                height
        );
    }

    @Override
    public boolean collidesWith(ICollidable other) {
        if (other == null) {
            return false;
        }
        return this.getBoundingBox().intersects(other.getBoundingBox());
    }

    @Override
    public String toString() {
        return "Bullet{" +
                "id='" + id + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", pos=" + position +
                ", dir=" + direction +
                ", active=" + active +
                '}';
    }
}
