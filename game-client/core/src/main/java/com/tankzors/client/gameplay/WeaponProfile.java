package com.tankzors.client.gameplay;

/**
 * Combat data for the six shell types from the reference HTML5 build.
 *
 * <p>The reference game measures projectile speed in pixels per tick.  This
 * client runs in seconds, so {@link #speedPixelsPerSecond()} maps its normal
 * shell speed (2) to the pre-existing 200 px/s simulation speed and preserves
 * all relative speeds exactly.</p>
 */
public enum WeaponProfile {
    SHELL(0, 1, 2.0, 0, Explosion.SMALL),
    PLASMA(1, 1, 3.5, 4, Explosion.PLASMA),
    DOUBLE_SHELL(2, 2, 2.0, 0, Explosion.SMALL),
    DOUBLE_PLASMA(3, 2, 3.5, 4, Explosion.BIG_PLASMA),
    ARMOR_PIERCING(4, 3, 2.8, 13, Explosion.BIG_RED),
    REINFORCED_AP(5, 5, 2.8, 13, Explosion.BIG_RED);

    /** Explosion sheet selected by the reference build for this shell. */
    public enum Explosion { SMALL, PLASMA, BIG_PLASMA, BIG_RED }

    private static final double PIXELS_PER_SECOND_PER_BUILD_SPEED = 100.0;

    private final int itemId;
    private final int damage;
    private final double buildSpeed;
    private final int spriteFrameOffset;
    private final Explosion explosion;

    WeaponProfile(int itemId, int damage, double buildSpeed, int spriteFrameOffset, Explosion explosion) {
        this.itemId = itemId;
        this.damage = damage;
        this.buildSpeed = buildSpeed;
        this.spriteFrameOffset = spriteFrameOffset;
        this.explosion = explosion;
    }

    public int getItemId() { return itemId; }
    public int getDamage() { return damage; }
    public double getBuildSpeed() { return buildSpeed; }
    public double speedPixelsPerSecond() { return buildSpeed * PIXELS_PER_SECOND_PER_BUILD_SPEED; }
    public int getSpriteFrameOffset() { return spriteFrameOffset; }
    public Explosion getExplosion() { return explosion; }

    /** The spritesheet has four cardinal frames per visual projectile type. */
    public int spriteFrameForDirection(double directionDegrees) {
        int cardinal = ((int) Math.floor((((directionDegrees % 360) + 360) % 360 + 45) / 90)) % 4;
        // Build order: down, left, up, right. Client angles: up, right, down, left.
        int[] buildDirectionFrame = {2, 3, 0, 1};
        return spriteFrameOffset + buildDirectionFrame[cardinal];
    }

    public static WeaponProfile fromItemId(int itemId) {
        for (WeaponProfile profile : values()) {
            if (profile.itemId == itemId) return profile;
        }
        return SHELL;
    }
}
