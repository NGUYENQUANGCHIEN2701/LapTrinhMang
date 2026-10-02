package com.tankzors.client.model;

/**
 * Một viên đạn trong STATE snapshot.
 *
 * <p>Wire format từ server:
 * {@code { id, x, y, direction, ownerId, active }}.</p>
 */
public final class BulletSnapshot {
    public String id;
    public float x;
    public float y;
    public float direction;
    public String ownerId;
    /** Shop ammunition ID (0..5), required to render the Builds projectile frame. */
    public int weaponType;
    /** Damage còn lại; Double Shell/Plasma còn 1 được render mờ như Builds. */
    public int damage;
    public boolean active;
}
