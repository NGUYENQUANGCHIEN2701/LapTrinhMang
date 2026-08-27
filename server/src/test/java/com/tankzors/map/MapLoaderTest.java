package com.tankzors.map;

import com.tankzors.entity.Position;
import com.tankzors.entity.Wall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Task 1.8: MapLoader & GameMap.
 */
class MapLoaderTest {

    private GameMap map;

    @BeforeEach
    void setUp() {
        map = MapLoader.load("campaign_1");
        assertNotNull(map, "Bản đồ campaign_1 phải được nạp thành công");
    }

    @Test
    @DisplayName("[AC] MapLoader.load('campaign_1') trả về GameMap không lỗi và đúng kích thước 20x15")
    void loadMap_success_correctDimensions() {
        assertEquals("Màn 1 - Khu vực tập luyện", map.getName());
        assertEquals(20, map.getWidth(), "Chiều rộng bản đồ phải là 20 ô");
        assertEquals(15, map.getHeight(), "Chiều cao bản đồ phải là 15 ô");
        assertEquals(40, map.getTileSize(), "Kích thước mỗi ô là 40px");
        assertEquals(800, map.getPixelWidth(), "Kích thước pixel rộng = 800px");
        assertEquals(600, map.getPixelHeight(), "Kích thước pixel cao = 600px");
    }

    @Test
    @DisplayName("[AC] map.getTileAt(0, 0) trả về WALL")
    void getTileAt_corner_isWall() {
        assertEquals(TileType.WALL, map.getTileAt(0, 0), "Ô (0,0) phải là WALL");
        assertEquals(TileType.WALL, map.getTileAt(19, 0), "Ô góc trên phải phải là WALL");
        assertEquals(TileType.WALL, map.getTileAt(0, 14), "Ô góc dưới trái phải là WALL");
        assertEquals(TileType.WALL, map.getTileAt(19, 14), "Ô góc dưới phải phải là WALL");
    }

    @Test
    @DisplayName("[AC] map.isWalkable(1, 1) trả về true và isWalkable(0, 0) trả về false")
    void isWalkable_correctValues() {
        assertTrue(map.isWalkable(1, 1), "Ô (1,1) là đất trống nên đi được");
        assertFalse(map.isWalkable(0, 0), "Ô (0,0) là tường nên không đi được");
        assertFalse(map.isWalkable(-1, 0), "Ô ngoài biên âm không đi được");
        assertFalse(map.isWalkable(20, 15), "Ô vượt quá kích thước không đi được");
    }

    @Test
    @DisplayName("[AC] map.getSpawnPoints() trả về đúng các vị trí xuất phát")
    void getSpawnPoints_correctPositions() {
        List<Position> spawnPoints = map.getSpawnPoints();
        assertEquals(2, spawnPoints.size(), "Bản đồ phải có 2 điểm spawn (player và enemy)");

        Position playerSpawn = map.getPlayerSpawn();
        assertNotNull(playerSpawn, "Phải có player spawn point");
        // Ô (2, 2) -> pixel (2*40, 2*40) = (80, 80)
        assertEquals(80.0, playerSpawn.getX(), 0.001);
        assertEquals(80.0, playerSpawn.getY(), 0.001);

        Position enemySpawn = map.getEnemySpawn();
        assertNotNull(enemySpawn, "Phải có enemy spawn point");
        // Ô (16, 11) -> pixel (16*40, 11*40) = (640, 440)
        assertEquals(640.0, enemySpawn.getX(), 0.001);
        assertEquals(440.0, enemySpawn.getY(), 0.001);
    }

    @Test
    @DisplayName("[AC] map.getWalls() trả về danh sách Wall với BoundingBox đúng")
    void getWalls_returnsValidWallEntities() {
        List<Wall> walls = map.getWalls();
        assertNotNull(walls);
        assertTrue(walls.size() > 50, "Bản đồ phải có nhiều ô tường");

        // Kiểm tra 1 tường bất kỳ
        Wall firstWall = walls.get(0);
        assertNotNull(firstWall.getBoundingBox());
        assertEquals(40.0, firstWall.getWidth(), 0.001);
        assertEquals(40.0, firstWall.getHeight(), 0.001);
        assertEquals(40.0, firstWall.getBoundingBox().getWidth(), 0.001);
    }

    @Test
    @DisplayName("Nạp bản đồ không tồn tại trả về null an toàn")
    void loadNonExistentMap_returnsNull() {
        GameMap nonExistent = MapLoader.load("non_existent_map");
        assertNull(nonExistent, "Bản đồ không tồn tại phải trả về null");
    }
}
