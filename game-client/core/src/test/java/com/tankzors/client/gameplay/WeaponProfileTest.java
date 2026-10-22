package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.interfaces.IBullet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression coverage for the six projectile types extracted from Builds. */
class WeaponProfileTest {

    @Test
    void projectileDamage_removesOneHitPointPerDamageUnit() {
        for (WeaponProfile profile : WeaponProfile.values()) {
            PlayerTank tank = new PlayerTank("target", 100, 100, 60, 10, 10);
            tank.takeDamage(profile.getDamage());
            assertEquals(10 - profile.getDamage(), tank.getHP(), profile.name());
        }
    }

    @Test
    void projectileDamage_consumesShieldBeforeHull() {
        for (WeaponProfile profile : WeaponProfile.values()) {
            PlayerTank tank = new PlayerTank("target", 100, 100, 60, 10, 10);
            tank.addShieldCapacity(2);
            for (int i = 0; i < 4; i++) tank.updateShield(100);
            assertEquals(2, tank.getShieldHp());
            tank.takeDamage(profile.getDamage());
            assertEquals(Math.max(0, 2 - profile.getDamage()), tank.getShieldHp(), profile.name());
            assertEquals(10 - Math.max(0, profile.getDamage() - 2), tank.getHP(), profile.name());
        }
    }

    @Test
    void referenceBuildStats_arePreserved() {
        int[] damage = {1, 1, 2, 2, 3, 5};
        double[] speed = {2.0, 3.5, 2.0, 3.5, 2.8, 2.8};
        for (int id = 0; id < 6; id++) {
            WeaponProfile profile = WeaponProfile.fromItemId(id);
            assertEquals(id, profile.getItemId());
            assertEquals(damage[id], profile.getDamage());
            assertEquals(speed[id], profile.getBuildSpeed(), 0.0001);
        }
    }

    @Test
    void playerShot_usesSelectedReferenceProfile() {
        int[] damage = {1, 1, 2, 2, 3, 5};
        double[] speed = {200, 350, 200, 350, 280, 280};
        PlayerTank tank = new PlayerTank("player", 100, 100);
        for (int id = 0; id < 6; id++) {
            tank.setActiveWeaponId(id);
            IBullet raw = tank.shoot();
            Bullet shot = (Bullet) raw;
            assertEquals(id, shot.getWeaponType());
            assertEquals(damage[id], shot.getDamage());
            assertEquals(speed[id], shot.getSpeed(), 0.0001);
            // Reset cooldown for the next independent profile assertion.
            tank.updateCooldown(tank.getShootCooldown());
        }
    }
}
