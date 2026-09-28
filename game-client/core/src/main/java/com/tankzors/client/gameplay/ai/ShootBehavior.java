package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;

import java.util.Comparator;
/**
 * Bắn khi player trong tầm (<250px), đường ngắm rõ, hết cooldown. AI chỉ
 * ngắm theo bốn hướng chính để đạn không bay chéo.
 * Parity server ai.ShootBehavior. Dùng java.util.Random seedable; bullet id
 * deterministic qua BulletIdSource (thay UUID).
 */
public final class ShootBehavior implements IAIBehavior {

    public static final double SHOOT_RANGE = 250.0;
    /** Đạn AI và player dùng cùng tốc độ để nhịp chiến đấu nhất quán. */
    public static final double AI_BULLET_SPEED = Bullet.DEFAULT_SPEED;

    private double cooldownRemaining = 0.0;
    private BulletIdSource bulletIdSource;
    private boolean reservingMovement;

    public ShootBehavior() { }
    /** Giữ constructor cũ để tương thích call site; AI không còn dùng spread ngẫu nhiên. */
    public ShootBehavior(java.util.Random ignored) { }

    public void setBulletIdSource(BulletIdSource source) { this.bulletIdSource = source; }
    public void reset() { cooldownRemaining = 0.0; reservingMovement = false; }
    public double getCooldownRemaining() { return cooldownRemaining; }
    public void setCooldownRemaining(double c) { this.cooldownRemaining = c; }
    public boolean isReservingMovement() { return reservingMovement; }

    @Override
    public void update(AITank self, GameWorld state, double deltaTime) {
        reservingMovement = false;
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
        double baseAngle = cardinalAngle(dx, dy);
        if (cooldownRemaining <= 1e-9) {
            // Một nhịp chỉ được có một chủ sở hữu hướng: nếu cần ngắm hướng mới,
            // AI đứng yên để xoay; nhịp sau mới bắn. Navigation cũng phải đứng
            // yên trong cả nhịp xoay và nhịp bắn để không tạo cảm giác trượt ngang.
            reservingMovement = true;
            if (normalizedRotation(self.getRotation()) != baseAngle) {
                self.rotate(baseAngle);
                return;
            }
            String id = bulletIdSource != null ? bulletIdSource.nextId(self.getId()) : "b-0";
            Bullet bullet = new Bullet(id, self.getId(),
                    new Position(self.getPosition().getX(), self.getPosition().getY()), baseAngle,
                    AI_BULLET_SPEED, Bullet.DEFAULT_DAMAGE, Bullet.DEFAULT_WIDTH, Bullet.DEFAULT_HEIGHT);
            state.addBullet(bullet);
            cooldownRemaining = self.getDifficulty().getShootCooldown();
        }
    }

    private static double distance(Position a, Position b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private static double normalizedRotation(double angle) {
        return ((angle % 360) + 360) % 360;
    }

    /** Chọn trục có sai lệch lớn hơn; tại 45° ưu tiên ngang để deterministic. */
    private static double cardinalAngle(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) return dx >= 0 ? 90.0 : 270.0;
        return dy >= 0 ? 180.0 : 0.0;
    }

    @Override public String getBehaviorName() { return "SHOOT"; }

    /** Nguồn id đạn deterministic — do GameSimulation cấp seed per-match. */
    public interface BulletIdSource {
        String nextId(String ownerId);
    }
}
