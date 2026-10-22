package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.interfaces.ICollidable;
import com.tankzors.client.gameplay.map.TileType;

/** Vật cản theo ô: gạch 4 hit, tường trắng bất tử, bụi cây 1 hit, khối đá chỉ vỡ khi nổ. */
public class Wall implements ICollidable {
    private final Position position;
    private final double width;
    private final double height;
    private final Rectangle boundingBox;
    private final int col;
    private final int row;
    private final TileType tileType;
    private int hitPoints;

    public Wall(double x, double y, double width, double height) {
        this(x, y, width, height, -1, -1, TileType.WALL);
    }

    public Wall(double x, double y, double width, double height,
                int col, int row, TileType tileType) {
        this.position = new Position(x, y);
        this.width = width;
        this.height = height;
        this.boundingBox = new Rectangle(x, y, width, height);
        this.col = col;
        this.row = row;
        this.tileType = tileType != null ? tileType : TileType.WALL;
        this.hitPoints = this.tileType == TileType.BUSH ? 1
                : this.tileType == TileType.WHITE_WALL || this.tileType == TileType.STONE_WALL
                ? Integer.MAX_VALUE : 4;
    }

    public Wall(Position position, double width, double height) {
        this(position != null ? position.getX() : 0, position != null ? position.getY() : 0,
                width, height, -1, -1, TileType.WALL);
    }

    public Position getPosition() { return position; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int getCol() { return col; }
    public int getRow() { return row; }
    public TileType getTileType() { return tileType; }
    public int getHitPoints() { return hitPoints; }
    public boolean isDestructible() {
        return tileType != TileType.WHITE_WALL && tileType != TileType.STONE_WALL;
    }

    /** Áp sát thương thực tế của projectile; @return true khi vật cản bị phá. */
    public boolean hit(int damage) {
        if (!isDestructible() || damage <= 0) return false;
        hitPoints = Math.max(0, hitPoints - damage);
        return hitPoints == 0;
    }

    /** Tương thích cho các tác động môi trường cũ: đúng một damage point. */
    public boolean hit() {
        return hit(1);
    }

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
