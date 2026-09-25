package com.tankzors.client.network;

/** Builds the GWT-safe v2 co-op relay envelope. */
public final class RelayPackets {
    private RelayPackets() {}

    public static String envelope(String type, String roomId, String runId, long seq, String payloadInner) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"version\":\"v2\",\"type\":").append(quote(type));
        if (roomId != null) sb.append(",\"roomId\":").append(quote(roomId));
        if (runId != null) sb.append(",\"runId\":").append(quote(runId));
        sb.append(",\"seq\":").append(seq);
        if (payloadInner != null && !payloadInner.isEmpty()) {
            sb.append(",\"payload\":{").append(payloadInner).append('}');
        }
        return sb.append('}').toString();
    }

    public static String playerInputPayload(String direction, boolean moving, boolean shoot, boolean stopAll) {
        StringBuilder sb = new StringBuilder();
        if (direction != null) sb.append("\"direction\":").append(quote(direction)).append(',');
        sb.append("\"moving\":").append(moving);
        if (shoot) sb.append(",\"shoot\":true");
        if (stopAll) sb.append(",\"stopAll\":true");
        return sb.toString();
    }

    public static String quote(String value) {
        if (value == null) return "\"\"";
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
