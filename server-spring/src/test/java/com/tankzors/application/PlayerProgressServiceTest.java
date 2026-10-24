package com.tankzors.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.tankzors.progress.PlayerProgress;
import com.tankzors.progress.PlayerProgressRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 test cho {@link PlayerProgressService}.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlayerProgressServiceTest {

    @Autowired
    private PlayerProgressService progressService;

    @Autowired
    private PlayerProgressRepository repository;

    @Test
    void continueFollowsProgressAndDoesNotRestartFinishedCampaign() {
        String playerId = "test-continue-progress";
        progressService.reset(playerId);
        for (String levelId : PlayerProgressService.CAMPAIGN_ORDER) {
            assertEquals(levelId, progressService.getContinueLevel(playerId).orElseThrow());
            progressService.markVictory(playerId, levelId);
        }
        assertTrue(progressService.getContinueLevel(playerId).isEmpty());
    }

    @Test
    void continueInitializesNewPlayerAndIsolatesTheirProgress() {
        String playerId = "test-continue-account";
        progressService.reset(playerId);
        progressService.markVictory(playerId, "campaign_1");
        assertEquals("campaign_2", progressService.getContinueLevel(playerId).orElseThrow());
        assertEquals("campaign_1", progressService.getContinueLevel("test-continue-new").orElseThrow());
    }

    @Test
    void continueChoosesFurthestPlayableLevelFromLegacyUnlockedRows() {
        String playerId = "test-continue-legacy";
        progressService.reset(playerId);
        repository.save(new PlayerProgress(playerId, "campaign_3"));
        repository.save(new PlayerProgress(playerId, "campaign_2"));
        repository.save(new PlayerProgress(playerId, "unknown-level"));
        assertEquals("campaign_3", progressService.getContinueLevel(playerId).orElseThrow());
    }

    @Test
    void newSessionStartsWithCampaign1Only() {
        List<String> unlocked = progressService.getUnlocked("test-session-fresh");
        assertEquals(List.of("campaign_1"), unlocked);
    }

    @Test
    void victoryLevel1UnlocksLevel2() {
        String session = "test-session-victory1";
        progressService.reset(session);
        String next = progressService.markVictory(session, "campaign_1");
        assertEquals("campaign_2", next);
        assertTrue(progressService.isUnlocked(session, "campaign_2"));
    }

    @Test
    void victoryLocksCompletedLevelSoItIsPlayedOnce() {
        String session = "test-session-once";
        progressService.reset(session);
        progressService.markVictory(session, "campaign_1");
        assertEquals(List.of("campaign_2"), progressService.getUnlocked(session));
        assertFalse(progressService.isUnlocked(session, "campaign_1"), "Màn đã thắng phải bị khoá");
        assertEquals(List.of("campaign_1"), progressService.getCompleted(session));
    }

    @Test
    void finishingWholeCampaignLeavesNothingPlayable() {
        String session = "test-session-all-done";
        progressService.reset(session);
        progressService.markVictory(session, "campaign_1");
        progressService.markVictory(session, "campaign_2");
        progressService.markVictory(session, "campaign_3");
        assertEquals(List.of(), progressService.getUnlocked(session),
            "Không được tự mở lại campaign_1 sau khi hoàn thành hết");
        assertEquals(List.of("campaign_1", "campaign_2", "campaign_3"), progressService.getCompleted(session));
    }

    @Test
    void repeatedVictoryReportDoesNotReopenLevel() {
        String session = "test-session-repeat";
        progressService.reset(session);
        progressService.markVictory(session, "campaign_1");
        assertNull(progressService.markVictory(session, "campaign_1"));
        assertEquals(List.of("campaign_2"), progressService.getUnlocked(session));
    }

    @Test
    void victoryLastLevelDoesNotUnlockMore() {
        String session = "test-session-last";
        progressService.reset(session);
        progressService.markVictory(session, "campaign_1");
        progressService.markVictory(session, "campaign_2");
        String next = progressService.markVictory(session, "campaign_3");
        assertNull(next, "Màn 3 là màn cuối, không có màn kế tiếp");
    }

    @Test
    void resetReturnsToCampaign1Only() {
        String session = "test-session-reset";
        progressService.markVictory(session, "campaign_1");
        assertTrue(progressService.isUnlocked(session, "campaign_2"));
        progressService.reset(session);
        assertEquals(List.of("campaign_1"), progressService.getUnlocked(session));
    }

    @Test
    void progressionIsIsolatedPerSession() {
        String s1 = "test-session-A";
        String s2 = "test-session-B";
        progressService.reset(s1);
        progressService.reset(s2);
        progressService.markVictory(s1, "campaign_1");
        assertTrue(progressService.isUnlocked(s1, "campaign_2"));
        assertFalse(progressService.isUnlocked(s2, "campaign_2"));
    }

    @Test
    void unknownLevelDoesNotBreak() {
        String session = "test-session-unknown";
        progressService.reset(session);
        String next = progressService.markVictory(session, "not_a_real_level");
        assertNull(next);
    }
}
