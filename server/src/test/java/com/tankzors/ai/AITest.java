package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.entity.PlayerTank;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AITest {

    @Test
    public void testAIBehaviorAtFarDistance() {
        GameMap map = MapLoader.load("campaign_1");
        assertNotNull(map);

        GameState state = new GameState();
        state.setCurrentMap(map);
        state.setStatus(GameState.Status.PLAYING);

        // Player at (180, 300) - like in the screenshot
        PlayerTank player = new PlayerTank("player", 180, 300);
        state.addTank(player);

        // AI at spawn (660, 460)
        AITank ai = new AITank("ai", 660, 460, DifficultyLevel.EASY);
        state.addTank(ai);

        ai.update(state, 0.033);
        assertEquals("IDLE", ai.getController().getBehaviorName(), "Should be IDLE when far away");
        assertEquals(660, ai.getPosition().getX(), 1e-3);
        assertEquals(460, ai.getPosition().getY(), 1e-3);
    }

    @Test
    public void testAIBehaviorWhenPlayerGetsClose() {
        GameMap map = MapLoader.load("campaign_1");
        assertNotNull(map);

        GameState state = new GameState();
        state.setCurrentMap(map);
        state.setStatus(GameState.Status.PLAYING);

        // AI at spawn (660, 460)
        AITank ai = new AITank("ai", 660, 460, DifficultyLevel.EASY);
        state.addTank(ai);

        // Player moves close to AI: at (660, 340) -> distance = 120px < 200px vision range, clear LOS
        PlayerTank player = new PlayerTank("player", 660, 340);
        state.addTank(player);

        // Wait for reaction time (0.5s for EASY -> ~16 ticks at 33ms)
        for (int i = 0; i < 25; i++) {
            ai.update(state, 0.033);
        }

        System.out.println("Behavior after 0.8s: " + ai.getController().getBehaviorName());
        System.out.println("AI position: " + ai.getPosition().getX() + ", " + ai.getPosition().getY());

        assertEquals("CHASE", ai.getController().getBehaviorName(), "Should be CHASE after reaction delay");
        // Should have moved towards player (y should decrease from 460 towards 340)
        assertTrue(ai.getPosition().getY() < 460, "AI should have moved towards player!");
    }
}
