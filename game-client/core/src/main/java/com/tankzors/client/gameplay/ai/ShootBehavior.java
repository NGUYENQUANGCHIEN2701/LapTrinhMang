package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WeaponProfile;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.interfaces.ITank;
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

        if (self.isTurret()) {
            updateTurret(self, state);
            return;
        }

        // Xe thù địch sống gần nhất; tiebreak theo id (deterministic).
        ITank target = null;
        double bestDist = Double.MAX_VALUE;
        for (ITank t : state.getAllTanks()) {
            if (!self.isHostileTo(t)) continue;
            double d = distance(self.getPosition(), t.getPosition());
            if (d < bestDist || (d == bestDist && (target == null || t.getId().compareTo(target.getId()) < 0))) {
                bestDist = d; target = t;
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
            fire(self, state, baseAngle);
            cooldownRemaining = self.getDifficulty().getShootCooldown();
        }
    }

    /**
     * Builds Turret.findEnemy với isStatic: chỉ quét thẳng theo hướng nòng tới
     * distance+1 ô; thấy xe thù địch thì bắn, nạp lại sau AITank.TURRET_RELOAD_SECONDS.
     */
    private void updateTurret(AITank self, GameWorld state) {
        if (cooldownRemaining > 1e-9) return;
        int tileSize = state.getCurrentMap().getTileSize();
        double facing = normalizedRotation(self.getRotation());
        double dirX = facing == 90 ? 1 : facing == 270 ? -1 : 0;
        double dirY = facing == 180 ? 1 : facing == 0 ? -1 : 0;
        double reach = (self.getTurretRangeTiles() + 1) * tileSize;
        for (ITank t : state.getAllTanks()) {
            if (!self.isHostileTo(t)) continue;
            double dx = t.getPosition().getX() - self.getPosition().getX();
            double dy = t.getPosition().getY() - self.getPosition().getY();
            double along = dx * dirX + dy * dirY;
            double across = Math.abs(dx * dirY - dy * dirX);
            if (along <= 0 || along > reach || across >= tileSize / 2.0) continue;
            fire(self, state, facing);
            cooldownRemaining = AITank.TURRET_RELOAD_SECONDS;
            return;
        }
    }

    private void fire(AITank self, GameWorld state, double angle) {
        String id = bulletIdSource != null ? bulletIdSource.nextId(self.getId()) : "b-0";
        WeaponProfile weapon = WeaponProfile.fromItemId(self.getDifficulty().getReferenceWeaponType());
        boolean horizontal = angle == 90 || angle == 270;
        Bullet bullet = new Bullet(id, self.getId(),
                new Position(self.getPosition().getX(), self.getPosition().getY()), angle,
                weapon.speedPixelsPerSecond(), weapon.getDamage(), horizontal ? 8 : 4, horizontal ? 4 : 8,
                weapon.getItemId());
        state.addBullet(bullet);
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
