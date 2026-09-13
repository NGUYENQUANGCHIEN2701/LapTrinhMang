package com.tankzors.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PacketTypeTest {

    @Test
    void wireNamesMatchProtocolV1() {
        assertEquals("CONNECT", PacketType.CONNECT.wireName());
        assertEquals("JOIN_GAME", PacketType.JOIN_GAME.wireName());
        assertEquals("MOVE_INPUT", PacketType.MOVE_INPUT.wireName());
        assertEquals("SHOOT_INPUT", PacketType.SHOOT_INPUT.wireName());
        assertEquals("STATE", PacketType.STATE.wireName());
        assertEquals("ACK", PacketType.ACK.wireName());
        assertEquals("PONG", PacketType.PONG.wireName());
        assertEquals("ERROR", PacketType.ERROR.wireName());
        assertEquals("INPUT", PacketType.INPUT.wireName());
        assertEquals("PING", PacketType.PING.wireName());
    }

    @Test
    void fromWireIsCaseInsensitive() {
        assertEquals(PacketType.STATE, PacketType.fromWire("state"));
        assertEquals(PacketType.STATE, PacketType.fromWire(" STATE "));
        assertEquals(PacketType.MOVE_INPUT, PacketType.fromWire("MOVE_INPUT"));
    }

    @Test
    void fromWireReturnsNullForUnknown() {
        assertNull(PacketType.fromWire(null));
        assertNull(PacketType.fromWire("WHATEVER"));
    }
}
