package com.tankzors.client.gameplay.interfaces;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;

/** Một hành vi AI (Patrol/Chase/Shoot/Idle). Parity server IAIBehavior. */
public interface IAIBehavior {
    void update(AITank self, GameWorld state, double deltaTime);
    String getBehaviorName();
}
