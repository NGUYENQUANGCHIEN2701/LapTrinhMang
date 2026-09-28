package com.tankzors.map;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class MapLoaderTest {

    @Test
    void loadsAllCampaignMapsAsData() {
        for (String levelId : new String[] {"campaign_1", "campaign_2", "campaign_3"}) {
            GameMap map = MapLoader.load(levelId);
            assertNotNull(map, levelId);
            if ("campaign_1".equals(levelId)) {
                assertEquals(20, map.getWidth());
                assertEquals(15, map.getHeight());
                assertEquals(1, countTiles(map, 3), "Màn 1 phải có đúng một player spawn xanh");
                assertEquals(1, countTiles(map, 4), "Màn 1 phải có đúng một AI spawn đỏ");
                assertEquals(1, countTiles(map, 9), "Màn 1 phải có đúng một cờ đích");
                assertTrue(isReachable(map, 3, 9), "Player phải có đường đi tới cờ");
                assertTrue(isReachable(map, 4, 3), "AI phải có đường đi tới khu player");
            } else {
                assertEquals(20, map.getWidth());
                assertEquals(15, map.getHeight());
            }
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

    private static int countTiles(GameMap map, int tile) {
        int count = 0;
        for (int[] row : map.getGrid()) {
            for (int value : row) if (value == tile) count++;
        }
        return count;
    }

    private static boolean isReachable(GameMap map, int fromTile, int toTile) {
        int[][] grid = map.getGrid();
        int[] start = null;
        int[] goal = null;
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] == fromTile) start = new int[]{col, row};
                if (grid[row][col] == toTile) goal = new int[]{col, row};
            }
        }
        if (start == null || goal == null) return false;
        boolean[][] visited = new boolean[map.getHeight()][map.getWidth()];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(start);
        visited[start[1]][start[0]] = true;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] current = queue.removeFirst();
            if (current[0] == goal[0] && current[1] == goal[1]) return true;
            for (int[] direction : directions) {
                int col = current[0] + direction[0];
                int row = current[1] + direction[1];
                if (col < 0 || col >= map.getWidth() || row < 0 || row >= map.getHeight()
                        || visited[row][col] || !isWalkableTile(grid[row][col])) continue;
                visited[row][col] = true;
                queue.addLast(new int[]{col, row});
            }
        }
        return false;
    }

    private static boolean isWalkableTile(int tile) {
        return tile == 0 || tile == 3 || tile == 4 || tile == 7 || tile == 8 || tile == 9;
    }
}
