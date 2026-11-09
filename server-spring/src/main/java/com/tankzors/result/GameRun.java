package com.tankzors.result;

import jakarta.persistence.*;
import java.time.Instant;

/** Issued-run metadata only; contains no gameplay simulation. */
@Entity
@Table(name = "game_run")
public class GameRun {
    @Id @Column(name = "run_id", length = 120) private String runId;
    @Column(name = "player_id", nullable = false, length = 80) private String playerId;
    @Column(name = "level_id", nullable = false, length = 30) private String levelId;
    @Column(name = "map_version", nullable = false, length = 60) private String mapVersion;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "finished_at") private Instant finishedAt;
    @Column(name = "claimed_pickups", nullable = false, columnDefinition = "TEXT") private String claimedPickups = "[]";
    @Column(name = "shop_floor", nullable = false, columnDefinition = "TEXT") private String shopFloor = "{}";
    protected GameRun() {}
    public GameRun(String runId, String playerId, String levelId, String version) {
        this.runId = runId; this.playerId = playerId; this.levelId = levelId; this.mapVersion = version;
        issuedAt = Instant.now(); expiresAt = issuedAt.plusSeconds(86400);
    }
    public String getRunId() { return runId; }
    public String getPlayerId() { return playerId; }
    public String getLevelId() { return levelId; }
    public String getMapVersion() { return mapVersion; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void finish() { finishedAt = Instant.now(); }
    public String getClaimedPickups() { return claimedPickups; }
    public void setClaimedPickups(String value) { claimedPickups = value; }
    public String getShopFloor() { return shopFloor; }
    public void setShopFloor(String value) { shopFloor = value; }
}
