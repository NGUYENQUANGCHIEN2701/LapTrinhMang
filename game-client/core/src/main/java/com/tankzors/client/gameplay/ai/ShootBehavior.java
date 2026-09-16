package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;

import java.util.Comparator;
import java.util.Random;

/**
 * Bắn khi player trong tầm (<250px), đường ngắm rõ, hết cooldown.
 * Parity server ai.ShootBehavior. Dùng java.util.Random seedable; bullet id
 * deterministic qua BulletIdSource (thay UUID).
 */
public final class ShootBehavior implements IAIBehavior {

    public static final double SHOOT_RANGE = 250.0;

    private final Random random;
    private double cooldownRemaining = 0.0;
    private BulletIdSource bulletIdSource;

    public ShootBehavior() { this(new Random()); }
    public ShootBehavior(Random random) { this.random = random != null ? random : new Random(); }

    public void setBulletIdSource(BulletIdSource source) { this.bulletIdSource = source; }
    public void reset() { cooldownRemaining = 0.0; }
    public double getCooldownRemaining() { return cooldownRemaining; }
    public void setCooldownRemaining(double c) { this.cooldownRemaining = c; }

    @Override
    public void update(AITank self, GameWorld state, double deltaTime) {
        if (self == null || !self.isAlive() || state == null || !state.isPlaying()
                || state.getCurrentMap() == null || !Double.isFinite(deltaTime) || deltaTime <= 0) return;

        if (cooldownRemaining > 0) cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaTime);

        // Player sống gần nhất; tiebreak theo id (deterministic).
        PlayerTank target = null;
        double bestDist = Double.MAX_VALUE;
        for (com.tankzors.client.gameplay.interfaces.ITank t : state.getAllTanks()) {
            if (!(t instanceof PlayerTank) || !t.isAlive()) continue;
            double d = distance(self.getPosition(), t.getPosition());
            if (d < bestDist || (d == bestDist && (target == null || t.getId().compareTo(target.getId()) < 0))) {
                bestDist = d; target = (PlayerTank) t;
            }
        }
        if (target == null) return;

        if (bestDist >= SHOOT_RANGE) return;
        if (!LineOfSight.hasClearShot(state.getCurrentMap(), self.getPosition(), target.getPosition())) return;

        double dx = target.getPosition().getX() - self.getPosition().getX();
        double dy = target.getPosition().getY() - self.getPosition().getY();
        double baseAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        baseAngle = (baseAngle % 360.0 + 360.0) % 360.0;
        self.rotate(baseAngle);

        if (cooldownRemaining <= 1e-9) {
            double spread = self.getDifficulty().getAccuracySpread();
            double offset = (random.nextDouble() * 2.0 - 1.0) * spread;
            double bulletAngle = (baseAngle + offset) % 360.0;
            if (bulletAngle < 0) bulletAngle += 360.0;
            String id = bulletIdSource != null ? bulletIdSource.nextId(self.getId()) : "b-0";
            Bullet bullet = new Bullet(id, self.getId(),
                    new Position(self.getPosition().getX(), self.getPosition().getY()), bulletAngle,
                    Bullet.DEFAULT_SPEED, Bullet.DEFAULT_DAMAGE, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
            state.addBullet(bullet);
            cooldownRemaining = self.getDifficulty().getShootCooldown();
        }
    }

    private static double distance(Position a, Position b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    @Override public String getBehaviorName() { return "SHOOT"; }

    /** Nguồn id đạn deterministic — do GameSimulation cấp seed per-match. */
    public interface BulletIdSource {
        String nextId(String ownerId);
    }
}
