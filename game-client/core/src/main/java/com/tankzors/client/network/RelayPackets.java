package com.tankzors.client.network;

/**
 * Build envelope relay v2 (GWT-safe, không String.format, không Gson).
 * Envelope: { version, type, roomId?, runId?, seq?, payload? }.
 * Server gắn senderId từ connection; KHÔNG tin senderId client truyền.
 */
public final class RelayPackets {
    private RelayPackets() {}

    /** Tạo một envelope v2 với payload inner. payloadInner=null => không có payload. */
    public static String envelope(String type, String roomId, String runId, long seq, String payloadInner) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"version\":\"v2\",\"type\":\"").append(type).append('"');
        if (roomId != null) sb.append(",\"roomId\":").append(quote(roomId));
        if (runId != null) sb.append(",\"runId\":").append(quote(runId));
        sb.append(",\"seq\":").append(seq);
        if (payloadInner != null && !payloadInner.isEmpty()) sb.append(",\"payload\":{").append(payloadInner).append('}');
        sb.append('}');
        return sb.toString();
    }

    /** Payload PLAYER_INPUT. */
    public static String playerInputPayload(String direction, boolean moving, boolean shoot, boolean stopAll) {
        StringBuilder sb = new StringBuilder();
        if (direction != null) sb.append("\"direction\":").append(quote(direction)).append(',');
        sb.append("\"moving\":").append(moving);
        if (shoot) sb.append(",\"shoot\":true");
        if (stopAll) sb.append(",\"stopAll\":true");
        return sb.toString();
    }

    /** Trích JSON cho số/ký tự. */
    public static String quote(String s) { return PacketJson.quote(s); }
}
