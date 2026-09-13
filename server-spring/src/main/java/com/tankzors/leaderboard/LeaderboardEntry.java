package com.tankzors.leaderboard;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Một dòng trong bảng {@code leaderboard}: điểm cao nhất của một player
 * trên một map cụ thể.
 *
 * <p>Phase 4 schema tuoogn đương {@code V1__init.sql bảng leaderboard}.</p>
 *
 * <p>Ràng buộc unique (player_name, map_id) đảm bảo chỉ giữ best score;
 * upsert thực hiện trong repository/service layer.</p>
 */
@Entity
@Table(name = "leaderboard", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_name", "map_id"})
})
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = 50)
    private String playerName;

    @Column(name = "map_id", nullable = false, length = 30)
    private String mapId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "time_seconds", nullable = false)
    private Integer timeSeconds = 0;

    @Column(name = "hp_remain")
    private Integer hpRemain = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(String playerName, String mapId, int score, int timeSeconds, int hpRemain) {
        this.playerName = playerName;
        this.mapId = mapId;
        this.score = score;
        this.timeSeconds = timeSeconds;
        this.hpRemain = hpRemain;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getMapId() { return mapId; }
    public void setMapId(String mapId) { this.mapId = mapId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }
    public Integer getHpRemain() { return hpRemain; }
    public void setHpRemain(Integer hpRemain) { this.hpRemain = hpRemain; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
