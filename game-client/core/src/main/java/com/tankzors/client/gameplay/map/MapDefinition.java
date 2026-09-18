package com.tankzors.client.gameplay.map;

import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bản đồ dạng lưới ô 2D. Port từ server map.GameMap: logic initializeMap() giữ
 * nguyên byte-for-byte. Bỏ Gson reflection (dùng MapLoader riêng với JsonReader).
 */
public class MapDefinition implements IMap {

    public static final int DEFAULT_TILE_SIZE = 40;

    private String name;
    private String difficulty;
    private int width;
    private int height;
    private int tileSize;
    private int[][] grid;
    private List<EnemyConfig> enemies = new ArrayList<>();
    private RewardsConfig rewards;

    private final List<Wall> walls = new ArrayList<>();
    private final List<Position> spawnPoints = new ArrayList<>();
    private Position playerSpawn;
    private Position enemySpawn;

    public static class EnemyConfig {
        private String type = "AI";
        private String difficulty = "EASY";
        private int spawnX;
        private int spawnY;

        public EnemyConfig() {}
        public EnemyConfig(String type, String difficulty, int spawnX, int spawnY) {
            this.type = type; this.difficulty = difficulty; this.spawnX = spawnX; this.spawnY = spawnY;
        }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public int getSpawnX() { return spawnX; }
        public void setSpawnX(int spawnX) { this.spawnX = spawnX; }
        public int getSpawnY() { return spawnY; }
        public void setSpawnY(int spawnY) { this.spawnY = spawnY; }
    }

    public static class RewardsConfig {
        private int stars;
        private int coins;
        public RewardsConfig() {}
        public RewardsConfig(int stars, int coins) { this.stars = stars; this.coins = coins; }
        public int getStars() { return stars; }
        public void setStars(int stars) { this.stars = stars; }
        public int getCoins() { return coins; }
        public void setCoins(int coins) { this.coins = coins; }
    }

    public MapDefinition() { this.tileSize = DEFAULT_TILE_SIZE; }

    public MapDefinition(String name, int width, int height, int tileSize, int[][] grid) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize > 0 ? tileSize : DEFAULT_TILE_SIZE;
        this.grid = grid;
        initializeMap();
    }

    /** Dựng walls + spawn points từ grid. Giữ nguyên logic server. */
    public void initializeMap() {
        walls.clear();
        spawnPoints.clear();
        playerSpawn = null;
        enemySpawn = null;

        if (grid == null) return;

        if (height <= 0) height = grid.length;
        if (width <= 0 && grid.length > 0 && grid[0] != null) width = grid[0].length;
        if (tileSize <= 0) tileSize = DEFAULT_TILE_SIZE;

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

        if (enemies != null) {
            for (EnemyConfig e : enemies) {
                Position ep = new Position((e.getSpawnX() + 0.5) * tileSize, (e.getSpawnY() + 0.5) * tileSize);
                if (enemySpawn == null) enemySpawn = ep;
                boolean alreadyAdded = false;
                for (Position p : spawnPoints) {
                    if (Math.hypot(p.getX() - ep.getX(), p.getY() - ep.getY()) < 1e-3) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) spawnPoints.add(ep);
            }
        }
    }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public List<EnemyConfig> getEnemies() { return enemies; }
    public void setEnemies(List<EnemyConfig> enemies) { this.enemies = enemies; }
    public RewardsConfig getRewards() { return rewards; }
    public void setRewards(RewardsConfig rewards) { this.rewards = rewards; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    @Override public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    @Override public int getTileSize() { return tileSize; }
    public void setTileSize(int tileSize) { this.tileSize = tileSize; }

    public Position getPlayerSpawn() { return playerSpawn; }
    public Position getEnemySpawn() { return enemySpawn; }

    @Override
    public TileType getTileAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height || grid == null
                || row >= grid.length || col >= grid[row].length) {
            return TileType.WALL; // Ngoài biên = tường
        }
        return TileType.fromValue(grid[row][col]);
    }

    @Override public List<Wall> getWalls() { return Collections.unmodifiableList(walls); }
    @Override public List<Position> getSpawnPoints() { return Collections.unmodifiableList(spawnPoints); }

    @Override
    public boolean isWalkable(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return false;
        TileType type = getTileAt(col, row);
        return type != TileType.WALL && type != TileType.WATER;
    }

    public int[][] getGrid() { return grid; }
    public void setGrid(int[][] grid) { this.grid = grid; initializeMap(); }
}
