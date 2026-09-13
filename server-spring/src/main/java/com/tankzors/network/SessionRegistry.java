package com.tankzors.network;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Quản lý danh sách WebSocket session đang hoạt động.
 *
 * <p>Tương đương {@code ConnectionManager} của server Javalin cũ, thay
 * {@code WsContext} bằng {@code WebSocketSession} của Spring. Không giữ
 * logic game, chỉ thêm/xóa/sessionId.</p>
 */
@Component
public class SessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SessionRegistry.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        if (session == null) return;
        sessions.put(session.getId(), session);
        logger.info("[SessionRegistry] + sessionId={} (tổng={})", session.getId(), sessions.size());
    }

    public void unregister(String sessionId) {
        if (sessionId == null) return;
        WebSocketSession removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("[SessionRegistry] - sessionId={} (tổng={})", sessionId, sessions.size());
        }
    }

    public WebSocketSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public Collection<WebSocketSession> all() {
        return sessions.values();
    }

    public int size() {
        return sessions.size();
    }
}
