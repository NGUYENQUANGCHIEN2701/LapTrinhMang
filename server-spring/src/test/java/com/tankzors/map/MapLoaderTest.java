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
                assertEquals(16, map.getWidth());
                assertEquals(22, map.getHeight());
                assertEquals(1, countTiles(map, 3), "Màn 1 phải có đúng một player spawn xanh");
                assertEquals(1, countTiles(map, 4), "Màn 1 phải có đúng một AI spawn đỏ");
                assertEquals(1, countTiles(map, 9), "Màn 1 phải có đúng một cờ đích");
                assertEquals(1, countTiles(map, 7), "Màn 1 phải có đúng một Repair như map gốc");
                assertEquals(9, map.getGrid()[3][3], "Cờ phải ở (3,3)");
                assertEquals(3, map.getGrid()[3][11], "Player spawn phải ở (11,3)");
                assertEquals(7, map.getGrid()[13][5], "Repair phải ở (5,13)");
                assertEquals(4, map.getGrid()[19][2], "AI spawn phải ở (2,19)");
                assertNotNull(map.getVisualGrid(), "Màn 1 phải giữ mã tile hình ảnh từ Builds");
                assertEquals(115, map.getVisualGrid()[3][3], "Visual cờ phải giữ mã 115 gốc");
                assertEquals(145, map.getVisualGrid()[8][0], "Visual bụi phải giữ mã 145 gốc");
                assertEquals(164, map.getVisualGrid()[13][5], "Visual Repair phải giữ mã 164 gốc");
                assertTrue(isReachable(map, 3, 9),
                        "Player phải có lộ trình tới cờ sau khi phá vật cản nâu/bụi");
                assertTrue(isReachable(map, 4, 3),
                        "AI phải có lộ trình tới khu player sau khi phá vật cản nâu/bụi");
                assertFalse(map.getEnemies().isEmpty());
            } else {
                // Màn 2/3 chuyển từ Builds 001/002: địch ra theo đợt từ mission.
                assertNotNull(map.getVisualGrid(), levelId + " phải giữ mã tile Builds");
                assertNotNull(map.getMission(), levelId + " phải có kịch bản mission");
                assertTrue(map.getEnemies().isEmpty(), levelId + " không đặt địch tĩnh");
            }
            assertEquals(40, map.getTileSize());
            assertNotNull(map.getGrid());
            assertEquals(map.getHeight(), map.getGrid().length);
            assertNotNull(map.getRewards());
        }
    }

    @Test
    void convertsBuildsLevelsTwoAndThree() {
        GameMap walls = MapLoader.load("campaign_2");
        assertEquals(19, walls.getWidth());
        assertEquals(19, walls.getHeight());
        assertEquals(3, walls.getGrid()[12][1], "Base người chơi Builds ở (1,12)");
        assertEquals(4, walls.getGrid()[3][15], "Base địch thứ nhất ở (15,3)");
        assertEquals(4, walls.getGrid()[15][15], "Base địch thứ hai ở (15,15)");
        assertEquals(DESTROY_TANKS, walls.getMission().get("objective"));
        assertTrue(walls.getMission().containsKey("allies"), "Màn 2 có xe đồng minh");

        GameMap sandbox = MapLoader.load("campaign_3");
        assertEquals(16, sandbox.getWidth());
        assertEquals(22, sandbox.getHeight());
        assertEquals(15, sandbox.getGrid()[12][0], "Khối đá (tile 11 Builds) chặn khoang ụ súng");
        assertEquals("DESTROY_ALL", sandbox.getMission().get("objective"));
        assertEquals(1, ((java.util.List<?>) sandbox.getMission().get("turrets")).size());
    }

    private static final String DESTROY_TANKS = "DESTROY_TANKS";

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
                        || visited[row][col] || !isRouteTile(grid[row][col])) continue;
                visited[row][col] = true;
                queue.addLast(new int[]{col, row});
            }
        }
        return false;
    }

    private static boolean isRouteTile(int tile) {
        // Tường nâu (1) và bụi (6) có thể bị phá để mở đường; nước (2) và
        // tường trắng (5) mới là vật cản vĩnh viễn của lộ trình campaign.
        return tile != 2 && tile != 5;
    }
}
