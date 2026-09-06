package com.tankzors.ai;

/** Shared AI configuration: distances in pixels, delays in simulation seconds. */
public enum DifficultyLevel {
    EASY(80, 800, 0.5), MEDIUM(120, 300, 0.3), HARD(140, 400, 0.1);

    private final double speed;
    private final double visionRange;
    private final double reactionDelay;

    DifficultyLevel(double speed, double visionRange, double reactionDelay) {
        this.speed = speed;
        this.visionRange = visionRange;
        this.reactionDelay = reactionDelay;
    }

    public double getSpeed() { return speed; }
    public double getVisionRange() { return visionRange; }
    public double getReactionDelay() { return reactionDelay; }
}
