package com.tankzors.client.gameplay.map;

import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WorldSnapshotMapper;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.model.GameStateSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapObstacleTest {
    @Test
    void snapshotKeepsEverySurvivingBrownWallDamageStage() {
        int[][] grid = {
                {0, 0, 0},
                {0, 1, 0},
                {0, 0, 0}
        };
        MapDefinition map = new MapDefinition("damage-frames", 3, 3, 40, grid);
        Wall wall = map.getWalls().get(0);
        GameWorld world = new GameWorld();
        world.setCurrentMap(map);

        assertSnapshotWallHp(world, 4);
        assertNull(map.hitWall(wall));
        assertSnapshotWallHp(world, 3);
        assertNull(map.hitWall(wall));
        assertSnapshotWallHp(world, 2);
        assertNull(map.hitWall(wall));
        assertSnapshotWallHp(world, 1);
        assertEquals(TileType.WALL, map.hitWall(wall));
        GameStateSnapshot destroyed = WorldSnapshotMapper.snapshot(world, "player", 0, 0);
        assertEquals(TileType.GROUND.getValue(), destroyed.map.grid[1][1]);
        assertEquals(0, destroyed.map.wallHitPoints[1][1]);
    }

    private static void assertSnapshotWallHp(GameWorld world, int expectedHp) {
        GameStateSnapshot snapshot = WorldSnapshotMapper.snapshot(world, "player", 0, 0);
        assertNotNull(snapshot.map.wallHitPoints);
        assertEquals(expectedHp, snapshot.map.wallHitPoints[1][1]);
    }

    @Test
    void obstaclesHaveRequiredDurabilityAndPickupsAreConsumable() {
        int[][] grid = {
                {7, 8, 0, 0, 0},
                {0, 1, 5, 6, 0},
                {0, 0, 0, 0, 0}
        };
        MapDefinition map = new MapDefinition("items", 5, 3, 40, grid);
        Wall brick = map.getWalls().stream().filter(w -> w.getTileType() == TileType.WALL).findFirst().orElseThrow();
        Wall white = map.getWalls().stream().filter(w -> w.getTileType() == TileType.WHITE_WALL).findFirst().orElseThrow();
        Wall bush = map.getWalls().stream().filter(w -> w.getTileType() == TileType.BUSH).findFirst().orElseThrow();

        assertNull(map.hitWall(brick));
        assertNull(map.hitWall(brick));
        assertNull(map.hitWall(brick));
        assertEquals(TileType.WALL, map.hitWall(brick));
        assertEquals(TileType.GROUND, map.getTileAt(1, 1));

        for (int i = 0; i < 20; i++) assertNull(map.hitWall(white));
        assertEquals(TileType.WHITE_WALL, map.getTileAt(2, 1));

        assertEquals(TileType.BUSH, map.hitWall(bush));
        assertEquals(TileType.GROUND, map.getTileAt(3, 1));

        assertEquals(TileType.REPAIR_PICKUP, map.collectPickupAt(20, 20));
        assertEquals(TileType.SPEED_PICKUP, map.collectPickupAt(60, 20));
        assertNull(map.collectPickupAt(60, 20));
    }

    @Test
    void brownBoundaryIsIndestructibleButInteriorBrownWallStillBreaksAfterFourHits() {
        int[][] grid = {
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
        MapDefinition map = new MapDefinition("boundary", 3, 3, 40, grid);
        Wall boundary = map.getWalls().stream()
                .filter(w -> w.getCol() == 1 && w.getRow() == 0)
                .findFirst().orElseThrow();
        Wall interior = map.getWalls().stream()
                .filter(w -> w.getCol() == 1 && w.getRow() == 1)
                .findFirst().orElseThrow();

        for (int i = 0; i < 10; i++) assertNull(map.hitWall(boundary));
        assertEquals(TileType.WALL, map.getTileAt(1, 0));
        assertEquals(4, boundary.getHitPoints(), "Boundary hits must not consume durability");

        for (int i = 0; i < 3; i++) assertNull(map.hitWall(interior));
        assertEquals(TileType.WALL, map.hitWall(interior));
        assertEquals(TileType.GROUND, map.getTileAt(1, 1));
    }
}
