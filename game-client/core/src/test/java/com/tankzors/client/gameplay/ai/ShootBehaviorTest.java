package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.interfaces.IBullet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShootBehaviorTest {

    @Test
    void turnsInPlaceBeforeShootingStraightAlongFacingDirection() {
        GameWorld world = new GameWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);
        AITank ai = new AITank("ai", 200, 200, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 260, 230);
        world.addTank(ai);
        world.addTank(player);

        ShootBehavior behavior = new ShootBehavior();
        behavior.setBulletIdSource(ownerId -> "ai-bullet");
        behavior.update(ai, world, 1.0 / 30.0);

        assertEquals(90.0, ai.getRotation(), 0.001);
        assertTrue(world.getAllBullets().isEmpty(), "AI must spend the first input on turning, not turn and fire together");
        assertTrue(behavior.isReservingMovement(), "Aiming must prevent navigation from overwriting the facing direction");

        behavior.update(ai, world, 1.0 / 30.0);

        IBullet bullet = world.getAllBullets().get(0);
        assertEquals(90.0, ai.getRotation(), 0.001);
        assertEquals(90.0, bullet.getDirection(), 0.001);
        assertEquals(com.tankzors.client.gameplay.entity.Bullet.DEFAULT_SPEED, bullet.getSpeed(), 0.001);
        assertEquals(1, bullet.getDamage(), "AI Shell phải dùng thang damage của Builds");
        assertEquals(0, ((com.tankzors.client.gameplay.entity.Bullet) bullet).getWeaponType());
        assertTrue(behavior.isReservingMovement(), "The firing tick must not also move the AI sideways");
        bullet.update(0.1);
        assertEquals(200.0 + com.tankzors.client.gameplay.entity.Bullet.DEFAULT_SPEED * 0.1,
                bullet.getPosition().getX(), 0.001);
        assertEquals(200.0, bullet.getPosition().getY(), 0.001);
        assertFalse(world.getAllBullets().isEmpty());
    }
}
