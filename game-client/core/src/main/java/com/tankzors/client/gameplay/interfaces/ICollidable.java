package com.tankzors.client.gameplay.interfaces;

import com.tankzors.client.gameplay.entity.Rectangle;

/** Đối tượng có thể va chạm AABB. Parity server ICollidable. */
public interface ICollidable {
    Rectangle getBoundingBox();
    boolean collidesWith(ICollidable other);
}
