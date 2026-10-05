package com.tankzors.client.model;

/** Render snapshot for one tank; co-op WORLD_STATE uses the same fields. */
public final class TankSnapshot {
    public String id;
    public float x;
    public float y;
    public float rotation;
    public int hp;
    public int maxHp;
    public int shieldHp;
    public int maxShieldHp;
    public boolean alive;
    public boolean isPlayer;
}
