package com.tankzors.client.gameplay.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm tra MatchResult mang reward (stars/coins) và backward-compat ctor (0).
 * Tách khỏi LocalCampaignSessionTest để isolate payload contract.
 */
class MatchResultRewardTest {

    @Test
    void rewardCtorCarriesStarsAndCoins() {
        MatchResult r = new MatchResult(
                "run-1", "CAMPAIGN", "campaign_1", "v1", "VICTORY",
                1700, 75, 2, 45, 3, 100);
        assertEquals(3, r.starsEarned, "starsEarned phải khớp giá trị truyền vào");
        assertEquals(100, r.coinsEarned, "coinsEarned phải khớp giá trị truyền vào");
        assertEquals("run-1", r.runId);
        assertEquals(1700, r.score);
    }

    @Test
    void compatCtorDefaultsRewardToZero() {
        // ctor cũ (10 tham số) — dùng ở các call site chưa khai báo reward.
        MatchResult r = new MatchResult(
                "run-2", "CAMPAIGN", "campaign_1", "v1", "DEFEAT",
                0, 0, 1, 30);
        assertEquals(0, r.starsEarned, "ctor cũ phải default reward = 0");
        assertEquals(0, r.coinsEarned);
    }

    @Test
    void defeatReportCarriesZeroReward() {
        // Session set 0 cho DEFEAT; ctor reward vẫn verify được giá trị truyền.
        MatchResult r = new MatchResult(
                "run-3", "CAMPAIGN", "campaign_1", "v1", "DEFEAT",
                0, 0, 1, 30, 0, 0);
        assertEquals(0, r.starsEarned);
        assertEquals(0, r.coinsEarned);
    }
}
