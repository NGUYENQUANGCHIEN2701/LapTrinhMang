package com.tankzors.entity;

import com.tankzors.interfaces.ICollidable;

/**
 * Tường trên bản đồ - chặn di chuyển và chặn đạn.
 *
 * <p>Triển khai {@link ICollidable} để tham gia hệ thống kiểm tra va chạm AABB.</p>
 */
public class Wall implements ICollidable {

    private final Position position;
    private final double width;
    private final double height;
    private final Rectangle boundingBox;

    /**
     * Khởi tạo Wall với toạ độ và kích thước.
     *
     * @param x      toạ độ X góc trên bên trái (pixel)
     * @param y      toạ độ Y góc trên bên trái (pixel)
     * @param width  chiều rộng (pixel)
     * @param height chiều cao (pixel)
     */
    public Wall(double x, double y, double width, double height) {
        this.position = new Position(x, y);
        this.width = width;
        this.height = height;
        this.boundingBox = new Rectangle(x, y, width, height);
    }

    /**
     * Khởi tạo Wall với đối tượng Position.
     */
    public Wall(Position position, double width, double height) {
        this.position = position != null ? position : new Position(0, 0);
        this.width = width;
        this.height = height;
        this.boundingBox = new Rectangle(this.position.getX(), this.position.getY(), width, height);
    }

    public Position getPosition() {
        return position;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    @Override
    public boolean collidesWith(ICollidable other) {
        if (other == null || other.getBoundingBox() == null) {
            return false;
        }
        return this.boundingBox.intersects(other.getBoundingBox());
    }

    @Override
    public String toString() {
        return "Wall{position=" + position + ", width=" + width + ", height=" + height + "}";
    }
}