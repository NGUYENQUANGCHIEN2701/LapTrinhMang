package com.tankzors.protocol;

/**
 * Packet {@code ERROR} (server -> client) — v1 mục 3.8.
 *
 * <p>Phát khi client gửi message trống hoặc JSON không parse được.</p>
 */
public class ErrorPacket extends BasePacket {
    private String message;
    private int code;

    public ErrorPacket() {
        super(PacketType.ERROR.wireName());
    }

    public ErrorPacket(String message, int code) {
        super(PacketType.ERROR.wireName());
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
