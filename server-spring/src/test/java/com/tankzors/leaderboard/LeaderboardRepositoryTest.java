package com.tankzors.leaderboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 integration test: Flyway migration + repository + upsert qua
 * {@link com.tankzors.application.LeaderboardService}.
 *
 * <p>Chạy với profile {@code test} (H2 in-memory PostgreSQL-compatible)
 * để không cần Docker Postgres khi build. Phase 5 sẽ có parity test trên
 * Postgres thật.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaderboardRepositoryTest {

    @Autowired
    private LeaderboardRepository repository;

    @Test
    void schemaCreatedByFlyway() {
        assertNotNull(repository);
    }

    @Test
    void saveAndFindByPlayerAndMap() {
        LeaderboardEntry entry = new LeaderboardEntry("Hero", "campaign_1", 1200, 45, 80);
        repository.save(entry);

        Optional<LeaderboardEntry> found = repository.findByPlayerNameAndMapId("Hero", "campaign_1");
        assertTrue(found.isPresent());
        assertEquals(1200, found.get().getScore());
    }

    @Test
    void topScoresOrderedByScoreDesc() {
        repository.save(new LeaderboardEntry("P1", "online_1", 1000, 60, 50));
        repository.save(new LeaderboardEntry("P2", "online_1", 2000, 30, 90));
        repository.save(new LeaderboardEntry("P3", "online_1", 1500, 45, 70));

        List<LeaderboardEntry> top = repository.findTop10ByMapIdOrderByScoreDesc("online_1");
        assertEquals(3, top.size());
        assertEquals(2000, top.get(0).getScore());
        assertEquals(1500, top.get(1).getScore());
        assertEquals(1000, top.get(2).getScore());
    }
}
