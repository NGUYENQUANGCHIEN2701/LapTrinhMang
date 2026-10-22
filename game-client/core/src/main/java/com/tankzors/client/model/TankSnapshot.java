package com.tankzors.client.model;

/** Render snapshot for one tank; co-op WORLD_STATE uses the same fields. */
public final class TankSnapshot {
    public String id;
    public float x;
    public float y;
    public float rotation;
    public int hp;
    public int maxHp;
    /** Installed temporary + permanent armor capacity, included in maxHp. */
    public int armorHp;

    public boolean isArmorSegment(int index) {
        return index >= Math.max(0, maxHp - armorHp) && index < maxHp;
    }

    public int shieldHp;
    public int maxShieldHp;
    public boolean alive;
    public boolean isPlayer;
    /** Xe AI đồng minh của người chơi. */
    public boolean ally;
    /** Ụ súng cố định (vẽ bằng sprite turret). */
    public boolean turret;
}
