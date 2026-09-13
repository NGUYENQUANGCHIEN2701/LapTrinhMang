package com.tankzors.application;

/**
 * Công thức tính điểm theo PROJECT.md parity-checklist mục 10.
 *
 * <p>{@code score = (hpRemain * 10) + (aiKills * 200) + max(0, 1000 - timeSeconds * 10)}.</p>
 *
 * <p>Giá trị mặc định ({@code aiKills=0}, {@code timeSeconds=0}) cho {@code 1000}
 * điểm. Phase 5 chưa track {@code aiKills} chính xác vì GameState chỉ có
 * score (vốn đã +=100 mỗi tank chết). Khi refactor engine theo phase sau,
 * cần logs riêng {@code killCount}.</p>
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    public static int calculate(int hpRemain, int aiKills, int timeSeconds) {
        int hp = Math.max(0, hpRemain) * 10;
        int kill = Math.max(0, aiKills) * 200;
        int timeBonus = Math.max(0, 1000 - Math.max(0, timeSeconds) * 10);
        return hp + kill + timeBonus;
    }

    /**
     * Variant tiện lợi: lấy {@code aiKills} ước lượng từ server's {@code score}
     * field hiện tại (vì mỗi AI chết server cộng +100 vào score).
     */
    public static int estimateAiKillsFromAccumulatedScore(int serverScore) {
        if (serverScore <= 0) return 0;
        return serverScore / 100;
    }
}
