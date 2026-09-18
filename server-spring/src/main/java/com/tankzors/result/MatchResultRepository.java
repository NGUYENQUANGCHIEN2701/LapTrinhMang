package com.tankzors.result;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository kết quả trận (Bước D). */
public interface MatchResultRepository extends JpaRepository<MatchResultEntity, Long> {

    Optional<MatchResultEntity> findByRunId(String runId);
}
