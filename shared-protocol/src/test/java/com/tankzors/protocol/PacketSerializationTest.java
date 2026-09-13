package com.tankzors.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PacketSerializationTest {

    private final Gson gson = new Gson();

    @Test
    void connectPacketSerializesToV1Shape() {
        ConnectPacket pkt = new ConnectPacket("session-1", "hello");
        String json = gson.toJson(pkt);
        ConnectPacket back = gson.fromJson(json, ConnectPacket.class);
        assertEquals("CONNECT", back.getType());
        assertEquals("session-1", back.getSessionId());
        assertEquals("hello", back.getMessage());
    }

    @Test
    void statePacketHasDefaultGameStatus() {
        StatePacket pkt = new StatePacket();
        assertNotNull(pkt.getTanks());
        assertNotNull(pkt.getBullets());
        assertEquals("PLAYING", pkt.getGameStatus());
    }

    @Test
    void joinGameRoundTripsPayloadFields() {
        JoinGamePacket pkt = new JoinGamePacket("campaign", "campaign_1", "Hero");
        String json = gson.toJson(pkt);
        JoinGamePacket back = gson.fromJson(json, JoinGamePacket.class);
        assertEquals("campaign", back.getMode());
        assertEquals("campaign_1", back.getLevelId());
        assertEquals("Hero", back.getPlayerName());
    }

    @Test
    void tileTypeRoundTripsIntValues() {
        assertEquals(0, TileTypeDto.GROUND.value());
        assertEquals(1, TileTypeDto.WALL.value());
        assertEquals(2, TileTypeDto.WATER.value());
        assertEquals(3, TileTypeDto.SPAWN_PLAYER.value());
        assertEquals(4, TileTypeDto.SPAWN_ENEMY.value());
        assertEquals(TileTypeDto.GROUND, TileTypeDto.fromValue(99));
    }
}
