package com.tankzors.client.gameplay.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MapVisualGridTest {

    @Test
    void parsesOptionalVisualGridWithoutChangingGameplayGrid() {
        MapDefinition map = MapLoader.parse("{"
                + "\"name\":\"visual\",\"width\":2,\"height\":1,\"tileSize\":40,"
                + "\"grid\":[[3,1]],\"visualGrid\":[[100,131]]}");

        assertNotNull(map);
        assertArrayEquals(new int[]{3, 1}, map.getGrid()[0]);
        assertArrayEquals(new int[]{100, 131}, map.getVisualGrid()[0]);
    }
}
