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
    /** Mã tile hình ảnh từ map Builds gốc; không tham gia collision/gameplay. */
    private int[][] visualGrid;
    private List<EnemyConfig> enemies = new ArrayList<>();
    private RewardsConfig rewards;
    /** Kịch bản màn Builds (đợt địch, đồng minh, ụ súng, trigger); null với map cũ. */
    private MissionConfig mission;

    private final List<com.tankzors.client.gameplay.entity.Building> buildings = new ArrayList<>();
    public List<com.tankzors.client.gameplay.entity.Building> getBuildings() { return buildings; }

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

                if (type == TileType.WALL || type == TileType.WHITE_WALL || type == TileType.BUSH
                        || type == TileType.STONE_WALL) {
                    Wall wall = new Wall(pixelX, pixelY, tileSize, tileSize, col, row, type);
                    int legacy = legacyWallId(col, row);
                    if (type == TileType.WALL && legacy >= 1 && legacy <= 8) {
                        wall.hit((legacy - 1) / 2);
                    }
                    walls.add(wall);
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
        for (com.tankzors.client.gameplay.entity.Building building : buildings) if (building.contains(col, row)) return false;
        TileType type = getTileAt(col, row);
        return type != TileType.WALL && type != TileType.WHITE_WALL
                && type != TileType.BUSH && type != TileType.WATER && type != TileType.STONE_WALL;
    }

    /**
     * Builds gán speedRatio theo id nền (Tilemap props): cát 0.45, đường 1.2,
     * tuyết 1.0, còn lại (cỏ) 0.8. Tốc độ engine được chỉnh cho cỏ nên trả tỉ lệ so với 0.8.
     */
    @Override
    public double speedFactorAt(double pixelX, double pixelY) {
        int col = (int) Math.floor(pixelX / tileSize);
        int row = (int) Math.floor(pixelY / tileSize);
        if (visualGrid == null || row < 0 || row >= visualGrid.length || visualGrid[row] == null
                || col < 0 || col >= visualGrid[row].length) {
            return 1.0;
        }
        return groundSpeedRatio(groundIdForCode(visualGrid[row][col])) / 0.8;
    }

    /** Id nền 1-based mà OldMap.parseMap của Builds gán cho một mã map_arr. */
    public static int groundIdForCode(int code) {
        if (code > 0 && code < 91) return code;
        if (code >= 250) return code - 149;
        int[] theme = {1, 34, 56};
        if (code >= 100 && code < 115) return theme[Math.min(2, (code - 100) / 5)];
        if (code >= 115 && code < 130) return theme[Math.min(2, (code - 115) / 5)];
        if (code >= 130 && code < 250) return theme[Math.min(2, (code - 130) / 40)];
        return 1;
    }

    static double groundSpeedRatio(int groundId) {
        if (groundId >= 11 && groundId <= 14) return 0.45;
        if (groundId >= 24 && groundId <= 33) return 1.2;
        if (groundId >= 34 && groundId <= 52) return 1.0;
        if (groundId >= 56 && groundId <= 90) return 0.45;
        if (groundId >= 91 && groundId <= 100) return 1.2;
        return 0.8;
    }

    /** Ô nền cát (Builds kéo dài tăng tốc gấp đôi khi xe đứng trên cát). */
    public boolean isSandAt(double pixelX, double pixelY) {
        int col = (int) Math.floor(pixelX / tileSize);
        int row = (int) Math.floor(pixelY / tileSize);
        if (visualGrid == null || row < 0 || row >= visualGrid.length || visualGrid[row] == null
                || col < 0 || col >= visualGrid[row].length) {
            return false;
        }
        int ground = groundIdForCode(visualGrid[row][col]);
        return ground >= 56 && ground <= 90;
    }

    /** Ghi một ô động (phá vật cản/nhặt vật phẩm) mà không reset HP các tường khác. */
    public void setTileAt(int col, int row, TileType type) {
        if (grid == null || row < 0 || row >= grid.length || col < 0 || col >= grid[row].length) return;
        grid[row][col] = (type != null ? type : TileType.GROUND).getValue();
    }

    /** Áp damage của projectile lên vật cản; trả loại vừa bị phá hoặc null. */
    public TileType hitWall(Wall wall, int damage) {
        if (wall == null || !walls.contains(wall) || isBoundaryWall(wall) || !wall.hit(damage)) return null;
        TileType destroyed = wall.getTileType();
        walls.remove(wall);
        setTileAt(wall.getCol(), wall.getRow(), TileType.GROUND);
        return destroyed;
    }

    /** Tương thích cho các lời gọi cũ: áp một damage point. */
    public TileType hitWall(Wall wall) {
        return hitWall(wall, 1);
    }

    /** Hàng/cột ngoài cùng là tường bao map: giữ nguyên sprite nhưng không thể phá. */
    private boolean isBoundaryWall(Wall wall) {
        return wall.getCol() == 0 || wall.getRow() == 0
                || wall.getCol() == width - 1 || wall.getRow() == height - 1;
    }

    /** Nhặt vật phẩm tại tâm xe; trả loại pickup hoặc null. */
    public TileType collectPickupAt(double pixelX, double pixelY) {
        int col = (int) Math.floor(pixelX / tileSize);
        int row = (int) Math.floor(pixelY / tileSize);
        TileType type = getTileAt(col, row);
        if (!type.isPickup()) return null;
        setTileAt(col, row, TileType.GROUND);
        return type;
    }

    /** Loại pickup tại ô mà không nhặt (Builds giữ Repair khi xe còn đầy máu). */
    public TileType pickupAt(double pixelX, double pixelY) {
        TileType type = getTileAt((int) Math.floor(pixelX / tileSize), (int) Math.floor(pixelY / tileSize));
        return type.isPickup() ? type : null;
    }

    /** Vụ nổ xoá hẳn một vật cản (khối đá, bụi, gạch) khỏi lưới. */
    public void removeWall(Wall wall) {
        if (wall == null || !walls.remove(wall)) return;
        setTileAt(wall.getCol(), wall.getRow(), TileType.GROUND);
    }

    public Wall wallAt(int col, int row) {
        for (Wall wall : walls) {
            if (wall.getCol() == col && wall.getRow() == row) return wall;
        }
        return null;
    }

    public boolean isBoundary(int col, int row) {
        return col == 0 || row == 0 || col == width - 1 || row == height - 1;
    }

    public MissionConfig getMission() { return mission; }
    public void setMission(MissionConfig mission) { this.mission = mission; }

    public int[][] getGrid() { return grid; }
    public void setGrid(int[][] grid) { this.grid = grid; initializeMap(); }
    /** Original overlay identity distinguishes concrete from armored steel. */
    public int legacyWallId(int col, int row) {
        if (visualGrid == null || row < 0 || row >= visualGrid.length || visualGrid[row] == null
                || col < 0 || col >= visualGrid[row].length) return 0;
        int code = visualGrid[row][col];
        return code >= 130 && code < 250 ? (code - 130) % 40 + 1 : 0;
    }

    public int[][] getVisualGrid() { return visualGrid; }
    public void setVisualGrid(int[][] visualGrid) { this.visualGrid = visualGrid; }
}
