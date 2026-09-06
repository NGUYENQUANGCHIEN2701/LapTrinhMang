package com.tankzors.map;

import com.tankzors.ai.DifficultyLevel;
import com.tankzors.engine.GameLoop;
import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;
import com.tankzors.entity.Position;
import com.tankzors.network.protocol.InputMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CampaignMapsTest {

    @Test
    @DisplayName("MapLoader đọc thành công cả 3 bản đồ Campaign")
    public void testLoadAllCampaignMaps() {
        GameMap map1 = MapLoader.load("campaign_1");
        GameMap map2 = MapLoader.load("campaign_2");
        GameMap map3 = MapLoader.load("campaign_3");

        assertNotNull(map1, "MapLoader phải nạp được campaign_1");
        assertNotNull(map2, "MapLoader phải nạp được campaign_2");
        assertNotNull(map3, "MapLoader phải nạp được campaign_3");

        assertEquals(20, map1.getWidth());
        assertEquals(15, map1.getHeight());
        assertEquals(20, map2.getWidth());
        assertEquals(15, map2.getHeight());
        assertEquals(20, map3.getWidth());
        assertEquals(15, map3.getHeight());
    }

    @Test
    @DisplayName("Kiểm tra metadata: difficulty, enemies, rewards của từng màn")
    public void testCampaignMetadata() {
        GameMap map1 = MapLoader.load("campaign_1");
        assertNotNull(map1);
        assertEquals("EASY", map1.getDifficulty());
        assertNotNull(map1.getRewards());
        assertEquals(3, map1.getRewards().getStars());
        assertEquals(100, map1.getRewards().getCoins());
        assertEquals(1, map1.getEnemies().size(), "Màn 1 phải có 1 AI");
        assertEquals("EASY", map1.getEnemies().get(0).getDifficulty());

        GameMap map2 = MapLoader.load("campaign_2");
        assertNotNull(map2);
        assertEquals("MEDIUM", map2.getDifficulty());
        assertNotNull(map2.getRewards());
        assertEquals(3, map2.getRewards().getStars());
        assertEquals(200, map2.getRewards().getCoins());
        assertEquals(2, map2.getEnemies().size(), "Màn 2 phải có 2 AI");
        for (var e : map2.getEnemies()) {
            assertEquals("MEDIUM", e.getDifficulty());
        }

        GameMap map3 = MapLoader.load("campaign_3");
        assertNotNull(map3);
        assertEquals("HARD", map3.getDifficulty());
        assertNotNull(map3.getRewards());
        assertEquals(3, map3.getRewards().getStars());
        assertEquals(300, map3.getRewards().getCoins());
        assertEquals(3, map3.getEnemies().size(), "Màn 3 phải có 3 AI");
        for (var e : map3.getEnemies()) {
            assertEquals("HARD", e.getDifficulty());
        }
    }

    @Test
    @DisplayName("Kiểm tra tính liên thông: Không có ô trống nào bị cô lập (BFS Flood-fill)")
    public void testNoIsolatedCellsInAllMaps() {
        for (String mapName : List.of("campaign_1", "campaign_2", "campaign_3")) {
            GameMap map = MapLoader.load(mapName);
            assertNotNull(map);

            Position playerSpawn = map.getPlayerSpawn();
            assertNotNull(playerSpawn, mapName + " phải có điểm spawn của người chơi");

            int startCol = (int) Math.floor(playerSpawn.getX() / map.getTileSize());
            int startRow = (int) Math.floor(playerSpawn.getY() / map.getTileSize());

            // BFS flood fill từ player spawn
            boolean[][] visited = new boolean[map.getHeight()][map.getWidth()];
            Queue<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{startCol, startRow});
            visited[startRow][startCol] = true;

            int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

            while (!queue.isEmpty()) {
                int[] curr = queue.poll();
                int c = curr[0];
                int r = curr[1];

                for (int[] d : directions) {
                    int nc = c + d[0];
                    int nr = r + d[1];

                    if (nc >= 0 && nc < map.getWidth() && nr >= 0 && nr < map.getHeight()) {
                        if (!visited[nr][nc] && map.isWalkable(nc, nr)) {
                            visited[nr][nc] = true;
                            queue.add(new int[]{nc, nr});
                        }
                    }
                }
            }

            // Đảm bảo toàn bộ các ô walkable (GROUND, SPAWN_PLAYER, SPAWN_ENEMY) đều đã được ghé thăm
            List<String> unreachable = new ArrayList<>();
            for (int r = 0; r < map.getHeight(); r++) {
                for (int c = 0; c < map.getWidth(); c++) {
                    if (map.isWalkable(c, r) && !visited[r][c]) {
                        unreachable.add(String.format("(col=%d, row=%d, tile=%s)", c, r, map.getTileAt(c, r)));
                    }
                }
            }

            assertTrue(unreachable.isEmpty(),
                    String.format("%s có ô trống bị cô lập không tiếp cận được: %s", mapName, unreachable));
        }
    }

    @Test
    @DisplayName("Vị trí spawn của Player và AI không đè lên tường hay nước")
    public void testSpawnsAreValid() {
        for (String mapName : List.of("campaign_1", "campaign_2", "campaign_3")) {
            GameMap map = MapLoader.load(mapName);
            assertNotNull(map);

            Position pSpawn = map.getPlayerSpawn();
            assertNotNull(pSpawn);
            int pCol = (int) Math.floor(pSpawn.getX() / map.getTileSize());
            int pRow = (int) Math.floor(pSpawn.getY() / map.getTileSize());
            assertTrue(map.isWalkable(pCol, pRow), mapName + " player spawn phải đi được");

            for (var enemy : map.getEnemies()) {
                assertTrue(map.isWalkable(enemy.getSpawnX(), enemy.getSpawnY()),
                        String.format("%s enemy spawn (%d, %d) phải đi được", mapName, enemy.getSpawnX(), enemy.getSpawnY()));
                assertFalse(pCol == enemy.getSpawnX() && pRow == enemy.getSpawnY(),
                        mapName + " enemy spawn không được trùng player spawn");
            }
        }
    }

    @Test
    @DisplayName("GameLoop spawn đúng số lượng AI và đúng độ khó theo metadata của từng map")
    public void testGameLoopSpawnsAIFromMetadata() {
        // Test campaign_2 (2 MEDIUM AI)
        GameState state2 = new GameState();
        GameMap map2 = MapLoader.load("campaign_2");
        state2.setCurrentMap(map2);

        GameLoop loop2 = new GameLoop(state2, null);
        InputMessage join2 = new InputMessage("JOIN_GAME");
        join2.setMode("campaign");
        join2.setLevelId("campaign_2");
        loop2.applyInput("p2", join2);

        List<AITank> aiList2 = state2.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(t -> (AITank) t)
                .toList();
        assertEquals(2, aiList2.size(), "campaign_2 phải spawn 2 AI");
        for (AITank ai : aiList2) {
            assertEquals(DifficultyLevel.MEDIUM, ai.getDifficulty(), "Độ khó AI campaign_2 phải là MEDIUM");
        }

        // Test campaign_3 (3 HARD AI)
        GameState state3 = new GameState();
        GameMap map3 = MapLoader.load("campaign_3");
        state3.setCurrentMap(map3);

        GameLoop loop3 = new GameLoop(state3, null);
        InputMessage join3 = new InputMessage("JOIN_GAME");
        join3.setMode("campaign");
        join3.setLevelId("campaign_3");
        loop3.applyInput("p3", join3);

        List<AITank> aiList3 = state3.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(t -> (AITank) t)
                .toList();
        assertEquals(3, aiList3.size(), "campaign_3 phải spawn 3 AI");
        for (AITank ai : aiList3) {
            assertEquals(DifficultyLevel.HARD, ai.getDifficulty(), "Độ khó AI campaign_3 phải là HARD");
        }
    }
}
