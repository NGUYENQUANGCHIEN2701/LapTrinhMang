package com.tankzors.ai;

/** Shared AI configuration: distances in pixels, delays in simulation seconds. */
public enum DifficultyLevel {
    EASY(80, 200, 0.5, 2.0, 15.0),
    MEDIUM(120, 300, 0.3, 1.5, 8.0),
    HARD(140, 400, 0.1, 1.0, 3.0);

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

    DifficultyLevel(double speed, double visionRange, double reactionDelay) {
        this(speed, visionRange, reactionDelay, 2.0, 15.0);
    }

    public double getSpeed() { return speed; }
    public double getVisionRange() { return visionRange; }
    public double getReactionDelay() { return reactionDelay; }
    public double getShootCooldown() { return shootCooldown; }
    public double getAccuracySpread() { return accuracySpread; }
}
