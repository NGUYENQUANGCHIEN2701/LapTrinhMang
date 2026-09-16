package com.tankzors.client.gameplay.interfaces;

import com.tankzors.client.gameplay.entity.Position;

/** Hợp đồng viên đạn. Parity server IBullet. */
public interface IBullet {
    void update(double deltaTime);
    Position getPosition();
    double getDirection();
    int getDamage();
    double getSpeed();
    boolean isActive();
    void deactivate();
    String getOwnerId();
}
