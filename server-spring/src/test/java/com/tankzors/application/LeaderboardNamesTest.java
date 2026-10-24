package com.tankzors.application;

import com.tankzors.leaderboard.LeaderboardEntry;
import com.tankzors.leaderboard.LeaderboardRepository;
import com.tankzors.result.MatchResultEntity;
import com.tankzors.result.MatchResultRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaderboardNamesTest {
    @Test void recoversLegacyAccountAndKeepsOnlyItsBestScoreWithoutRewritingHistory() {
        LeaderboardRepository repository = mock(LeaderboardRepository.class);
        MatchResultRepository results = mock(MatchResultRepository.class);
        LeaderboardEntry old = new LeaderboardEntry("Player-user:t", "campaign_1", 1200, 40, 5);
        old.setId(1L);
        LeaderboardEntry recent = new LeaderboardEntry("tuan12", "campaign_1", 900, 35, 5);
        when(repository.findByMapIdOrderByScoreDesc("campaign_1")).thenReturn(List.of(old, recent));
        MatchResultEntity match = new MatchResultEntity();
        match.setPlayerId("user:tuan12");
        when(results.findByPlayerNameAndMapIdAndScoreAndTimeSecondsAndHpRemainAndStatusAndMode(
                "Player-user:t", "campaign_1", 1200, 40, 5, "VICTORY", "CAMPAIGN"))
                .thenReturn(List.of(match));
        List<LeaderboardEntry> rows = new LeaderboardService(repository, results).topScores("campaign_1");
        assertEquals(1, rows.size());
        assertEquals("tuan12", rows.get(0).getPlayerName());
        assertEquals(1200, rows.get(0).getScore());
        assertEquals("Player-user:t", old.getPlayerName());
        verify(repository, never()).save(any());
    }

    @Test void ambiguousOrMissingLegacyIdentityNeverGuessesAnAccountName() {
        LeaderboardRepository repository = mock(LeaderboardRepository.class);
        MatchResultRepository results = mock(MatchResultRepository.class);
        LeaderboardEntry old = new LeaderboardEntry("Player-user:t", "campaign_1", 1200, 40, 5);
        old.setId(1L);
        when(repository.findByMapIdOrderByScoreDesc("campaign_1")).thenReturn(List.of(old));
        MatchResultEntity first = new MatchResultEntity(), second = new MatchResultEntity();
        first.setPlayerId("user:tuan12");
        second.setPlayerId("user:trang");
        when(results.findByPlayerNameAndMapIdAndScoreAndTimeSecondsAndHpRemainAndStatusAndMode(
                "Player-user:t", "campaign_1", 1200, 40, 5, "VICTORY", "CAMPAIGN"))
                .thenReturn(List.of(first, second));
        assertEquals("Người chơi cũ", new LeaderboardService(repository, results)
                .topScores("campaign_1").get(0).getPlayerName());
    }
}
