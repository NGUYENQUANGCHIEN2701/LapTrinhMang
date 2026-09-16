package com.tankzors.client.gameplay.result;

/**
 * Kết quả trận đã chốt. Overlay UI và payload REST lấy từ cùng object này.
 * Điểm được tính bằng ScoreCalculator ở client.
 */
public final class MatchResult {
    public final String runId;
    public final String mode;
    public final String levelId;
    public final String mapVersion;
    public final String status;       // VICTORY / DEFEAT
    public final int score;
    public final int hpRemain;
    public final int aiKills;
    public final int elapsedSeconds;

    public MatchResult(String runId, String mode, String levelId, String mapVersion,
                       String status, int score, int hpRemain, int aiKills, int elapsedSeconds) {
        this.runId = runId;
        this.mode = mode;
        this.levelId = levelId;
        this.mapVersion = mapVersion;
        this.status = status;
        this.score = score;
        this.hpRemain = hpRemain;
        this.aiKills = aiKills;
        this.elapsedSeconds = elapsedSeconds;
    }
}
