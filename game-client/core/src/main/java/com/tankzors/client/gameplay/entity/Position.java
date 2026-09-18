package com.tankzors.client.gameplay.entity;

/** Vị trí (x, y) pixel trên bản đồ. Port nguyên vẹn từ server entity.Position. */
public class Position {
    private double x;
    private double y;

    public Position(double x, double y) { this.x = x; this.y = y; }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    @Override public String toString() { return "Position{x=" + x + ", y=" + y + "}"; }
}
