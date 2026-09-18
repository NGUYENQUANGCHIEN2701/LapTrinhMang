package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.interfaces.ICollidable;

/** Tường — chặn di chuyển và chặn đạn. Immutable. Parity server. */
public class Wall implements ICollidable {
    private final Position position;
    private final double width;
    private final double height;
    private final Rectangle boundingBox;

    public Wall(double x, double y, double width, double height) {
        this.position = new Position(x, y);
        this.width = width;
        this.height = height;
        this.boundingBox = new Rectangle(x, y, width, height);
    }

    public Wall(Position position, double width, double height) {
        this.position = position != null ? position : new Position(0, 0);
        this.width = width;
        this.height = height;
        this.boundingBox = new Rectangle(this.position.getX(), this.position.getY(), width, height);
    }

    public Position getPosition() { return position; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    @Override public Rectangle getBoundingBox() { return boundingBox; }

    @Override
    public boolean collidesWith(ICollidable other) {
        if (other == null || other.getBoundingBox() == null) return false;
        return this.boundingBox.intersects(other.getBoundingBox());
    }

    @Override public String toString() {
        return "Wall{position=" + position + ", width=" + width + ", height=" + height + "}";
    }
}
