package com.tankzors.progress;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShopRequestRepository extends JpaRepository<ShopRequest, String> {
    void deleteByPlayerId(String playerId);
}
