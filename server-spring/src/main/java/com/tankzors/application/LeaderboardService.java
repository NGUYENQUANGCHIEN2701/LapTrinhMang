package com.tankzors.application;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tankzors.leaderboard.LeaderboardEntry;
import com.tankzors.leaderboard.LeaderboardRepository;
import com.tankzors.identity.PlayerNames;
import com.tankzors.result.MatchResultRepository;

/**
 * Service quản lý leaderboard trong PostgreSQL.
 *
 * <p>Upsert (theo cặp playerName+mapId): nếu player đã có entry trên map
 * và score mới lớn hơn -> override; ngược lại giữ nguyên.</p>
 *
 * <p>Chỉ truy cập database khi xử lý request hoặc kết quả trận.</p>
 */
@Service
public class LeaderboardService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);

    private final LeaderboardRepository repository;
    private final MatchResultRepository results;

    public LeaderboardService(LeaderboardRepository repository, MatchResultRepository results) {
        this.repository = repository;
        this.results = results;
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
        Map<String, LeaderboardEntry> bestByPlayer = new LinkedHashMap<>();
        for (LeaderboardEntry entry : repository.findByMapIdOrderByScoreDesc(mapId)) {
            String name = entry.getPlayerName();
            String key = "account:" + name;
            if (PlayerNames.isLegacyGeneratedName(name)) {
                // Recover only an unambiguous owner of this exact historical score.
                var matches = results.findByPlayerNameAndMapIdAndScoreAndTimeSecondsAndHpRemainAndStatusAndMode(
                        name, mapId, entry.getScore(), entry.getTimeSeconds(), entry.getHpRemain(), "VICTORY", "CAMPAIGN");
                Set<String> owners = matches.stream().map(r -> r.getPlayerId())
                        .filter(id -> id != null).collect(Collectors.toSet());
                String recovered = owners.size() == 1 ? PlayerNames.accountName(owners.iterator().next()) : null;
                key = recovered == null ? "legacy:" + entry.getId() : "account:" + recovered;
                name = recovered == null ? "Người chơi cũ" : recovered;
            }
            if (!bestByPlayer.containsKey(key)) {
                // A detached view: reading the leaderboard never rewrites stored history.
                LeaderboardEntry view = new LeaderboardEntry(name, mapId, entry.getScore(),
                        entry.getTimeSeconds(), entry.getHpRemain() == null ? 0 : entry.getHpRemain());
                view.setId(entry.getId());
                view.setCreatedAt(entry.getCreatedAt());
                bestByPlayer.put(key, view);
            }
            if (bestByPlayer.size() == 10) break;
        }
        return new ArrayList<>(bestByPlayer.values());
    }
}
