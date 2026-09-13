package com.tankzors.protocol;

/**
 * Packet type wire protocol v1.
 *
 * <p>Giá trị string được chốt tại {@code docs/protocol-v1.md} mục 2.
 * KHÔNG đổi tên hoặc thứ tự enum khi chưa tạo protocol-v2 song song.</p>
 */
public enum PacketType {
    CONNECT("CONNECT"),
    JOIN_GAME("JOIN_GAME"),
    MOVE_INPUT("MOVE_INPUT"),
    SHOOT_INPUT("SHOOT_INPUT"),
    STATE("STATE"),
    ACK("ACK"),
    PONG("PONG"),
    ERROR("ERROR"),
    INPUT("INPUT"),
    PING("PING");

    private final String wireName;

    PacketType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static PacketType fromWire(String wireName) {
        if (wireName == null) return null;
        String normalized = wireName.trim().toUpperCase();
        for (PacketType t : values()) {
            if (t.wireName.equals(normalized)) return t;
        }
        return null;
    }
}
