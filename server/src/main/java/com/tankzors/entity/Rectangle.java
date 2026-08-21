package com.tankzors.entity;

/**
 * Hình chữ nhật dùng cho kiểm tra va chạm AABB
 * (Axis-Aligned Bounding Box).
 */
public class Rectangle {

    private double x;
    private double y;
    private double width;
    private double height;

    public Rectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    /**
     * Kiểm tra va chạm AABB giữa hình chữ nhật này và hình chữ nhật khác.
     *
     * @param other hình chữ nhật cần kiểm tra
     * @return {@code true} nếu 2 hình giao nhau
     */
    public boolean intersects(Rectangle other) {
        return this.x < other.x + other.width
                && this.x + this.width > other.x
                && this.y < other.y + other.height
                && this.y + this.height > other.y;
    }

    @Override
    public String toString() {
        return "Rectangle{x=" + x + ", y=" + y
                + ", width=" + width + ", height=" + height + "}";
    }
}