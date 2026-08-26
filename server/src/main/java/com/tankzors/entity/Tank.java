package com.tankzors.entity;

import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;
import com.tankzors.interfaces.ITank;

/**
 * Lớp trừu tượng đại diện cho xe tăng trong game.
 *
 * <p>Implements ITank (hợp đồng logic game) và ICollidable (va chạm AABB).
 * Các lớp con (PlayerTank, AITank) chỉ cần định nghĩa các hằng số
 * (speed, hp, size) và phương thức shoot().</p>
 *
 * <p>Di chuyển dựa trên speed * deltaTime để đảm bảo mượt mà
 * bất kể tick rate thực tế.</p>
 */
public abstract class Tank implements ITank, ICollidable {

    // ========== IDENTITY ==========
    protected final String id;

    // ========== POSITION & ROTATION ==========
    protected final Position position;
    protected double rotation; // 0=UP, 90=RIGHT, 180=DOWN, 270=LEFT

    // ========== STATS ==========
    protected double speed;    // pixels/giây
    protected int hp;
    protected int maxHp;

    // ========== SIZE (dùng cho bounding box) ==========
    protected double width;
    protected double height;

    // ========== CONSTRUCTOR ==========

    /**
     * Tạo xe tăng tại vị trí cho trước.
     *
     * @param id       ID duy nhất
     * @param x        vị trí X ban đầu
     * @param y        vị trí Y ban đầu
     * @param speed    tốc độ di chuyển (pixels/giây)
     * @param hp       máu ban đầu
     * @param maxHp    máu tối đa
     * @param width    chiều rộng (pixels)
     * @param height   chiều cao (pixels)
     */
    protected Tank(String id, double x, double y, double speed, int hp, int maxHp, double width, double height) {
        this.id = id;
        this.position = new Position(x, y);
        this.rotation = 0; // Mặc định hướng lên
        this.speed = speed;
        this.hp = hp;
        this.maxHp = maxHp;
        this.width = width;
        this.height = height;
    }

    // ========== ITank: MOVEMENT ==========

    /**
     * Di chuyển xe tăng theo hướng chỉ định.
     *
     * <p>Công thức: position += speed * deltaTime theo trục tương ứng.
     * Đồng thời xoay tank theo hướng di chuyển:
     * UP=0, RIGHT=90, DOWN=180, LEFT=270.</p>
     *
     * @param direction hướng di chuyển
     * @param deltaTime thời gian tick (giây)
     */
    @Override
    public void move(Direction direction, double deltaTime) {
        double distance = speed * deltaTime;

        switch (direction) {
            case UP:
                position.setY(position.getY() - distance);
                rotation = 0;
                break;
            case DOWN:
                position.setY(position.getY() + distance);
                rotation = 180;
                break;
            case LEFT:
                position.setX(position.getX() - distance);
                rotation = 270;
                break;
            case RIGHT:
                position.setX(position.getX() + distance);
                rotation = 90;
                break;
        }
    }

    // ========== ITank: ROTATION ==========

    @Override
    public void rotate(double angle) {
        this.rotation = angle;
    }

    @Override
    public double getRotation() {
        return rotation;
    }

    // ========== ITank: POSITION ==========

    @Override
    public Position getPosition() {
        return position;
    }

    // ========== ITank: HEALTH ==========

    @Override
    public int getHP() {
        return hp;
    }

    @Override
    public int getMaxHP() {
        return maxHp;
    }

    @Override
    public void takeDamage(int damage) {
        this.hp = Math.max(0, this.hp - damage);
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    // ========== ITank: IDENTITY ==========

    @Override
    public String getId() {
        return id;
    }

    // ========== ITank: SHOOT (abstract - lớp con implement) ==========

    @Override
    public abstract IBullet shoot();

    // ========== ICollidable ==========

    /**
     * Trả về bounding box (AABB) cho va chạm.
     * Tọa độ position là tâm của tank.
     */
    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(
                position.getX() - width / 2,
                position.getY() - height / 2,
                width,
                height
        );
    }

    @Override
    public boolean collidesWith(ICollidable other) {
        return this.getBoundingBox().intersects(other.getBoundingBox());
    }

    // ========== GETTERS bổ sung ==========

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "'"
                + ", pos=" + position
                + ", rotation=" + rotation
                + ", hp=" + hp + "/" + maxHp
                + "}";
    }
}
