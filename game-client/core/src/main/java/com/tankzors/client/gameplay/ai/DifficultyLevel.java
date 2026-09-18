package com.tankzors.client.gameplay.ai;

/**
 * Cấu hình AI theo độ khó. Parity server ai.DifficultyLevel.
 * (speed px/s, visionRange px, reactionDelay s, shootCooldown s, accuracySpread deg)
 */
public enum DifficultyLevel {
    EASY(80, 200, 0.5, 2.0, 15.0),
    MEDIUM(90, 240, 0.5, 2.5, 10.0),
    HARD(105, 280, 0.3, 2.0, 6.0);

    private final double speed;
    private final double visionRange;
    private final double reactionDelay;
    private final double shootCooldown;
    private final double accuracySpread;

    DifficultyLevel(double speed, double visionRange, double reactionDelay, double shootCooldown, double accuracySpread) {
        this.speed = speed;
        this.visionRange = visionRange;
        this.reactionDelay = reactionDelay;
        this.shootCooldown = shootCooldown;
        this.accuracySpread = accuracySpread;
    }

    public double getSpeed() { return speed; }
    public double getVisionRange() { return visionRange; }
    public double getReactionDelay() { return reactionDelay; }
    public double getShootCooldown() { return shootCooldown; }
    public double getAccuracySpread() { return accuracySpread; }
}
