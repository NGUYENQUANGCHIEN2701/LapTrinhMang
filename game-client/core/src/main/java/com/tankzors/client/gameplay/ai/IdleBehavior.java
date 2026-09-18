package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;

/** No-op behavior. Parity server ai.IdleBehavior. */
public final class IdleBehavior implements IAIBehavior {
    @Override public void update(AITank self, GameWorld state, double deltaTime) {}
    @Override public String getBehaviorName() { return "IDLE"; }
}
