package com.tankzors.progress;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho {@link PlayerProgress} - Phase 5+ persistent progression.
 */
@Repository
public interface PlayerProgressRepository extends JpaRepository<PlayerProgress, PlayerProgress.Pk> {

    List<PlayerProgress> findBySessionId(String sessionId);

    long deleteBySessionId(String sessionId);
}
