package com.tankzors.client.gameplay.entity;

/**
 * AABB dùng cho kiểm tra va chạm.
 * intersects(): strict overlap (chép cạnh KHÔNG tính) — parity server.
 */
public class Rectangle {
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public Rectangle(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    /** @return true nếu 2 hình giao nhau (chép cạnh không tính). */
    public boolean intersects(Rectangle other) {
        return this.x < other.x + other.width
                && this.x + this.width > other.x
                && this.y < other.y + other.height
                && this.y + this.height > other.y;
    }

    @Override public String toString() {
        return "Rectangle{x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "}";
    }
}
