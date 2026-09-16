package com.tankzors.client.model;

/**
 * Một tank trong STATE snapshot.
 *
 * <p>Wire format từ server (protocol-v1 mục 3.5):
 * {@code { id, x, y, rotation, hp, maxHp, alive, isPlayer }}.</p>
 */
public final class TankSnapshot {
    public String id;
    public float x;
    public float y;
    public float rotation; // 0=UP, 90=RIGHT, 180=DOWN, 270=LEFT
    public int hp;
    public int maxHp;
    public boolean alive;
    public boolean isPlayer;
}
