package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IAIBehavior;

/** Pursues only a controller-approved observation, never a hidden player's live position. */
public final class ChaseBehavior implements IAIBehavior {
    private final AINavigator navigator = new AINavigator();
    private Position target;
    private boolean visible;
    private boolean holding;
    private boolean searchFinished;

    public void setTarget(Position observation, boolean visible) {
        target = new Position(observation.getX(), observation.getY());
        this.visible = visible;
    }

    public void reset() {
        navigator.reset();
        target = null;
        holding = false;
        searchFinished = false;
    }

    public boolean isSearchFinished() { return searchFinished; }

    @Override
    public void update(AITank self, GameState state, double deltaTime) {
        if (target == null || !self.isAlive() || !state.isPlaying()
                || state.getCurrentMap() == null || !Double.isFinite(deltaTime) || deltaTime <= 0) return;
        double distance = Math.hypot(target.getX() - self.getPosition().getX(),
                target.getY() - self.getPosition().getY());
        searchFinished = false;
        if (visible) {
            if (distance <= 100 + 1e-6) holding = true;
            else if (distance > 200) holding = false;
            if (holding) return;
        } else {
            holding = false;
        }
        double budget = self.getSpeed() * deltaTime;
        if (visible) budget = Math.min(budget, Math.max(0, distance - 100));
        AINavigator.Result result = navigator.move(self, state.getCurrentMap(), target, budget, deltaTime);
        searchFinished = !visible && result != AINavigator.Result.MOVING;
    }

    @Override
    public String getBehaviorName() { return "CHASE"; }
}
