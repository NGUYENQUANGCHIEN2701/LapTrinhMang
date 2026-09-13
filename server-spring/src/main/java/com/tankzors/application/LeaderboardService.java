package com.tankzors.application;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tankzors.leaderboard.LeaderboardEntry;
import com.tankzors.leaderboard.LeaderboardRepository;

/**
 * Service quản lý leaderboard (Phase 4 PostgreSQL).
 *
 * <p>Upsert (theo cặp playerName+mapId): nếu player đã có entry trên map
 * và score mới lớn hơn -> override; ngược lại giữ nguyên.</p>
 *
 * <p>Tách khỏi {@code GameLoop} tick để không gọi DB mỗi tick (theo AI guide).</p>
 */
@Service
public class LeaderboardService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);

    private final LeaderboardRepository repository;

    public LeaderboardService(LeaderboardRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LeaderboardEntry submitScore(String playerName, String mapId, int score,
                                        int timeSeconds, int hpRemain) {
        LeaderboardEntry existing = repository.findByPlayerNameAndMapId(playerName, mapId).orElse(null);
        if (existing == null) {
            LeaderboardEntry entry = new LeaderboardEntry(playerName, mapId, score, timeSeconds, hpRemain);
            LeaderboardEntry saved = repository.save(entry);
            logger.info("[Leaderboard] new entry: player={} map={} score={}", playerName, mapId, score);
            return saved;
        }
        if (score > existing.getScore()) {
            existing.setScore(score);
            existing.setTimeSeconds(timeSeconds);
            existing.setHpRemain(hpRemain);
            existing.setCreatedAt(Instant.now());
            LeaderboardEntry saved = repository.save(existing);
            logger.info("[Leaderboard] improved: player={} map={} {}->{}",
                playerName, mapId, existing.getScore(), score);
            return saved;
        }
        logger.debug("[Leaderboard] ignored (lower): player={} map={} new={} best={}",
            playerName, mapId, score, existing.getScore());
        return existing;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> topScores(String mapId) {
        return repository.findTop10ByMapIdOrderByScoreDesc(mapId);
    }
}
