package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
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
}
