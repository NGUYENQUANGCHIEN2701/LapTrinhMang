package com.tankzors.application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tankzors.progress.PlayerProgress;
import com.tankzors.progress.PlayerProgressRepository;

/**
 * Theo dõi progression unlock của mỗi player theo session - Phase 5+ persistent
 * qua PostgreSQL ({@link PlayerProgressRepository}).
 *
 * <p>Campaign ban đầu chỉ unlock {@code campaign_1}. Khi server detect VICTORY
 * ở màn N, unlock màn N+1. Trạng thái lưu DB, survive qua server restart.</p>
 *
 * <p>Thứ tự levels cố định trong {@link #CAMPAIGN_ORDER}.</p>
 */
@Service
public class PlayerProgressService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProgressService.class);

    public static final List<String> CAMPAIGN_ORDER = List.of(
        "campaign_1", "campaign_2", "campaign_3"
    );

    private final PlayerProgressRepository repository;

    public PlayerProgressService(PlayerProgressRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized List<String> getUnlocked(String sessionId) {
        List<PlayerProgress> rows = repository.findBySessionId(sessionId);
        if (rows.isEmpty()) {
            // First-touch: insert campaign_1 default
            repository.save(new PlayerProgress(sessionId, "campaign_1"));
            logger.info("[PlayerProgress] init session={} -> campaign_1", sessionId);
            return new ArrayList<>(List.of("campaign_1"));
        }
        return rows.stream()
            .map(PlayerProgress::getUnlockedLevel)
            .collect(Collectors.toList());
    }

    @Transactional
    public synchronized boolean isUnlocked(String sessionId, String levelId) {
        return getUnlocked(sessionId).contains(levelId);
    }

    @Transactional
    public synchronized String markVictory(String sessionId, String levelId) {
        // Ensure session initialized
        List<String> unlocked = getUnlocked(sessionId);
        if (!unlocked.contains(levelId)) {
            repository.save(new PlayerProgress(sessionId, levelId));
        }
        int idx = CAMPAIGN_ORDER.indexOf(levelId);
        if (idx < 0 || idx >= CAMPAIGN_ORDER.size() - 1) {
            return null;
        }
        String nextLevel = CAMPAIGN_ORDER.get(idx + 1);
        if (!unlocked.contains(nextLevel)) {
            repository.save(new PlayerProgress(sessionId, nextLevel));
            logger.info("[PlayerProgress] unlock next: {} -> {} for session={}", levelId, nextLevel, sessionId);
            return nextLevel;
        }
        return null;
    }

    @Transactional
    public synchronized void reset(String sessionId) {
        repository.deleteBySessionId(sessionId);
        repository.save(new PlayerProgress(sessionId, "campaign_1"));
        logger.info("[PlayerProgress] reset session={}", sessionId);
    }
}
