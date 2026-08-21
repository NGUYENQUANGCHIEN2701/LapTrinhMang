package com.tankzors.entity;

/**
 * Vị trí (x, y) trên bản đồ, dùng cho mọi đối tượng trong game.
 */
public class Position {

    private double x;
    private double y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Position{x=" + x + ", y=" + y + "}";
    }
}