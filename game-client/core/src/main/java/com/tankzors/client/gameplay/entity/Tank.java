package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.ICollidable;
import com.tankzors.client.gameplay.interfaces.ITank;

/**
 * Lớp trừu tượng xe tăng. Parity server entity.Tank.
 * position là TÂM tank; rotation: 0=UP,90=RIGHT,180=DOWN,270=LEFT.
 */
public abstract class Tank implements ITank, ICollidable {

    protected final String id;
    protected final Position position;
    protected double rotation;
    protected double speed;
    protected int hp;
    protected int maxHp;
    protected double width;
    protected double height;

    protected Tank(String id, double x, double y, double speed, int hp, int maxHp, double width, double height) {
        this.id = id;
        this.position = new Position(x, y);
        this.rotation = 0;
        this.speed = speed;
        this.hp = hp;
        this.maxHp = maxHp;
        this.width = width;
        this.height = height;
    }

    @Override
    public void move(Direction direction, double deltaTime) {
        double distance = speed * deltaTime;
        switch (direction) {
            case UP: position.setY(position.getY() - distance); rotation = 0; break;
            case DOWN: position.setY(position.getY() + distance); rotation = 180; break;
            case LEFT: position.setX(position.getX() - distance); rotation = 270; break;
            case RIGHT: position.setX(position.getX() + distance); rotation = 90; break;
        }
    }

    @Override public void rotate(double angle) { this.rotation = angle; }
    @Override public double getRotation() { return rotation; }
    @Override public Position getPosition() { return position; }
    @Override public int getHP() { return hp; }
    @Override public int getMaxHP() { return maxHp; }
    @Override public void takeDamage(int damage) { this.hp = Math.max(0, this.hp - damage); }
    @Override public boolean isAlive() { return hp > 0; }
    @Override public String getId() { return id; }
    @Override public abstract IBullet shoot();

    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(position.getX() - width / 2, position.getY() - height / 2, width, height);
    }

    @Override
    public boolean collidesWith(ICollidable other) {
        return this.getBoundingBox().intersects(other.getBoundingBox());
    }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    @Override public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', pos=" + position
                + ", rotation=" + rotation + ", hp=" + hp + "/" + maxHp + "}";
    }
}
