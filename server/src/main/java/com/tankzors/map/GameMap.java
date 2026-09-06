package com.tankzors.map;

import com.tankzors.entity.Position;
import com.tankzors.entity.Wall;
import com.tankzors.interfaces.IMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triển khai cụ thể của {@link IMap} đại diện cho một bản đồ trong game.
 *
 * <p>Bản đồ được biểu diễn dưới dạng ma trận ô 2 chiều (grid).
 * Tự động tạo danh sách các đối tượng {@link Wall} và các điểm spawn.</p>
 */
public class GameMap implements IMap {

    public static final int DEFAULT_TILE_SIZE = 40;

    private String name;
    private int width;      // Số cột (trục X)
    private int height;     // Số hàng (trục Y)
    private int tileSize;   // Kích thước mỗi ô (pixel)
    private int[][] grid;   // Ma trận [row][col]

    private final List<Wall> walls = new ArrayList<>();
    private final List<Position> spawnPoints = new ArrayList<>();
    private Position playerSpawn;
    private Position enemySpawn;

    public GameMap() {
        this.tileSize = DEFAULT_TILE_SIZE;
    }

    public GameMap(String name, int width, int height, int tileSize, int[][] grid) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize > 0 ? tileSize : DEFAULT_TILE_SIZE;
        this.grid = grid;
        initializeMap();
    }

    /**
     * Khởi tạo và nạp danh sách tường, spawn points từ ma trận grid.
     */
    public void initializeMap() {
        walls.clear();
        spawnPoints.clear();
        playerSpawn = null;
        enemySpawn = null;

        if (grid == null) return;

        for (int row = 0; row < height && row < grid.length; row++) {
            if (grid[row] == null) continue;
            for (int col = 0; col < width && col < grid[row].length; col++) {
                int tileValue = grid[row][col];
                TileType type = TileType.fromValue(tileValue);

                double pixelX = col * tileSize;
                double pixelY = row * tileSize;

                if (type == TileType.WALL) {
                    walls.add(new Wall(pixelX, pixelY, tileSize, tileSize));
                } else if (type == TileType.SPAWN_PLAYER) {
                    playerSpawn = new Position(pixelX + tileSize / 2.0, pixelY + tileSize / 2.0);
                    spawnPoints.add(playerSpawn);
                } else if (type == TileType.SPAWN_ENEMY) {
                    enemySpawn = new Position(pixelX + tileSize / 2.0, pixelY + tileSize / 2.0);
                    spawnPoints.add(enemySpawn);
                }
            }
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getTileSize() {
        return tileSize;
    }

    public String getName() {
        return name;
    }

    public int getPixelWidth() {
        return width * tileSize;
    }

    public int getPixelHeight() {
        return height * tileSize;
    }

    @Override
    public TileType getTileAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height || grid == null || row >= grid.length || col >= grid[row].length) {
            return TileType.WALL; // Ngoài biên coi như là tường
        }
        return TileType.fromValue(grid[row][col]);
    }

    @Override
    public List<Wall> getWalls() {
        return Collections.unmodifiableList(walls);
    }

    @Override
    public List<Position> getSpawnPoints() {
        return Collections.unmodifiableList(spawnPoints);
    }

    public Position getPlayerSpawn() {
        return playerSpawn;
    }

    public Position getEnemySpawn() {
        return enemySpawn;
    }

    @Override
    public boolean isWalkable(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) {
            return false;
        }
        TileType type = getTileAt(col, row);
        return type != TileType.WALL && type != TileType.WATER;
    }

    public int[][] getGrid() {
        return grid;
    }

    public void setGrid(int[][] grid) {
        this.grid = grid;
        initializeMap();
    }
}
