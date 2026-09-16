package com.tankzors.network;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tra cứu kết nối WS theo connectionId cho {@link RoomRelayHandler} (server
 * Spring không expose registry session công khai). Handshake interceptor đăng ký,
 * close unregister.
 */
public final class SessionHolder {
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    private SessionHolder() {}

    static void put(WebSocketSession s) { if (s != null && s.getId() != null) SESSIONS.put(s.getId(), s); }
    static void remove(String id) { if (id != null) SESSIONS.remove(id); }
    public static WebSocketSession get(String id) { return id == null ? null : SESSIONS.get(id); }

    /** Gọi khi close nhận diện để dọn. */
    public static void onClosed(String id) { remove(id); }
}
