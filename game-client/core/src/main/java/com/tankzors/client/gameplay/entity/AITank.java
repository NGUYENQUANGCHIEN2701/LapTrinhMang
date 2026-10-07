package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WeaponProfile;
import com.tankzors.client.gameplay.ai.AIController;
import com.tankzors.client.gameplay.ai.DifficultyLevel;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.ITank;

import java.util.Objects;

/** Một tank = một controller. Parity server entity.AITank. */
public class AITank extends Tank {
    /** Phe của xe AI: địch (mặc định) hoặc đồng minh của người chơi. */
    public enum Team { ENEMY, ALLY }

    /** Builds: ụ súng nạp đạn reloadDelay mặc định 45 + 10 frame ở 60 Hz. */
    public static final double TURRET_RELOAD_SECONDS = 55.0 / 60.0;

    private final DifficultyLevel difficulty;
    private final AIController controller;
    private Team team = Team.ENEMY;
    private boolean turret;
    private int turretRangeTiles;
    private boolean followPlayer;

    public AITank(String id, double x, double y, DifficultyLevel difficulty) {
        this(id, x, y, difficulty, new AIController());
    }

    public AITank(String id, double x, double y, DifficultyLevel difficulty, AIController controller) {
        super(id, x, y, Objects.requireNonNull(difficulty).getSpeed(), difficulty.getReferenceHitPoints(), difficulty.getReferenceHitPoints(), 40, 40);
        this.difficulty = difficulty;
        this.controller = controller;
    }

    /** Ụ súng cố định Builds: không di chuyển, chỉ bắn theo hướng nòng trong tầm. */
    public static AITank turret(String id, double x, double y, Direction facing, int rangeTiles, int hitPoints) {
        AITank tank = new AITank(id, x, y, DifficultyLevel.EASY);
        tank.turret = true;
        tank.turretRangeTiles = Math.max(1, rangeTiles);
        tank.hp = Math.max(1, hitPoints);
        tank.maxHp = tank.hp;
        tank.speed = 0;
        tank.rotation = facing == Direction.RIGHT ? 90 : facing == Direction.DOWN ? 180
                : facing == Direction.LEFT ? 270 : 0;
        return tank;
    }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team != null ? team : Team.ENEMY; }
    public boolean isAlly() { return team == Team.ALLY; }
    public boolean isTurret() { return turret; }
    public int getTurretRangeTiles() { return turretRangeTiles; }
    /** Đồng minh Builds aiType 1 (FollowPlayer): bám theo người chơi khi không thấy địch. */
    public boolean isFollowPlayer() { return followPlayer; }
    public void setFollowPlayer(boolean followPlayer) { this.followPlayer = followPlayer; }

    /** Hai xe thù địch với nhau (người chơi + đồng minh đối đầu phe địch). */
    public boolean isHostileTo(ITank other) {
        if (other == null || other == this || !other.isAlive()) return false;
        if (isAlly()) return other instanceof AITank && !((AITank) other).isAlly();
        return other instanceof PlayerTank || (other instanceof AITank && ((AITank) other).isAlly());
    }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public AIController getController() { return controller; }

    public void update(GameWorld state, double deltaTime) {
        controller.update(this, state, deltaTime);
    }

    @Override
    public IBullet shoot() {
        WeaponProfile weapon = WeaponProfile.fromItemId(difficulty.getReferenceWeaponType());
        boolean horizontal = rotation == 90 || rotation == 270;
        return new Bullet("b-0", id, new Position(position.getX(), position.getY()), rotation,
                weapon.speedPixelsPerSecond(), weapon.getDamage(), horizontal ? 8 : 4, horizontal ? 4 : 8,
                weapon.getItemId());
    }
}
