package com.tankzors.result;

import com.tankzors.progress.PlayerTotals;
import com.tankzors.progress.PlayerTotalsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test ResultService: reward compute, persistence, idempotency.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>VICTORY campaign → row + player_totals credit.</li>
 *   <li>Same runId + same payload → alreadySaved, totals unchanged (idempotent).</li>
 *   <li>Same runId + different payload → 409 conflict.</li>
 *   <li>DEFEAT → no credit.</li>
 *   <li>Client over-reports → clamp về map rewards.</li>
 *   <li>Two runs different runId same player → totals accumulate.</li>
 * </ul>
 *
 * <p>Dùng profile "test" → H2 in-memory, Flyway V1..V4 apply. Map campaign_1
 * khai báo rewards {stars=3, coins=100} (server resources).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ResultServiceTest {

    @Autowired
    private ResultService resultService;

    @Autowired
    private PlayerTotalsRepository totalsRepo;

    @Autowired
    private MatchResultRepository matchResults;

    @Autowired
    private com.tankzors.leaderboard.LeaderboardRepository leaderboard;

    @Test
    void authenticatedResultStoresFullAccountNameAndDoesNotMergeMatchingInitials() {
        for (String username : new String[]{"name-check-alice", "name-check-andrew"}) {
            String runId = "run-" + username;
            resultService.submit(campaignVictory(runId, "campaign_1", 3, 100), "user:" + username);
            assertEquals(username, matchResults.findByRunId(runId).orElseThrow().getPlayerName());
            assertTrue(leaderboard.findByPlayerNameAndMapId(username, "campaign_1").isPresent());
        }
    }

    private String player;

    @BeforeEach
    void setUp() {
        // Mỗi test dùng player riêng để cô lập totals.
        player = "test-player-" + System.nanoTime();
    }

    // ===== 1. VICTORY → credit =====

    @Test
    void victoryCreditsTotalsAndStoresRewardRow() {
        ResultService.ResultPayload p = campaignVictory("run-v1", "campaign_1", 3, 100);

        ResultService.SubmitOutcome out = resultService.submit(p, player);

        assertFalse(out.alreadySaved, "First submit phải là save mới, không idempotent");
        assertEquals(3, out.view.starsEarned, "Row phải lưu stars theo client");
        assertEquals(100, out.view.coinsEarned);
        assertEquals(3, out.totalStars, "player_totals đã credit stars");
        assertEquals(100, out.totalCoins, "player_totals đã credit coins");
    }

    // ===== 2. Idempotency: same runId + payload → no double credit =====

    @Test
    void retrySameRunIdPayloadDoesNotDoubleCredit() {
        ResultService.ResultPayload p = campaignVictory("run-retry", "campaign_1", 3, 100);
        resultService.submit(p, player);
        long before = totalsStars(player);

        ResultService.SubmitOutcome second = resultService.submit(p, player);

        assertTrue(second.alreadySaved, "Retry cùng payload → alreadySaved=true");
        assertEquals(before, totalsStars(player), "Totals phải KHÔNG đổi khi retry idempotent");
    }

    // ===== 3. Conflict: same runId + different payload =====

    @Test
    void retrySameRunIdDifferentPayloadConflicts() {
        ResultService.ResultPayload p1 = campaignVictory("run-conflict", "campaign_1", 3, 100);
        resultService.submit(p1, player);

        ResultService.ResultPayload p2 = campaignVictory("run-conflict", "campaign_1", 3, 150); // coins khác
        assertThrows(ResultService.ConflictException.class,
                () -> resultService.submit(p2, player),
                "Cùng runId + payload khác → 409");
    }

    // ===== 4. DEFEAT → no credit =====

    @Test
    void defeatDoesNotCredit() {
        ResultService.ResultPayload p = new ResultService.ResultPayload();
        p.runId = "run-defeat";
        p.mode = "CAMPAIGN";
        p.levelId = "campaign_1";
        p.mapVersion = "v1";
        p.status = "DEFEAT";
        p.score = 0;
        p.hpRemain = 0;
        p.aiKills = 0;
        p.elapsedSeconds = 30;
        p.starsEarned = 3;   // client có gửi, nhưng DEFEAT phải bỏ
        p.coinsEarned = 100;

        ResultService.SubmitOutcome out = resultService.submit(p, player);

        assertEquals(0, out.view.starsEarned, "DEFEAT → 0 stars dù client báo");
        assertEquals(0, out.view.coinsEarned);
        assertEquals(0, totalsStars(player), "DEFEAT → totals không tăng");
        assertEquals(0, totalsCoins(player));
    }

    // ===== 5. Clamp: client over-reports =====

    @Test
    void clientOverreportClampedToMapRewards() {
        // campaign_1 map = {3 stars, 100 coins}. Client báo 999/9999.
        ResultService.ResultPayload p = campaignVictory("run-clamp", "campaign_1", 999, 9999);

        ResultService.SubmitOutcome out = resultService.submit(p, player);

        assertEquals(3, out.view.starsEarned, "Server phải clamp stars về max của map");
        assertEquals(100, out.view.coinsEarned, "Server phải clamp coins về max của map");
        assertEquals(3, totalsStars(player), "Totals chỉ nhận giá trị đã clamp");
        assertEquals(100, totalsCoins(player));
    }

    @Test
    void clientUnderreportIsHonored() {
        // Client báo ít hơn map: lấy giá trị client (không inflate).
        ResultService.ResultPayload p = campaignVictory("run-under", "campaign_1", 1, 10);

        ResultService.SubmitOutcome out = resultService.submit(p, player);

        assertEquals(1, out.view.starsEarned, "Client báo < map → giữ nguyên client (không inflate)");
        assertEquals(10, out.view.coinsEarned);
    }

    // ===== 6. Accumulate across runs =====

    @Test
    void twoVictoriesAccumulate() {
        resultService.submit(campaignVictory("run-acc-1", "campaign_1", 3, 100), player);
        resultService.submit(campaignVictory("run-acc-2", "campaign_1", 3, 100), player);

        assertEquals(6, totalsStars(player), "2 VICTORY → 6 stars");
        assertEquals(200, totalsCoins(player), "2 VICTORY → 200 coins");
    }

    // ===== helpers =====

    private static ResultService.ResultPayload campaignVictory(String runId, String levelId,
                                                               int stars, int coins) {
        ResultService.ResultPayload p = new ResultService.ResultPayload();
        p.runId = runId;
        p.mode = "CAMPAIGN";
        p.levelId = levelId;
        p.mapVersion = "v1";
        p.status = "VICTORY";
        p.score = 1000;
        p.hpRemain = 80;
        p.aiKills = 1;
        p.elapsedSeconds = 20;
        p.starsEarned = stars;
        p.coinsEarned = coins;
        return p;
    }

    private int totalsStars(String playerId) {
        return totalsRepo.findByPlayerId(playerId).map(PlayerTotals::getTotalStars).orElse(0);
    }

    private int totalsCoins(String playerId) {
        return totalsRepo.findByPlayerId(playerId).map(PlayerTotals::getTotalCoins).orElse(0);
    }
}
