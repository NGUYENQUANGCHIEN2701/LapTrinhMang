package com.tankzors.client.gameplay.map;

import com.tankzors.client.gameplay.entity.Wall;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapObstacleTest {
    @Test
    void obstaclesHaveRequiredDurabilityAndPickupsAreConsumable() {
        int[][] grid = {
                {1, 5, 6},
                {7, 8, 0}
        };
        MapDefinition map = new MapDefinition("items", 3, 2, 40, grid);
        Wall brick = map.getWalls().stream().filter(w -> w.getTileType() == TileType.WALL).findFirst().orElseThrow();
        Wall white = map.getWalls().stream().filter(w -> w.getTileType() == TileType.WHITE_WALL).findFirst().orElseThrow();
        Wall bush = map.getWalls().stream().filter(w -> w.getTileType() == TileType.BUSH).findFirst().orElseThrow();

        assertNull(map.hitWall(brick));
        assertNull(map.hitWall(brick));
        assertNull(map.hitWall(brick));
        assertEquals(TileType.WALL, map.hitWall(brick));
        assertEquals(TileType.GROUND, map.getTileAt(0, 0));

        for (int i = 0; i < 20; i++) assertNull(map.hitWall(white));
        assertEquals(TileType.WHITE_WALL, map.getTileAt(1, 0));

        assertEquals(TileType.BUSH, map.hitWall(bush));
        assertEquals(TileType.GROUND, map.getTileAt(2, 0));

        assertEquals(TileType.REPAIR_PICKUP, map.collectPickupAt(20, 60));
        assertEquals(TileType.SPEED_PICKUP, map.collectPickupAt(60, 60));
        assertNull(map.collectPickupAt(60, 60));
    }
}
