package com.tankzors.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data contract for a campaign map returned to the LibGDX client.
 *
 * <p>The Spring server stores and validates map metadata only. Gameplay entities,
 * collision data and simulation state are built inside {@code game-client}.</p>
 */
public class GameMap {

    public static final int DEFAULT_TILE_SIZE = 40;

    private String name;
    private String difficulty;
    private int width;
    private int height;
    private int tileSize = DEFAULT_TILE_SIZE;
    private int[][] grid;
    /** Optional source visual tile IDs consumed only by the LibGDX renderer. */
    private int[][] visualGrid;
    private List<EnemyConfig> enemies = new ArrayList<>();
    private RewardsConfig rewards;
    /**
     * Kịch bản màn (đợt địch, đồng minh, ụ súng, trigger, briefing). Spring chỉ
     * chuyển nguyên khối này cho client; gameplay diễn giải trong game-client.
     */
    private Map<String, Object> mission;

    public static class EnemyConfig {
        private String type = "AI";
        private String difficulty = "EASY";
        private int spawnX;
        private int spawnY;

        public EnemyConfig() {}

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

        public int getStars() { return stars; }
        public void setStars(int stars) { this.stars = stars; }
        public int getCoins() { return coins; }
        public void setCoins(int coins) { this.coins = coins; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getTileSize() { return tileSize; }
    public void setTileSize(int tileSize) {
        this.tileSize = tileSize > 0 ? tileSize : DEFAULT_TILE_SIZE;
    }
    public int[][] getGrid() { return grid; }
    public void setGrid(int[][] grid) { this.grid = grid; }
    public int[][] getVisualGrid() { return visualGrid; }
    public void setVisualGrid(int[][] visualGrid) { this.visualGrid = visualGrid; }
    public List<EnemyConfig> getEnemies() { return enemies; }
    public void setEnemies(List<EnemyConfig> enemies) {
        this.enemies = enemies == null ? new ArrayList<>() : enemies;
    }
    public RewardsConfig getRewards() { return rewards; }
    public void setRewards(RewardsConfig rewards) { this.rewards = rewards; }
    public Map<String, Object> getMission() { return mission; }
    public void setMission(Map<String, Object> mission) { this.mission = mission; }
}
