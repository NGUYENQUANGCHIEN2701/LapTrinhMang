package com.tankzors.application;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tankzors.engine.MatchEndObserver;

/**
 * Implementation {@link MatchEndObserver}: khi VICTORY thực hiện 2 việc:
 *
 * <ol>
 *   <li>Submit score vào {@link LeaderboardService} (best-score upsert).</li>
 *   <li>Unlock màn kế tiếp trong {@link PlayerProgressService}.</li>
 * </ol>
 *
 * <p>DEFEAT: chỉ log, không update.</p>
 *
 * <p>Cả 2 action đều gọi async ({@link Async}) để tránh block GameLoop thread.
 * Spring Boot cần {@code @EnableAsync} active.</p>
 */
@Service
public class MatchEndService implements MatchEndObserver {

    private static final Logger logger = LoggerFactory.getLogger(MatchEndService.class);

    private final LeaderboardService leaderboardService;
    private final PlayerProgressService progressService;

    public MatchEndService(LeaderboardService leaderboardService, PlayerProgressService progressService) {
        this.leaderboardService = leaderboardService;
        this.progressService = progressService;
    }

    @Override
    @Async
    public void onMatchEnd(String sessionId, String levelId, String status,
                           int hpRemain, int aiKills, int elapsedSeconds) {
        try {
            logger.info("[MatchEndService] sessionId={} level={} status={} hp={} aiKills={} time={}s",
                sessionId, levelId, status, hpRemain, aiKills, elapsedSeconds);

            if ("VICTORY".equalsIgnoreCase(status)) {
                int score = ScoreCalculator.calculate(hpRemain, aiKills, elapsedSeconds);
                String playerName = "Player-" + (sessionId != null ? sessionId.substring(0, Math.min(6, sessionId.length())) : "?");
                leaderboardService.submitScore(playerName, levelId, score, elapsedSeconds, hpRemain);

                String next = progressService.markVictory(sessionId, levelId);
                if (next != null) {
                    logger.info("[MatchEndService] unlocked next level: {} (for {})", next, sessionId);
                }
            }
        } catch (Exception e) {
            logger.warn("[MatchEndService] error: {}", e.getMessage(), e);
        }
    }
}
