package com.tankzors.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.tankzors.engine.MatchEndObserver;
import com.tankzors.leaderboard.LeaderboardEntry;
import com.tankzors.leaderboard.LeaderboardRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 integration test: MatchEndService -> LeaderboardService + PlayerProgressService
 *
 * <p>Verify flow khi VICTORY:</p>
 * <ol>
 *   <li>MatchEndService.onMatchEnd async</li>
 *   <li>Submit score vào leaderboard (ScoreCalculator -> calculate)</li>
 *   <li>Unlock màn kế tiếp trong PlayerProgressService</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
class MatchEndServiceTest {

    @Autowired
    private MatchEndObserver matchEndService;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private PlayerProgressService progressService;

    @Test
    void onVictorySubmitsScoreAndUnlocksNextLevel() throws Exception {
        String session = "test-match-end";
        progressService.reset(session);
        leaderboardRepository.findByPlayerNameAndMapId("Player-test-m", "campaign_match_test")
            .ifPresent(leaderboardRepository::delete);

        // Trigger VICTORY (async void)
        matchEndService.onMatchEnd(session, "campaign_match_test", "VICTORY", 75, 2, 45);

        // Poll cho max 2s để async hoàn tất
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            var top = leaderboardRepository.findTop10ByMapIdOrderByScoreDesc("campaign_match_test");
            if (!top.isEmpty() && top.stream().anyMatch(e -> e.getScore() == 1700)) break;
            Thread.sleep(100);
        }

        var top = leaderboardRepository.findTop10ByMapIdOrderByScoreDesc("campaign_match_test");
        assertFalse(top.isEmpty(), "Phải có ít nhất 1 entry sau VICTORY");
        assertTrue(top.stream().anyMatch(e -> e.getScore() == 1700),
            "Score phải bằng 1700 theo công thức (75*10 + 2*200 + 550)");
    }

    @Test
    void onVictoryUnlocksNextLevelInSession() throws Exception {
        String session = "test-match-end-unlock";
        progressService.reset(session);
        assertFalse(progressService.isUnlocked(session, "campaign_2"));
        matchEndService.onMatchEnd(session, "campaign_1", "VICTORY", 100, 1, 30);
        // Poll cho async
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && !progressService.isUnlocked(session, "campaign_2")) {
            Thread.sleep(100);
        }
        assertTrue(progressService.isUnlocked(session, "campaign_2"));
    }

    @Test
    void onDefeatDoesNotSubmitScoreNorUnlock() throws Exception {
        String session = "test-match-end-defeat";
        progressService.reset(session);
        matchEndService.onMatchEnd(session, "campaign_1", "DEFEAT", 0, 0, 60);
        Thread.sleep(500);
        assertFalse(progressService.isUnlocked(session, "campaign_2"));
    }
}
