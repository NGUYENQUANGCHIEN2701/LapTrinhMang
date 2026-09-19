package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;

import java.util.Random;

/**
 * Controller FSM: IDLE -> CHASE(visible) -> CHASE(blind) -> PATROL.
 * Bắn luôn chạy riêng. Parity server ai.AIController.
 */
public final class AIController {
    private final IdleBehavior idle = new IdleBehavior();
    private final ChaseBehavior chase = new ChaseBehavior();
    private final PatrolBehavior patrol = new PatrolBehavior();
    private final ShootBehavior defaultShootBehavior = new ShootBehavior();
    private IAIBehavior behavior = idle;
    private IAIBehavior shootBehavior = defaultShootBehavior;
    private PlayerTank target;
    private Position lastSeen;
    private double reactionElapsed;
    private boolean hadSight;
    private IMap map;

    /** Cấp RNG seed và bullet-id source cho các behavior (gọi một lần khi tạo AI). */
    public void seed(Random rng, ShootBehavior.BulletIdSource idSource) {
        // Mỗi behavior giữ instance riêng; ở đây không inject trực tiếp vì chúng
        // đã được new ở trên với default seed. Nếu cần tái lập deterministic,
        // controller sẽ được tạo lại qua AITank constructor với seed từ match.
    }

    public String getBehaviorName() { return behavior.getBehaviorName(); }
    public Position getLastSeenPosition() {
        return lastSeen == null ? null : new Position(lastSeen.getX(), lastSeen.getY());
    }
    public IAIBehavior getShootBehavior() { return shootBehavior; }
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
        if (shootBehavior instanceof ShootBehavior) ((ShootBehavior) shootBehavior).reset();
    }

    public void update(AITank self, GameWorld state, double dt) {
        if (!self.isAlive()) { reset(); return; }
        if (!state.isPlaying() || !Double.isFinite(dt) || dt <= 0) return;
        if (map != state.getCurrentMap()) {
            reset();
            map = state.getCurrentMap();
        }
        if (map == null) return;

        // Giữ cùng player đến khi chết/rời; tiebreak deterministic.
        if (target == null || !target.isAlive() || state.getTank(target.getId()) != target) {
            reset();
            map = state.getCurrentMap();
            double bestDist = Double.MAX_VALUE;
            PlayerTank found = null;
            for (ITank t : state.getAllTanks()) {
                if (!(t instanceof PlayerTank) || !t.isAlive()) continue;
                double d = distance(self.getPosition(), t.getPosition());
                if (d < bestDist || (d == bestDist && (found == null || t.getId().compareTo(found.getId()) < 0))) {
                    bestDist = d; found = (PlayerTank) t;
                }
            }
            target = found;
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
            } else if (behavior != patrol) {
                // Chưa phát hiện player: tuần tra nhỏ quanh vị trí spawn thay vì đứng yên.
                patrol.start(self.getPosition());
                behavior = patrol;
            }
        }

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
