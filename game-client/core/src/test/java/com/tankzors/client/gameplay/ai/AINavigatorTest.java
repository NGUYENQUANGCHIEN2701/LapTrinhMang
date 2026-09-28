package com.tankzors.client.gameplay.ai;

import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.map.MapDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AINavigatorTest {

    @Test
    void directionChangeRotatesInPlaceBeforeFollowingRoute() {
        MapDefinition map = TestMaps.open();
        AITank tank = new AITank("ai", 100, 100, DifficultyLevel.EASY);
        AINavigator navigator = new AINavigator();

        AINavigator.Result first = navigator.move(tank, map, new Position(140, 100), 10, 1.0 / 30.0);

        assertEquals(AINavigator.Result.MOVING, first);
        assertEquals(100.0, tank.getPosition().getX(), 1e-9);
        assertEquals(100.0, tank.getPosition().getY(), 1e-9);
        assertEquals(90.0, tank.getRotation(), 1e-9);

        navigator.move(tank, map, new Position(140, 100), 10, 1.0 / 30.0);

        assertTrue(tank.getPosition().getX() > 100.0);
        assertEquals(100.0, tank.getPosition().getY(), 1e-9);
    }
}
