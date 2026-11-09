package com.tankzors.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository tổng reward tích lũy của player (V4). */
public interface PlayerTotalsRepository extends JpaRepository<PlayerTotals, String> {
    Optional<PlayerTotals> findByPlayerId(String playerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PlayerTotals t where t.playerId = :playerId")
    Optional<PlayerTotals> findForUpdate(@Param("playerId") String playerId);
}
