package com.tankzors.client.gameplay;

/** Rules extracted from Builds game.Player, Bullet, Rocket, Dynamite and Aircraft. */
public final class SpecialAmmoRules {
    private SpecialAmmoRules() {}
    public static int artilleryDistance(int level) { return level >= 3 ? 5 : 4; }
    public static int artilleryRadius(int level) { return level >= 5 ? 4 : 3; }
    public static int artilleryPower(int level) {
        return level >= 5 ? 17 : level >= 4 ? 14 : level >= 2 ? 11 : 9;
    }
    public static double artillerySpeed(int level, int tileSize) {
        return (level >= 4 ? 1.0 : 0.7) * 60 * tileSize / 20.0;
    }
    public static int airRadius(int level) { return level >= 2 ? 4 : 3; }
    public static int airPower(int level) { return level >= 3 ? 21 : level >= 2 ? 18 : 11; }
    public static int airBombs(int level) { return level >= 3 ? 7 : 5; }
    public static boolean repeatsWhileHeld(int itemId) { return itemId <= 7; }
}
