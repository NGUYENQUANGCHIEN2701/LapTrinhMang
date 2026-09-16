package com.tankzors.application;

/**
 * View model nhẹ của một level campaign dùng cho Thymeleaf page
 * (campaign selection / play). Chỉ chứa metadata, không chứa
 * {@code GameState} hay dữ liệu realtime.
 */
public class CampaignLevelView {

    private final String id;
    private final String name;
    private final String difficulty;
    private final String difficultyLabel;
    private final int enemiesCount;
    private final String description;

    public CampaignLevelView(String id, String name, String difficulty,
                             String difficultyLabel, int enemiesCount, String description) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.difficultyLabel = difficultyLabel;
        this.enemiesCount = enemiesCount;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDifficulty() { return difficulty; }
    public String getDifficultyLabel() { return difficultyLabel; }
    public int getEnemiesCount() { return enemiesCount; }
    public String getDescription() { return description; }
}
