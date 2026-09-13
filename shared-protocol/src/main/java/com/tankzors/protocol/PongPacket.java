package com.tankzors.protocol;

/**
 * Packet {@code PONG} (server -> client) — v1 mục 3.7.
 *
 * <p>Server đáp lại packet {@code PING}. Client React hiện tại chưa dùng
 * ping/pong (auto-reconnect dựa vào onClose), nhưng nếu LibGDX client
 * cần heartbeat thì wire format phải giữ nguyên v1.</p>
 */
public class PongPacket extends BasePacket {
    public PongPacket() {
        super(PacketType.PONG.wireName());
    }
}
