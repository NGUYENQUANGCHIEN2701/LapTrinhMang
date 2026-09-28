package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.interfaces.IBullet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIControllerMovementTest {

    @Test
    void movingAiFacesItsCardinalTravelDirectionInsteadOfStrafing() {
        GameWorld world = new GameWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);
        AITank ai = new AITank("ai", 100, 100, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 140, 220);
        world.addTank(ai);
        world.addTank(player);

        double beforeX = ai.getPosition().getX();
        for (int i = 0; i < 20; i++) {
            ai.update(world, 1.0 / 30.0);
        }

        assertTrue(ai.getPosition().getX() > beforeX, "BFS route should start by moving right");
        assertEquals(90.0, ai.getRotation(), 1e-9,
                "AI that is moving right must face right, not aim sideways while travelling");
    }

    @Test
    void firingTickKeepsPositionAndBulletDirectionMatchesRenderedFacing() {
        GameWorld world = new GameWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);
        AITank ai = new AITank("ai", 100, 100, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 140, 220);
        world.addTank(ai);
        world.addTank(player);

        ShootBehavior shoot = (ShootBehavior) ai.getController().getShootBehavior();
        shoot.setBulletIdSource(ownerId -> "ai-shot");

        // Let reaction delay expire and wait until navigation is actively moving right.
        for (int i = 0; i < 30 && ai.getPosition().getX() <= 100.0; i++) {
            ai.update(world, 1.0 / 30.0);
        }
        assertTrue(ai.getPosition().getX() > 100.0);
        assertEquals(90.0, ai.getRotation(), 1e-9);

        // Force a fresh shot while the target's dominant direction is down.
        shoot.setCooldownRemaining(0.0);
        double beforeX = ai.getPosition().getX();
        double beforeY = ai.getPosition().getY();
        ai.update(world, 1.0 / 30.0);

        assertEquals(beforeX, ai.getPosition().getX(), 1e-9);
        assertEquals(beforeY, ai.getPosition().getY(), 1e-9);
        assertEquals(180.0, ai.getRotation(), 1e-9,
                "Navigation must not overwrite the aiming turn in the same tick");

        int bulletsBefore = world.getBulletCount();
        ai.update(world, 1.0 / 30.0);

        assertEquals(bulletsBefore + 1, world.getBulletCount());
        IBullet bullet = world.getAllBullets().get(world.getBulletCount() - 1);
        assertEquals(beforeX, ai.getPosition().getX(), 1e-9);
        assertEquals(beforeY, ai.getPosition().getY(), 1e-9);
        assertEquals(ai.getRotation(), bullet.getDirection(), 1e-9,
                "AI sprite and projectile must never point in different cardinal directions");
    }
}
