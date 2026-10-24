package com.tankzors.leaderboard;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository cho {@link LeaderboardEntry}.
 *
 * <p>Phase 4 PostgreSQL-only. Phase 5+ nếu cần port sang DB khác, tạo
 * interface port riêng và giữ impl này.</p>
 */
@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    /**
     * Lấy top N score của một map, sắp xếp giảm dần.
     */
    List<LeaderboardEntry> findTop10ByMapIdOrderByScoreDesc(String mapId);

    List<LeaderboardEntry> findByMapIdOrderByScoreDesc(String mapId);

    /**
     * Tìm entry của player trên map (để upsert best score).
     */
    Optional<LeaderboardEntry> findByPlayerNameAndMapId(String playerName, String mapId);
}
