package com.tankzors.map;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MapLoaderTest {

    @Test
    void loadsAllCampaignMapsAsData() {
        for (String levelId : new String[] {"campaign_1", "campaign_2", "campaign_3"}) {
            GameMap map = MapLoader.load(levelId);
            assertNotNull(map, levelId);
            assertEquals(20, map.getWidth());
            assertEquals(15, map.getHeight());
            assertEquals(40, map.getTileSize());
            assertNotNull(map.getGrid());
            assertEquals(map.getHeight(), map.getGrid().length);
            assertNotNull(map.getRewards());
            assertFalse(map.getEnemies().isEmpty());
        }
    }

    @Test
    void keepsMapMetadataNeededByClientAndResultValidation() {
        GameMap map = MapLoader.load("campaign_1");
        assertNotNull(map);
        assertEquals("EASY", map.getDifficulty());
        assertEquals(3, map.getRewards().getStars());
        assertEquals(100, map.getRewards().getCoins());
        assertEquals(1, map.getEnemies().size());
        assertEquals("EASY", map.getEnemies().get(0).getDifficulty());
    }

    @Test
    void returnsNullForMissingOrInvalidMap() {
        assertNull(MapLoader.load("non_existent_map"));
        assertNull(MapLoader.loadFromStream(new ByteArrayInputStream(
                "{not-json}".getBytes(StandardCharsets.UTF_8))));
    }
}
