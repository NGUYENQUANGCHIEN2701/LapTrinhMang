package com.tankzors.protocol;

/**
 * Packet {@code SHOOT_INPUT} (client -> server) — v1 mục 3.4.
 *
 * <p>Không có payload; client chỉ gửi type + timestamp khi bấm phím Space.
 * Server ứng dụng cooldown (PlayerTank 0.5s) và own state để quyết định
 * có spawn bullet hay không.</p>
 */
public class ShootInputPacket extends BasePacket {
    public ShootInputPacket() {
        super(PacketType.SHOOT_INPUT.wireName());
    }
}
