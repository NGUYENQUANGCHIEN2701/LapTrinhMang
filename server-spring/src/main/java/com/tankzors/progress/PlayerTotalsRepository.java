package com.tankzors.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository tổng reward tích lũy của player (V4). */
public interface PlayerTotalsRepository extends JpaRepository<PlayerTotals, String> {
    Optional<PlayerTotals> findByPlayerId(String playerId);
}
