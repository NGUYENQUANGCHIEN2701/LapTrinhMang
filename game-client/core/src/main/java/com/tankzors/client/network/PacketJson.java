package com.tankzors.client.network;

/**
 * JSON helper nhỏ, GWT-safe (không String.format, không Gson). Dùng chung cho
 * PacketBuilder + RelayPackets + host snapshot serialize.
 */
public final class PacketJson {
    private PacketJson() {}

    /** Quote UTF-8 string an toàn JSON. */
    public static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        String hex = "0000".substring(Integer.toHexString(c).length()) + Integer.toHexString(c);
                        sb.append("\\u").append(hex);
                    } else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
