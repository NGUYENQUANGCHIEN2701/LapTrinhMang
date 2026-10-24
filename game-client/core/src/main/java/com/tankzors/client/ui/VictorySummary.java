package com.tankzors.client.ui;

import com.tankzors.client.model.GameStateSnapshot;

/** Presentation values come from the completed match, never placeholder statistics. */
public final class VictorySummary {
    private VictorySummary() {}

    public static String time(int seconds) {
        int value = Math.max(0, seconds);
        return (value / 60) + ":" + (value % 60 < 10 ? "0" : "") + value % 60;
    }

    public static String[][] rows(GameStateSnapshot state) {
        return new String[][] {
            {"Xu nhận được", "+" + Math.max(0, state.coinsEarned)},
            {"Sao nhận được", "+" + Math.max(0, state.starsEarned)},
            {"Xu hiện có", "" + Math.max(0, state.playerCoins)},
            {"Sao hiện có", "" + Math.max(0, state.playerStars)},
            {"Thời gian nhiệm vụ", time(state.elapsedSeconds)},
            {"Xe địch đã tiêu diệt", "" + Math.max(0, state.aiKills)},
            {"Máu còn lại", Math.max(0, state.hpRemain) + " HP"},
            {"Điểm nhiệm vụ", "" + Math.max(0, state.score)}
        };
    }
}
