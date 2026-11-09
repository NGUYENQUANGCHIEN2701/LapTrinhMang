package com.tankzors.result;

import com.tankzors.application.PlayerProgressService;
import com.tankzors.progress.PlayerTotalsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ResultServiceTest {
    @Autowired ResultService results;
    @Autowired GameRunService runs;
    @Autowired GameRunRepository runRows;
    @Autowired PlayerProgressService progress;
    @Autowired PlayerTotalsRepository totals;
    @Autowired MatchResultRepository rows;
    @Autowired com.tankzors.leaderboard.LeaderboardRepository leaderboard;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.tankzors.application.PlayerShopService shop;
    private String player() { return "user:result-" + UUID.randomUUID().toString().substring(0, 12); }
    private ResultService.ResultPayload payload(String run, String level) {
        var p = new ResultService.ResultPayload(); p.runId = run; p.mode = "CAMPAIGN"; p.levelId = level;
        p.mapVersion = "v1"; p.status = "VICTORY"; p.score = 1000; p.hpRemain = 5; p.aiKills = 1;
        p.elapsedSeconds = 20; p.starsEarned = 3; p.coinsEarned = 100; return p;
    }
    private ResultService.ResultPayload issued(String player) {
        return payload(runs.issue(player, "campaign_1").getRunId(), "campaign_1");
    }
    @Test void victoryCreditsWalletAndProgressAtomically() {
        String player = player(); var p = issued(player); var out = results.submit(p, player);
        assertFalse(out.alreadySaved); assertEquals(100, out.totalCoins); assertEquals(3, out.totalStars);
        assertEquals(java.util.List.of("campaign_2"), out.unlockedLevels);
        assertNotNull(runRows.findById(p.runId).orElseThrow().getFinishedAt());
        assertEquals(player.substring(5), rows.findByRunId(p.runId).orElseThrow().getPlayerName());
    }
    @Test void retryReturnsCurrentMetadataAndNeverCreditsTwice() {
        String player = player(); var p = issued(player); var first = results.submit(p, player);
        var retry = results.submit(p, player);
        assertTrue(retry.alreadySaved); assertEquals(first.totalCoins, retry.totalCoins);
        assertEquals(first.totalStars, retry.totalStars); assertEquals(first.unlockedLevels, retry.unlockedLevels);
        assertEquals(first.bestScore, retry.bestScore); assertEquals(first.revision, retry.revision);
        assertEquals(1, rows.findAll().stream().filter(row -> p.runId.equals(row.getRunId())).count());
    }
    @Test void differentPayloadConflicts() {
        String player = player(); var p = issued(player); results.submit(p, player); p.score++;
        assertThrows(ResultService.ConflictException.class, () -> results.submit(p, player));
    }
    @Test void inventedRunCannotUnlockOrCredit() {
        String player = player(); var p = payload("invented-" + UUID.randomUUID(), "campaign_1");
        assertThrows(IllegalArgumentException.class, () -> results.submit(p, player));
        assertTrue(rows.findByRunId(p.runId).isEmpty());
    }
    @Test void anotherAccountCannotSubmitOrReplayAnOwnedRun() {
        String a = player(), b = player(); var p = issued(a);
        assertThrows(ResultService.ConflictException.class, () -> results.submit(p, b));
        results.submit(p, a);
        assertThrows(ResultService.ConflictException.class, () -> results.submit(p, b));
    }
    @Test void mismatchedLevelAndVersionAreRejected() {
        String player = player(); var p = issued(player); p.mapVersion = "other";
        assertThrows(IllegalArgumentException.class, () -> results.submit(p, player));
        p.mapVersion = "v1"; p.levelId = "campaign_3";
        assertThrows(IllegalArgumentException.class, () -> results.submit(p, player));
    }
    @Test void secondIssuedRunCannotWinCompletedLevel() {
        String player = player(); var first = issued(player); var second = issued(player);
        results.submit(first, player);
        assertThrows(IllegalArgumentException.class, () -> results.submit(second, player));
        assertTrue(rows.findByRunId(second.runId).isEmpty());
        assertEquals(100, totals.findByPlayerId(player).orElseThrow().getTotalCoins());
    }
    @Test void defeatHasNoRewardEvenWhenClientAsksForOne() {
        String player = player(); var p = issued(player); p.status = "DEFEAT";
        var out = results.submit(p, player);
        assertEquals(0, out.totalCoins); assertEquals(0, out.view.starsEarned);
        assertEquals(java.util.List.of("campaign_1"), out.unlockedLevels);
    }
    @Test void rewardIsDerivedFromMapRatherThanClientClaim() {
        String player = player(); var p = issued(player); p.starsEarned = 999; p.coinsEarned = 99999;
        var out = results.submit(p, player); assertEquals(3, out.view.starsEarned); assertEquals(100, out.view.coinsEarned);
    }
    @Test void invalidLengthAndModeFailBeforeDatabaseInsert() {
        String player = player(); var p = issued(player); p.runId = "x".repeat(121);
        assertThrows(IllegalArgumentException.class, () -> results.submit(p, player));
        var invalid = issued(player); invalid.mode = "COOP";
        assertThrows(IllegalArgumentException.class, () -> results.submit(invalid, player));
    }
    @Test void concurrentSameRunReturnsSavedAndIdempotentWithoutUniqueFailure() throws Exception {
        String player = player(); var p = issued(player); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<ResultService.SubmitOutcome> task = () -> { start.await(); return results.submit(p, player); };
            var a = pool.submit(task); var b = pool.submit(task); start.countDown();
            var first = a.get(15, TimeUnit.SECONDS); var second = b.get(15, TimeUnit.SECONDS);
            assertNotEquals(first.alreadySaved, second.alreadySaved);
            assertEquals(100, first.totalCoins); assertEquals(100, second.totalCoins);
        }
    }
    @Test void failedValidationLeavesWalletAndProgressUntouched() {
        String player = player(); var p = issued(player); p.score = -1;
        assertThrows(IllegalArgumentException.class, () -> results.submit(p, player));
        assertTrue(rows.findByRunId(p.runId).isEmpty());
        assertEquals(0, totals.findByPlayerId(player).orElseThrow().getTotalCoins());
        assertEquals(java.util.List.of("campaign_1"), progress.getUnlocked(player));
    }
    @Test void expiredIssuedRunCannotBeSubmitted() {
        String player = player(); var p = issued(player);
        jdbc.update("UPDATE game_run SET expires_at = ? WHERE run_id = ?", java.sql.Timestamp.from(java.time.Instant.EPOCH), p.runId);
        assertThrows(IllegalArgumentException.class, () -> results.submit(p, player));
        assertTrue(rows.findByRunId(p.runId).isEmpty());
    }
    @Test void interestUsesServerResearchAndIsCreditedExactlyOnce() {
        String player = player(); var p = issued(player); shop.getShopData(player);
        var wallet = totals.findByPlayerId(player).orElseThrow(); wallet.setTotalStars(20); totals.save(wallet);
        shop.syncShopData(player, new com.tankzors.application.PlayerShopService.SyncRequest(p.runId,
                UUID.randomUUID().toString(), 0L, java.util.List.of(new com.tankzors.application.PlayerShopService.Operation(
                        "RESEARCH", null, "extraInterest", null, null, null))));
        var out = results.submit(p, player);
        assertEquals(105, out.totalCoins); assertEquals(14, out.totalStars);
        assertEquals(105, results.submit(p, player).totalCoins);
    }
}
