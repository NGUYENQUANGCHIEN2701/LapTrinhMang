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
}
