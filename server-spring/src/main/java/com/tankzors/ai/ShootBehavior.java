package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.entity.Bullet;
import com.tankzors.entity.PlayerTank;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IAIBehavior;

import java.util.Comparator;
import java.util.Random;

/**
 * AI shooting behavior.
 * Shoots at the player when in range (< 250px), line of sight is clear, and cooldown expired.
 */
public final class ShootBehavior implements IAIBehavior {

    public static final double SHOOT_RANGE = 250.0;

    private final Random random;
    private double cooldownRemaining = 0.0;

    public ShootBehavior() {
        this(new Random());
    }

    public ShootBehavior(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void reset() {
        cooldownRemaining = 0.0;
    }

    public double getCooldownRemaining() {
        return cooldownRemaining;
    }

    public void setCooldownRemaining(double cooldownRemaining) {
        this.cooldownRemaining = cooldownRemaining;
    }

    @Override
    public void update(AITank self, GameState state, double deltaTime) {
        if (self == null || !self.isAlive() || state == null || !state.isPlaying()
                || state.getCurrentMap() == null || !Double.isFinite(deltaTime) || deltaTime <= 0) {
            return;
        }

        // Tích lũy giảm cooldown theo deltaTime
        if (cooldownRemaining > 0) {
            cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaTime);
        }

        // Tìm player còn sống gần nhất
        PlayerTank target = state.getAllTanks().stream()
                .filter(t -> t instanceof PlayerTank && t.isAlive())
                .map(t -> (PlayerTank) t)
                .min(Comparator.comparingDouble((PlayerTank t) -> distance(self.getPosition(), t.getPosition()))
                        .thenComparing(PlayerTank::getId))
                .orElse(null);

        if (target == null) {
            return;
        }

        // 1. Kiểm tra tầm bắn (< 250px)
        double dist = distance(self.getPosition(), target.getPosition());
        if (dist >= SHOOT_RANGE) {
            return;
        }

        // 2. Kiểm tra tường chặn giữa AI và player
        if (!LineOfSight.hasClearShot(state.getCurrentMap(), self.getPosition(), target.getPosition())) {
            return;
        }

        // 3. Tính góc ngắm: atan2(player.y - ai.y, player.x - ai.x)
        // Hệ toạ độ game: 0°=UP, 90°=RIGHT, 180°=DOWN, 270°=LEFT
        double dx = target.getPosition().getX() - self.getPosition().getX();
        double dy = target.getPosition().getY() - self.getPosition().getY();
        double baseAngle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        baseAngle = (baseAngle % 360.0 + 360.0) % 360.0;

        // Xoay súng (thân tank) về phía player
        self.rotate(baseAngle);

        // 4. Kiểm tra cooldown bắn
        if (cooldownRemaining <= 1e-9) {
            // Thêm độ lệch sai số góc ngẫu nhiên theo độ khó (+/- accuracySpread)
            double spread = self.getDifficulty().getAccuracySpread();
            double offset = (random.nextDouble() * 2.0 - 1.0) * spread;
            double bulletAngle = (baseAngle + offset) % 360.0;
            if (bulletAngle < 0) {
                bulletAngle += 360.0;
            }

            Bullet bullet = new Bullet(self.getId(), new Position(self.getPosition().getX(), self.getPosition().getY()), bulletAngle);
            state.addBullet(bullet);

            // Bắt đầu tính cooldown mới
            cooldownRemaining = self.getDifficulty().getShootCooldown();
        }
    }

    private static double distance(Position a, Position b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    @Override
    public String getBehaviorName() {
        return "SHOOT";
    }
}
