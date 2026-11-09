package com.tankzors.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
 * <p>Campaign ban đầu chỉ unlock {@code campaign_1}. Mỗi màn chỉ chơi một lần:
 * khi server detect VICTORY ở màn N, màn N bị khoá (completed) và màn N+1 được
 * mở. Thua thì được chơi lại chính màn đó. Trạng thái lưu DB, survive qua
 * server restart.</p>
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
    private final PlayerStateLock lock;

    public PlayerProgressService(PlayerProgressRepository repository, PlayerStateLock lock) {
        this.repository = repository;
        this.lock = lock;
    }

    /** Các màn đang được chơi (đã mở và chưa thắng), theo thứ tự campaign. */
    @Transactional
    public List<String> getUnlocked(String sessionId) {
        lock.acquire(sessionId);
        List<PlayerProgress> rows = repository.findBySessionId(sessionId);
        if (rows.isEmpty()) {
            // First-touch: insert campaign_1 default
            repository.save(new PlayerProgress(sessionId, "campaign_1"));
            logger.info("[PlayerProgress] init session={} -> campaign_1", sessionId);
            return new ArrayList<>(List.of("campaign_1"));
        }
        return rows.stream()
            .filter(row -> !row.isCompleted())
            .map(PlayerProgress::getUnlockedLevel)
            .sorted(Comparator.comparingInt(PlayerProgressService::orderOf))
            .collect(Collectors.toList());
    }

    /** Các màn đã thắng và bị khoá, theo thứ tự campaign. */
    @Transactional(readOnly = true)
    public List<String> getCompleted(String sessionId) {
        return repository.findBySessionId(sessionId).stream()
            .filter(PlayerProgress::isCompleted)
            .map(PlayerProgress::getUnlockedLevel)
            .sorted(Comparator.comparingInt(PlayerProgressService::orderOf))
            .collect(Collectors.toList());
    }

    @Transactional
    public boolean isUnlocked(String sessionId, String levelId) {
        return getUnlocked(sessionId).contains(levelId);
    }

    /** Continue mở màn xa nhất đang được chơi, không quay lại màn đã hoàn thành. */
    @Transactional
    public Optional<String> getContinueLevel(String sessionId) {
        List<String> unlocked = getUnlocked(sessionId);
        return CAMPAIGN_ORDER.stream()
            .filter(unlocked::contains)
            .reduce((previous, current) -> current);
    }

    /**
     * Thắng màn {@code levelId}: khoá màn đó và mở màn kế tiếp.
     *
     * @return màn vừa được mở, hoặc {@code null} nếu không có màn mới.
     */
    @Transactional
    public String markVictory(String sessionId, String levelId) {
        lock.acquire(sessionId);
        // Ensure session initialized
        getUnlocked(sessionId);
        Map<String, PlayerProgress> rows = repository.findBySessionId(sessionId).stream()
            .collect(Collectors.toMap(PlayerProgress::getUnlockedLevel, Function.identity()));
        PlayerProgress won = rows.get(levelId);
        if (won == null) {
            won = new PlayerProgress(sessionId, levelId);
        }
        if (!won.isCompleted()) {
            won.setCompleted(true);
            repository.save(won);
            logger.info("[PlayerProgress] lock completed level {} for session={}", levelId, sessionId);
        }
        int idx = CAMPAIGN_ORDER.indexOf(levelId);
        if (idx < 0 || idx >= CAMPAIGN_ORDER.size() - 1) {
            return null;
        }
        String nextLevel = CAMPAIGN_ORDER.get(idx + 1);
        if (!rows.containsKey(nextLevel)) {
            repository.save(new PlayerProgress(sessionId, nextLevel));
            logger.info("[PlayerProgress] unlock next: {} -> {} for session={}", levelId, nextLevel, sessionId);
            return nextLevel;
        }
        return null;
    }

    private static int orderOf(String levelId) {
        int idx = CAMPAIGN_ORDER.indexOf(levelId);
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }

    @Transactional
    public void reset(String sessionId) {
        lock.acquire(sessionId);
        repository.deleteBySessionId(sessionId);
        repository.save(new PlayerProgress(sessionId, "campaign_1"));
        logger.info("[PlayerProgress] reset session={}", sessionId);
    }
}
