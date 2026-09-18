package com.tankzors.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Tổng reward tích lũy của một player: stars/coins.
 *
 * <p>Được {@link com.tankzors.result.ResultService} credit khi một VICTORY campaign
 * được lưu (mỗi runId chỉ credit 1 lần nhờ {@code match_result.run_id UNIQUE}).
 * Khóa theo playerId từ {@link com.tankzors.identity.PlayerIdentityResolver}.</p>
 */
@Entity
@Table(name = "player_totals")
public class PlayerTotals {

    @Id
    @Column(name = "player_id", length = 80, nullable = false)
    private String playerId;

    @Column(name = "total_stars", nullable = false)
    private int totalStars;

    @Column(name = "total_coins", nullable = false)
    private int totalCoins;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public PlayerTotals() {
    }

    public PlayerTotals(String playerId, int totalStars, int totalCoins) {
        this.playerId = playerId;
        this.totalStars = totalStars;
        this.totalCoins = totalCoins;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getTotalStars() { return totalStars; }
    public void setTotalStars(int totalStars) { this.totalStars = totalStars; }

    public int getTotalCoins() { return totalCoins; }
    public void setTotalCoins(int totalCoins) { this.totalCoins = totalCoins; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
