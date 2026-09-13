package com.tankzors.protocol;

/**
 * Packet {@code CONNECT} (server -> client) — v1 mục 3.1.
 *
 * <p>Server gửi ngay sau khi WebSocket onConnect, chứa sessionId mà
 * client dùng để identify player tank trong {@code STATE.tanks}.</p>
 */
public class ConnectPacket extends BasePacket {
    private String sessionId;
    private String message;

    public ConnectPacket() {
        super(PacketType.CONNECT.wireName());
    }

    public ConnectPacket(String sessionId, String message) {
        super(PacketType.CONNECT.wireName());
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
