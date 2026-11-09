package com.tankzors.result;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRunRepository extends JpaRepository<GameRun, String> {
    void deleteByPlayerId(String playerId);
}
