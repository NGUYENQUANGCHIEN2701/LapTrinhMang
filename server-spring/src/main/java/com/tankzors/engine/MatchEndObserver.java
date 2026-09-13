package com.tankzors.engine;

/**
 * Output port: thông báo khi một trận đấu kết thúc (VICTORY/DEFEAT).
 *
 * <p>Engine không được gọi trực tiếp {@code LeaderboardService} vì sẽ
 * phụ thuộc Spring/JPA, vi phạm nguyên tắc dependency rule.</p>
 *
 * <p>Implementation cụ thể nằm ở layer application: {@code MatchEndService}
 * (submit score + unlock progression).</p>
 *
 * <p>Engine gọi observer KHÔNG đồng bộ - observer phải tự lo thread-safety.</p>
 */
public interface MatchEndObserver {
    /**
     * Callback khi match kết thúc.
     *
     * @param sessionIdSession ID của player (campaign: chỉ có 1 player)
     * @param levelId       level vừa chơi
     * @param status        VICTORY hoặc DEFEAT
     * @param hpRemain      HP còn lại của player (0 nếu chết)
     * @param aiKills       số AI đã hạ trong match
     * @param elapsedSeconds thời gian match kéo dài
     */
    void onMatchEnd(String sessionId, String levelId, String status,
                    int hpRemain, int aiKills, int elapsedSeconds);
}
