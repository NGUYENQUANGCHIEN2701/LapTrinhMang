package com.tankzors.result;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository kết quả trận (Bước D). */
public interface MatchResultRepository extends JpaRepository<MatchResultEntity, Long> {

    Optional<MatchResultEntity> findByRunId(String runId);

    List<MatchResultEntity> findByPlayerNameAndMapIdAndScoreAndTimeSecondsAndHpRemainAndStatusAndMode(
            String playerName, String mapId, Integer score, Integer timeSeconds,
            Integer hpRemain, String status, String mode);
}
