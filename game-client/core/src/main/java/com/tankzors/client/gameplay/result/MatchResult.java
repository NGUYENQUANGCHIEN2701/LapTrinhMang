package com.tankzors.client.gameplay.result;

/**
 * Kết quả trận đã chốt. Overlay UI và payload REST lấy từ cùng object này.
 * Điểm được tính bằng ScoreCalculator ở client.
 *
 * <p>{@code starsEarned}/{@code coinsEarned} lấy từ {@code MapDefinition.rewards},
 * chỉ khác 0 khi VICTORY. DEFEAT luôn 0. Server clamp lại theo map khi persist
 * (chống client giả mạo), nên đây là giá trị client-reported, không phải sự thật tuyệt đối.</p>
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
    public final int starsEarned;
    public final int coinsEarned;

    public MatchResult(String runId, String mode, String levelId, String mapVersion,
                       String status, int score, int hpRemain, int aiKills, int elapsedSeconds) {
        this(runId, mode, levelId, mapVersion, status, score, hpRemain, aiKills, elapsedSeconds, 0, 0);
    }

    public MatchResult(String runId, String mode, String levelId, String mapVersion,
                       String status, int score, int hpRemain, int aiKills, int elapsedSeconds,
                       int starsEarned, int coinsEarned) {
        this.runId = runId;
        this.mode = mode;
        this.levelId = levelId;
        this.mapVersion = mapVersion;
        this.status = status;
        this.score = score;
        this.hpRemain = hpRemain;
        this.aiKills = aiKills;
        this.elapsedSeconds = elapsedSeconds;
        this.starsEarned = starsEarned;
        this.coinsEarned = coinsEarned;
    }
}
