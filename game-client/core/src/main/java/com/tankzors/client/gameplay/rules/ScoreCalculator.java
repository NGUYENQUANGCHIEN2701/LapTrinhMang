package com.tankzors.client.gameplay.rules;

/**
 * Công thức điểm campaign. Parity server application.ScoreCalculator.
 * score = hp*10 + aiKills*200 + max(0, 1000 - time*10). Mỗi hạng clamp ≥0.
 * Chỉ dùng cho VICTORY. Di chuyển từ server sang client (client-reported).
 */
public final class ScoreCalculator {
    private ScoreCalculator() {}

    public static int calculate(int hpRemain, int aiKills, int timeSeconds) {
        int hp = Math.max(0, hpRemain) * 10;
        int kill = Math.max(0, aiKills) * 200;
        int timeBonus = Math.max(0, 1000 - Math.max(0, timeSeconds) * 10);
        return hp + kill + timeBonus;
    }
}
