package com.tankzors.progress;

import jakarta.persistence.*;

@Entity @Table(name = "shop_request")
public class ShopRequest {
    @Id @Column(name = "request_id", length = 201) private String requestId;
    @Column(name = "player_id", nullable = false, length = 80) private String playerId;
    @Column(name = "run_id", nullable = false, length = 120) private String runId;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    protected ShopRequest() {}
    public ShopRequest(String id, String player, String run, String hash) {
        requestId = id; playerId = player; runId = run; payloadHash = hash;
    }
    public String getPlayerId() { return playerId; }
    public String getPayloadHash() { return payloadHash; }
}
