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
    public boolean active;
}
