package com.tankzors.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Lưu trạng thái túi đồ (inventory) và cây nghiên cứu (research) của player.
 */
@Entity
@Table(name = "player_shop_state")
public class PlayerShopState {

    @Id
    @Column(name = "player_id", length = 80, nullable = false)
    private String playerId;

    @Column(name = "research_json", nullable = false, columnDefinition = "TEXT")
    private String researchJson = "{}";

    @Column(name = "inventory_json", nullable = false, columnDefinition = "TEXT")
    private String inventoryJson = "{}";

    @Column(name = "updated_at")
    private Instant updatedAt;

    public PlayerShopState() {
    }

    public PlayerShopState(String playerId, String researchJson, String inventoryJson) {
        this.playerId = playerId;
        this.researchJson = researchJson != null ? researchJson : "{}";
        this.inventoryJson = inventoryJson != null ? inventoryJson : "{}";
        this.updatedAt = Instant.now();
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getResearchJson() { return researchJson; }
    public void setResearchJson(String researchJson) { this.researchJson = researchJson; }

    public String getInventoryJson() { return inventoryJson; }
    public void setInventoryJson(String inventoryJson) { this.inventoryJson = inventoryJson; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

