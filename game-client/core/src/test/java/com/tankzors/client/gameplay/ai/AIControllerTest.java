package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIControllerTest {

    @Test
    void patrolsNearSpawnBeforePlayerEntersVisionRange() {
        GameWorld world = new GameWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);
        AITank ai = new AITank("ai", 500, 300, DifficultyLevel.EASY);
        PlayerTank player = new PlayerTank("player", 60, 60);
        world.addTank(ai);
        world.addTank(player);

        // Nhịp đầu có thể chỉ dùng để quay sang hướng tuần tra.
        ai.update(world, 1.0);
        ai.update(world, 1.0);

        assertEquals("PATROL", ai.getController().getBehaviorName());
        assertTrue(Math.abs(ai.getPosition().getX() - 500.0) > 0.001
                        || Math.abs(ai.getPosition().getY() - 300.0) > 0.001,
                "AI phải rời khỏi ô spawn khi player còn ở ngoài tầm nhìn");
    }
}
