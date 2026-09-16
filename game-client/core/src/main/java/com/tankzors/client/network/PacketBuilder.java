package com.tankzors.client.network;

/**
 * Build packet JSON gửi lên server.
 *
 * <p>Tương đương {@code MessageBuilder.ts} của React client. Cùng nested
 * payload format (parity với wire format v1):</p>
 *
 * <pre>{@code
 *   { "type": "MOVE_INPUT", "payload": { "direction":"UP", "moving":true }, "timestamp": ... }
 * }</pre>
 */
public final class PacketBuilder {

    private PacketBuilder() {
    }

    public static String joinGame(String mode, String levelId, String playerName) {
        return nested(
            "JOIN_GAME",
            kv("mode", mode) + "," + kv("levelId", levelId) + "," + kv("playerName", playerName)
        );
    }

    public static String joinGameCampaign(String levelId) {
        return joinGame("campaign", levelId, "Player");
    }

    public static String moveInput(String direction, boolean moving) {
        return nested("MOVE_INPUT", kv("direction", direction) + "," + kvBool("moving", moving));
    }

    public static String shootInput() {
        return "{\"type\":\"SHOOT_INPUT\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    /** Phase G: dừng toàn bộ di chuyển (thả phím cuối cùng / pause / dispose). */
    public static String stop() {
        return "{\"type\":\"STOP\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    /** Phase G: heartbeat; server đáp PONG (protocol-v1.md mục 3.7). */
    public static String ping() {
        return "{\"type\":\"PING\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private static String nested(String type, String payloadFields) {
        return "{\"type\":\"" + type + "\",\"payload\":{" + payloadFields
            + "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private static String kv(String key, String value) {
        return "\"" + key + "\":" + jsonQuoted(value);
    }

    private static String kvBool(String key, boolean value) {
        return "\"" + key + "\":" + value;
    }

    private static String jsonQuoted(String value) {
        if (value == null) return "null";
        // Escape cơ bản (server chỉ parse hostname, name ASCII; đủ an toàn)
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(escapeUnicode(c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String escapeUnicode(char c) {
        // GWT không emulate String.format -> dựng thủ công "\\ uxxxx".
        String hex = Integer.toHexString(c);
        String padded = "0000".substring(hex.length()) + hex;
        return "\\u" + padded;
    }
}
