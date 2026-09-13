package com.tankzors.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.tankzors.leaderboard.LeaderboardEntry;
import com.tankzors.leaderboard.LeaderboardRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test upsert behavior của {@link LeaderboardService} (Phase 4):
 * submit score mới -> insert; submit score thấp hơn -> giữ nguyên; submit
 * cao hơn -> update.
 */
@SpringBootTest
@ActiveProfiles("test")
class LeaderboardServiceTest {

    @Autowired
    private LeaderboardService service;

    @Autowired
    private LeaderboardRepository repository;

    @Test
    void submitFirstScoreInserts() {
        LeaderboardEntry saved = service.submitScore("Hero1", "campaign_1", 800, 60, 50);
        assertNotNull(saved.getId());
        assertEquals(800, saved.getScore());

        // Restart giả lập: query lại từ repo (DB persistent)
        LeaderboardEntry found = repository.findByPlayerNameAndMapId("Hero1", "campaign_1").orElseThrow();
        assertEquals(800, found.getScore());
    }

    @Test
    void submitLowerScoreKeepsBest() {
        service.submitScore("Hero2", "campaign_2", 1500, 30, 90);
        LeaderboardEntry result = service.submitScore("Hero2", "campaign_2", 1200, 40, 80);
        assertEquals(1500, result.getScore(), "Best score phải được giữ nguyên khi submit thấp hơn");
    }

    @Test
    void submitHigherScoreUpdates() {
        service.submitScore("Hero3", "campaign_3", 1000, 50, 70);
        LeaderboardEntry result = service.submitScore("Hero3", "campaign_3", 1800, 25, 95);
        assertEquals(1800, result.getScore());
        assertEquals(25, result.getTimeSeconds());
        assertEquals(95, result.getHpRemain());
    }

    @Test
    void topScoresReturnsSorted() {
        service.submitScore("Hero4", "online_3", 1000, 60, 50);
        service.submitScore("Hero5", "online_3", 2000, 30, 90);
        assertEquals(2000, service.topScores("online_3").get(0).getScore());
    }
}
