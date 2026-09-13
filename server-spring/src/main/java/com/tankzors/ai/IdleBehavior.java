package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.interfaces.IAIBehavior;

public final class IdleBehavior implements IAIBehavior {
    @Override
    public void update(AITank self, GameState state, double deltaTime) {}

    @Override
    public String getBehaviorName() { return "IDLE"; }
}
