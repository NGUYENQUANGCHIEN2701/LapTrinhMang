package com.tankzors.result;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Một kết quả trận client-reported (Bước D). Bảng match_result mới ở V3 bổ sung
 * run_id/map_version/ai_kills/player_id/payload_hash + unique(run_id) idempotency.
 *
 * <p>Điểm là client-reported và có thể bị sửa — bảng này không phải sự thật tuyệt đối
 * (UI bảng điểm phải diễn độ tin cậy).</p>
 */
@Entity
@Table(name = "match_result")
public class MatchResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", length = 120)
    private String runId;

    @Column(name = "player_id", length = 80)
    private String playerId;

    @Column(name = "session_id", length = 80)
    private String sessionId;

    @Column(name = "player_name", length = 50)
    private String playerName;

    @Column(name = "map_id", length = 30)
    private String mapId;

    @Column(name = "map_version", length = 60)
    private String mapVersion;

    @Column(name = "mode", length = 20)
    private String mode;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "score")
    private Integer score;

    @Column(name = "ai_kills")
    private Integer aiKills;

    @Column(name = "time_seconds")
    private Integer timeSeconds;

    @Column(name = "hp_remain")
    private Integer hpRemain;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "ended_at")
    private Instant endedAt;

    public MatchResultEntity() {}

    protected Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getMapId() { return mapId; }
    public void setMapId(String mapId) { this.mapId = mapId; }
    public String getMapVersion() { return mapVersion; }
    public void setMapVersion(String mapVersion) { this.mapVersion = mapVersion; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getAiKills() { return aiKills; }
    public void setAiKills(Integer aiKills) { this.aiKills = aiKills; }
    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }
    public Integer getHpRemain() { return hpRemain; }
    public void setHpRemain(Integer hpRemain) { this.hpRemain = hpRemain; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
