package com.tankzors.network;

import com.tankzors.room.Room;
import com.tankzors.room.RoomRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomRelayHandlerTest {

    private final RoomRegistry registry = new RoomRegistry();
    private final RoomRelayHandler handler = new RoomRelayHandler(registry);

    private WebSocketSession connect(String id, String playerId) {
        WebSocketSession s = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("tankzors.playerId", playerId);
        when(s.getId()).thenReturn(id);
        when(s.getAttributes()).thenReturn(attrs);
        when(s.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(s);
        return s;
    }

    private void send(WebSocketSession s, String json) {
        handler.handleTextMessage(s, new TextMessage(json));
    }

    private Room roomOf(WebSocketSession s) {
        return registry.findByConnection(s.getId()).orElse(null);
    }

    @Test
    void creatingSecondRoomRemovesTheFirst() {
        WebSocketSession host = connect("c-create", "user:a");
        send(host, "{\"version\":\"v2\",\"type\":\"CREATE_ROOM\"}");
        String first = roomOf(host).getRoomId();

        send(host, "{\"version\":\"v2\",\"type\":\"CREATE_ROOM\"}");

        assertNull(registry.get(first), "Phòng cũ không còn ai phải bị xoá");
        assertNotEquals(first, roomOf(host).getRoomId());
        handler.afterConnectionClosed(host, CloseStatus.NORMAL);
    }

    @Test
    void hostLeavingFreesTheRoomEvenWhileGuestStaysConnected() {
        WebSocketSession host = connect("c-host", "user:h");
        WebSocketSession guest = connect("c-guest", "user:g");
        send(host, "{\"version\":\"v2\",\"type\":\"CREATE_ROOM\"}");
        String roomId = roomOf(host).getRoomId();
        send(guest, "{\"version\":\"v2\",\"type\":\"JOIN_ROOM\",\"roomId\":\"" + roomId + "\"}");
        assertEquals(2, registry.get(roomId).size());

        handler.afterConnectionClosed(host, CloseStatus.NORMAL);

        assertNull(registry.get(roomId), "Phòng đã đóng phải được xoá khỏi bộ nhớ");
        assertNull(roomOf(guest), "Guest không còn bị gắn vào phòng đã đóng");
        handler.afterConnectionClosed(guest, CloseStatus.NORMAL);
    }
    @Test void matchStartedContainsBothPlayersAndCannotStartTwice() throws Exception {
        WebSocketSession host = connect("start-host", "user:host");
        WebSocketSession guest = connect("start-guest", "user:guest");
        send(host, "{\"version\":\"v2\",\"type\":\"CREATE_ROOM\"}");
        String id = roomOf(host).getRoomId();
        send(guest, "{\"version\":\"v2\",\"type\":\"JOIN_ROOM\",\"roomId\":\"" + id + "\"}");
        send(host, "{\"version\":\"v2\",\"type\":\"READY\",\"payload\":{\"ready\":true,\"levelId\":\"campaign_1\"}}");
        send(guest, "{\"version\":\"v2\",\"type\":\"READY\",\"payload\":{\"ready\":true}}");
        send(host, "{\"version\":\"v2\",\"type\":\"START_MATCH\"}");
        var messages = org.mockito.ArgumentCaptor.forClass(org.springframework.web.socket.WebSocketMessage.class);
        org.mockito.Mockito.verify(host, org.mockito.Mockito.atLeastOnce()).sendMessage(messages.capture());
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var start = messages.getAllValues().stream().map(message -> {
            try { return mapper.readTree(message.getPayload().toString()); } catch (Exception e) { throw new RuntimeException(e); }
        }).filter(packet -> "MATCH_STARTED".equals(packet.path("type").asText())).findFirst().orElseThrow();
        assertEquals(2, start.path("payload").path("members").size());
        assertEquals("user:guest", start.path("payload").path("members").get(1).path("playerId").asText());
        String run = roomOf(host).getRunId();
        send(host, "{\"version\":\"v2\",\"type\":\"START_MATCH\"}");
        assertEquals(run, roomOf(host).getRunId());
        handler.revokePlayer("user:host");
        org.mockito.Mockito.verify(host).close(CloseStatus.POLICY_VIOLATION);
        assertNull(registry.get(id));
        handler.afterConnectionClosed(guest, CloseStatus.NORMAL);
    }
}
