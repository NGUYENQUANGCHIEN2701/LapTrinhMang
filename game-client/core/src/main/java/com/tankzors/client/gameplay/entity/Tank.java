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
    protected int shieldHp;
    protected int maxShieldHp;
    private double shieldRegenRemaining;
    private int shieldResearchLevel;
    protected double width;
    protected double height;
    /** Vị trí đầu step gần nhất — chỉ dùng để render nội suy giữa hai tick. */
    private double previousX;
    private double previousY;

    protected Tank(String id, double x, double y, double speed, int hp, int maxHp, double width, double height) {
        this.id = id;
        this.position = new Position(x, y);
        this.rotation = 0;
        this.speed = speed;
        this.hp = hp;
        this.maxHp = maxHp;
        this.width = width;
        this.height = height;
        this.previousX = x;
        this.previousY = y;
    }

    /** Gọi đầu mỗi fixed step, trước khi tank di chuyển. */
    public void capturePreviousPosition() {
        previousX = position.getX();
        previousY = position.getY();
    }
    public double getPreviousX() { return previousX; }
    public double getPreviousY() { return previousY; }

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
    @Override public void takeDamage(int damage) {
        if (damage <= 0) return;
        int absorbed = Math.min(shieldHp, damage);
        shieldHp -= absorbed;
        hp = Math.max(0, hp - (damage - absorbed));
    }
    /** Builds doubles blast power before the shield, then halves overflow for mobile hulls. */
    public void takeExplosionDamage(int power, boolean turret) {
        if (power <= 0) return;
        int damage = power * 2;
        int absorbed = Math.min(shieldHp, damage);
        shieldHp -= absorbed;
        int overflow = damage - absorbed;
        hp = Math.max(0, hp - (turret ? overflow : overflow / 2));
    }
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
    public int getShieldHp() { return shieldHp; }
    public int getMaxShieldHp() { return maxShieldHp; }
    /** Workshop armor in Builds increases both current and maximum hull HP. */
    public void addHullHitPoints(int amount) {
        if (amount <= 0) return;
        maxHp += amount;
        hp += amount;
    }
    /** Removes temporary armor after a lost life without affecting permanent armor. */
    public void removeHullHitPoints(int amount) {
        if (amount <= 0) return;
        maxHp = Math.max(1, maxHp - amount);
        hp = Math.min(hp, maxHp);
    }
    public void restoreHull() { hp = maxHp; }
    public void addShieldCapacity(int amount) {
        if (amount <= 0) return;
        maxShieldHp += amount;
        // The reference starts the normal regeneration timer; a newly mounted
        // field is not instantly filled on the very next simulation tick.
        if (shieldHp < maxShieldHp && shieldRegenRemaining <= 0) {
            shieldRegenRemaining = shieldRegenDelaySeconds();
        }
    }
    public void clearShield() { shieldHp = 0; maxShieldHp = 0; shieldRegenRemaining = 0; }

    /** Research affects the next delay calculation, preserving the active Builds timer. */
    public void setShieldResearchLevel(int level) {
        int normalized = Math.max(0, Math.min(4, level));
        if (shieldResearchLevel == normalized) return;
        shieldResearchLevel = normalized;
    }

    /** Force field cadence ported from Builds (its original 60 Hz timer). */
    public void updateShield(double deltaTime) {
        if (maxShieldHp <= shieldHp || deltaTime <= 0) return;
        if (shieldRegenRemaining > 0) {
            shieldRegenRemaining = Math.max(0, shieldRegenRemaining - deltaTime);
            return;
        }
        // Builds calculates the next delay using the deficit BEFORE restoring one HP.
        shieldRegenRemaining = shieldRegenDelaySeconds();
        shieldHp = Math.min(maxShieldHp, shieldHp + 1);
    }

    private double shieldRegenDelaySeconds() {
        double hpBonus;
        switch (shieldResearchLevel) {
            case 2: hpBonus = maxHp / 5.0; break;
            case 3: hpBonus = maxHp / 3.0; break;
            case 4: hpBonus = maxHp / 2.0; break;
            default: hpBonus = 0; break;
        }
        hpBonus = Math.min(hpBonus, Math.max(0, maxHp - hp));
        double denominator = Math.max(1.0, hp + hpBonus);
        double buildFrames = (30.0 + (maxShieldHp - shieldHp) * 25.0) * maxHp / denominator * 3.0;
        // Builds truncates its delay to an integer number of 60 Hz frames.
        return (int) buildFrames / 60.0;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', pos=" + position
                + ", rotation=" + rotation + ", hp=" + hp + "/" + maxHp + "}";
    }
}
