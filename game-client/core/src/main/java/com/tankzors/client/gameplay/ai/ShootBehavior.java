package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WeaponProfile;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;
import com.tankzors.client.gameplay.interfaces.ITank;

/**
 * Builds Turret.findEnemy với isStatic: ụ súng chỉ quét thẳng theo hướng nòng
 * tới distance+1 ô; thấy xe thù địch thì bắn, nạp lại sau
 * {@link AITank#TURRET_RELOAD_SECONDS}. Bullet id deterministic qua {@link BulletIdSource}.
 */
public final class ShootBehavior implements IAIBehavior {

    private double cooldownRemaining = 0.0;
    private BulletIdSource bulletIdSource;

    public void setBulletIdSource(BulletIdSource source) { this.bulletIdSource = source; }
    public void reset() { cooldownRemaining = 0.0; }
    public double getCooldownRemaining() { return cooldownRemaining; }
    public void setCooldownRemaining(double c) { this.cooldownRemaining = c; }

    @Override
    public void update(AITank self, GameWorld state, double deltaTime) {
        if (self == null || !self.isAlive() || state == null || !state.isPlaying()
                || state.getCurrentMap() == null || !Double.isFinite(deltaTime) || deltaTime <= 0) return;
        if (cooldownRemaining > 0) cooldownRemaining = Math.max(0.0, cooldownRemaining - deltaTime);
        if (cooldownRemaining > 1e-9) return;

        int tileSize = state.getCurrentMap().getTileSize();
        double facing = ((self.getRotation() % 360) + 360) % 360;
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
        state.addBullet(new Bullet(id, self.getId(),
                new Position(self.getPosition().getX(), self.getPosition().getY()), angle,
                weapon.speedPixelsPerSecond(), weapon.getDamage(), horizontal ? 8 : 4, horizontal ? 4 : 8,
                weapon.getItemId()));
    }

    @Override public String getBehaviorName() { return "TURRET"; }

    /** Nguồn id đạn deterministic — do GameSimulation cấp seed per-match. */
    public interface BulletIdSource {
        String nextId(String ownerId);
    }
}
