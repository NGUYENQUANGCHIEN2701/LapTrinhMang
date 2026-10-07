package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.interfaces.IAIBehavior;

/**
 * Bộ điều khiển một xe AI. Xe thường dùng {@link BuildsBotBrain} (port AI
 * xe tăng của Builds); ụ súng cố định dùng nhánh bắn của {@link ShootBehavior}.
 */
public final class AIController {
    private final ShootBehavior turretShooter = new ShootBehavior();
    private BuildsBotBrain brain = new BuildsBotBrain(0L);

    /**
     * Cấp seed RNG (rẽ trái/phải ngẫu nhiên như Builds) và nguồn id đạn
     * deterministic; gọi một lần khi simulation tạo xe.
     */
    public void seed(long seed, BuildsBotBrain.BulletIdSource idSource) {
        brain = new BuildsBotBrain(seed);
        brain.setBulletIdSource(idSource);
        turretShooter.setBulletIdSource(idSource::nextId);
    }

    public IAIBehavior getShootBehavior() { return turretShooter; }

    /** Xe bị gỡ khỏi world: bỏ đường đi/trạng thái; seed và nguồn id giữ nguyên. */
    public void reset() {
        brain.reset();
        turretShooter.reset();
    }

    public void update(AITank self, GameWorld state, double dt) {
        if (!self.isAlive() || !state.isPlaying() || !Double.isFinite(dt) || dt <= 0
                || state.getCurrentMap() == null) {
            return;
        }
        if (self.isTurret()) {
            turretShooter.update(self, state, dt);
            return;
        }
        brain.update(self, state, dt);
    }

    /** Xe bị trúng đạn bay theo hướng {@code bulletDirection} (độ, 0 = lên). */
    public void onAttacked(double bulletDirection) {
        brain.onAttacked(bulletDirection);
    }

    /** Ô lưới xe đang đứng hoặc đã giữ chỗ (teamsGrid của Builds). */
    public boolean occupies(int col, int row) {
        return brain.occupies(col, row);
    }

    BuildsBotBrain brain() { return brain; }
}
