package com.tankzors.client.gameplay.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShieldRegenerationTest {
    @Test
    void fullHull_researchDoesNotAccelerateRegeneration() {
        for (int level = 0; level <= 4; level++) {
            PlayerTank tank = tankWithShield(5, level, 1);
            tick(tank, 164);
            assertEquals(0, tank.getShieldHp(), "research " + level);
            tick(tank, 3);
            assertEquals(1, tank.getShieldHp(), "research " + level);
        }
    }

    @Test
    void damagedHull_researchOnlyAddsItsCappedHullBonus() {
        // HP=3/5, deficit=1: floor(165 * 5 / (3 + bonus)) frames.
        int[] delays = {275, 275, 206, 176, 165};
        for (int level = 0; level <= 4; level++) {
            PlayerTank tank = tankWithShield(3, level, 1);
            tick(tank, delays[level] - 1);
            assertEquals(0, tank.getShieldHp(), "research " + level);
            tick(tank, 3);
            assertEquals(1, tank.getShieldHp(), "research " + level);
        }
    }

    @Test
    void nextDelay_usesDeficitBeforeRestoringShield() {
        PlayerTank tank = tankWithShield(5, 0, 2);
        tick(tank, 242); // 240-frame delay plus the regeneration tick.
        assertEquals(1, tank.getShieldHp());
        tick(tank, 200); // Still waiting on the same 240-frame delay, not 165.
        assertEquals(1, tank.getShieldHp());
        tick(tank, 43);
        assertEquals(2, tank.getShieldHp());
    }

    @Test
    void researchAndDamage_doNotRestartActiveTimer() {
        PlayerTank tank = tankWithShield(3, 0, 1);
        tick(tank, 150);
        tank.setShieldResearchLevel(4);
        tank.takeDamage(1);
        tick(tank, 124);
        assertEquals(0, tank.getShieldHp());
        tick(tank, 3);
        assertEquals(1, tank.getShieldHp());
    }

    private static PlayerTank tankWithShield(int hp, int research, int capacity) {
        PlayerTank tank = new PlayerTank("player", 100, 100, 60, hp, 5);
        tank.setShieldResearchLevel(research);
        tank.addShieldCapacity(capacity);
        return tank;
    }

    private static void tick(PlayerTank tank, int frames) {
        for (int i = 0; i < frames; i++) tank.updateShield(1.0 / 60.0);
    }
}
