package com.tankzors.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Theo dõi progression unlock của mỗi player theo session.
 *
 * <p>Campaign ban đầu chỉ unlock {@code campaign_1}. Khi server detect VICTORY
 * ở màn N, unlock màn N+1.</p>
 *
 * <p>Phase 5+: phân tách data ra PostgreSQL nếu cần persist qua restart.
 * Hiện tại in-memory (theo session id), reset khi server restart.</p>
 *
 * <p>Thứ tự level cố định trong {@link #CAMPAIGN_ORDER}.</p>
 */
@Service
public class PlayerProgressService {

    /**
     * Thứ tự các màn Campaign, dùng để xác định "màn kế tiếp".
     */
    public static final List<String> CAMPAIGN_ORDER = List.of(
        "campaign_1", "campaign_2", "campaign_3"
    );

    // sessionId -> unlocked levels (theo package, e.g. ["campaign_1", "campaign_2"])
    private final Map<String, List<String>> unlockedBySession = new LinkedHashMap<>();

    /**
     * Lấy danh sách màn đã unlock của session. Default: chỉ campaign_1.
     */
    public synchronized List<String> getUnlocked(String sessionId) {
        return unlockedBySession.computeIfAbsent(sessionId, k -> new ArrayList<>(List.of("campaign_1")));
    }

    /**
     * Kiểm tra một màn có được unlock cho session không.
     */
    public synchronized boolean isUnlocked(String sessionId, String levelId) {
        return getUnlocked(sessionId).contains(levelId);
    }

    /**
     * Đánh dấu hoàn thành một màn. Mở khoá màn kế tiếp nếu có.
     *
     * @return màn đã được unlock mới (hoặc null nếu không có).
     */
    public synchronized String markVictory(String sessionId, String levelId) {
        List<String> unlocked = getUnlocked(sessionId);
        // Thêm level anh dược thẳng vào unlocked (defensive)
        if (!unlocked.contains(levelId)) {
            unlocked.add(levelId);
        }
        int idx = CAMPAIGN_ORDER.indexOf(levelId);
        if (idx < 0 || idx >= CAMPAIGN_ORDER.size() - 1) {
            // Không có màn kế tiếp hoặc level không hợp lệ
            return null;
        }
        String nextLevel = CAMPAIGN_ORDER.get(idx + 1);
        if (!unlocked.contains(nextLevel)) {
            unlocked.add(nextLevel);
            return nextLevel;
        }
        return null;
    }

    /**
     * Reset tiến trình về trạng thái ban đầu (chỉ unlock campaign_1).
     */
    public synchronized void reset(String sessionId) {
        unlockedBySession.put(sessionId, new ArrayList<>(List.of("campaign_1")));
    }
}
