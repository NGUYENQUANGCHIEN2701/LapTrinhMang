package com.tankzors.client.gameplay.interfaces;

import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.entity.Position;

/** Hợp đồng xe tăng. Parity server ITank. */
public interface ITank {
    String getId();
    void move(Direction direction, double deltaTime);
    void rotate(double angle);
    Position getPosition();
    double getRotation();
    int getHP();
    int getMaxHP();
    void takeDamage(int damage);
    boolean isAlive();
    IBullet shoot();
}
