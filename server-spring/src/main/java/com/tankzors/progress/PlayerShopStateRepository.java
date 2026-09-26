package com.tankzors.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerShopStateRepository extends JpaRepository<PlayerShopState, String> {
    Optional<PlayerShopState> findByPlayerId(String playerId);
}

