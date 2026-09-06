package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.entity.PlayerTank;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IAIBehavior;
import com.tankzors.interfaces.IMap;
import java.util.Comparator;

/** Simulation-thread controller. Idle before first contact, local search after contact. */
public final class AIController {
    private final IdleBehavior idle = new IdleBehavior();
    private final ChaseBehavior chase = new ChaseBehavior();
    private final PatrolBehavior patrol = new PatrolBehavior();
    private IAIBehavior behavior = idle;
    private IAIBehavior shootBehavior;
    private PlayerTank target;
    private Position lastSeen;
    private double reactionElapsed;
    private boolean hadSight;
    private IMap map;

    public String getBehaviorName() { return behavior.getBehaviorName(); }
    public Position getLastSeenPosition() {
        return lastSeen == null ? null : new Position(lastSeen.getX(), lastSeen.getY());
    }

    /** Task 2.2 plugs shooting in after movement, without replacing Chase/Patrol. */
    public void setShootBehavior(IAIBehavior shootBehavior) { this.shootBehavior = shootBehavior; }

    public void reset() {
        behavior = idle;
        target = null;
        lastSeen = null;
        reactionElapsed = 0;
        hadSight = false;
        map = null;
        chase.reset();
        patrol.reset();
    }

    public void update(AITank self, GameState state, double dt) {
        if (!self.isAlive()) { reset(); return; }
        if (!state.isPlaying() || !Double.isFinite(dt) || dt <= 0) return;
        if (map != state.getCurrentMap()) {
            reset();
            map = state.getCurrentMap();
        }
        if (map == null) return;
        // Retain the same player until death/removal; ties on acquisition are deterministic.
        if (target == null || !target.isAlive() || state.getTank(target.getId()) != target) {
            reset();
            map = state.getCurrentMap();
            target = state.getAllTanks().stream()
                    .filter(t -> t instanceof PlayerTank && t.isAlive())
                    .map(t -> (PlayerTank) t)
                    .min(Comparator.comparingDouble((PlayerTank t) -> distance(self.getPosition(), t.getPosition()))
                            .thenComparing(PlayerTank::getId)).orElse(null);
        }
        if (target == null) return;
        boolean visible = distance(self.getPosition(), target.getPosition()) < self.getDifficulty().getVisionRange()
                && LineOfSight.hasClearShot(map, self.getPosition(), target.getPosition());
        if (visible) {
            reactionElapsed = Math.min(self.getDifficulty().getReactionDelay(), reactionElapsed + dt);
            if (reactionElapsed + 1e-9 >= self.getDifficulty().getReactionDelay()) {
                lastSeen = new Position(target.getPosition().getX(), target.getPosition().getY());
                if (behavior != chase) chase.reset();
                behavior = chase;
                chase.setTarget(lastSeen, true);
            }
        } else {
            reactionElapsed = 0;
            if (hadSight && lastSeen != null) {
                chase.reset();
                chase.setTarget(lastSeen, false);
                behavior = chase;
            }
        }
        // During reacquisition delay, continue searching the old observation only.
        hadSight = visible && reactionElapsed + 1e-9 >= self.getDifficulty().getReactionDelay();
        behavior.update(self, state, dt);
        if (behavior == chase && chase.isSearchFinished()) {
            patrol.start(lastSeen);
            behavior = patrol;
        }
        if (shootBehavior != null) shootBehavior.update(self, state, dt);
    }

    private static double distance(Position a, Position b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }
}
