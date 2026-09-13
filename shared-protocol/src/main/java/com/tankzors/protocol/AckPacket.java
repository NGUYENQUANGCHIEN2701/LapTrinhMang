package com.tankzors.protocol;

/**
 * Packet {@code ACK} (server -> client) — v1 mục 3.6.
 *
 * <p>Phát sau mỗi input (JOIN/MOVE/SHOOT) hoặc cho generic message.
 * Client React hiện tại KHÔNG dùng ACK để chặn input, chỉ log.</p>
 */
public class AckPacket extends BasePacket {
    private String status;
    private String action;
    private String receivedType;
    private String message;

    public AckPacket() {
        super(PacketType.ACK.wireName());
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReceivedType() {
        return receivedType;
    }

    public void setReceivedType(String receivedType) {
        this.receivedType = receivedType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
