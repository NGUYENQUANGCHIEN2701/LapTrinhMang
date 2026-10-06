package com.tankzors.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyTileMappingTest {

    @Test
    void keepsOriginalGroundFrameIds() {
        assertEquals(1, GameRenderer.legacyGroundTileId(1));
        assertEquals(53, GameRenderer.legacyGroundTileId(53));
        assertEquals(1, GameRenderer.legacyGroundTileId(130));
        assertEquals(34, GameRenderer.legacyGroundTileId(170));
    }

    @Test
    void unpacksOriginalWallAndBushFrameIds() {
        assertEquals(1, GameRenderer.legacyWallTileId(130));
        assertEquals(8, GameRenderer.legacyWallTileId(137));
        assertEquals(9, GameRenderer.legacyWallTileId(138));
        assertEquals(10, GameRenderer.legacyWallTileId(139));
        assertEquals(16, GameRenderer.legacyWallTileId(145));
        assertEquals(35, GameRenderer.legacyWallTileId(164));
    }
}
