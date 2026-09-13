package com.tankzors.network;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.tankzors.protocol.ConnectPacket;

/**
 * Phase 1 placeholder WebSocket handler.
 *
 * <p>Phase 1: chỉ handle onConnect -> gửi {@link ConnectPacket} đúng v1
 * shape. Phase 2 sẽ route JOIN_GAME/MOVE_INPUT/SHOOT_INPUT vào
 * GameSessionService.</p>
 *
 * <p>Lưu ý: không thay thế Javalin endpoint trong module này - chạy trên
 * port khác (8081 mặc định) để không xung đột legacy server (8080).</p>
 */
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final Gson gson;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public GameWebSocketHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        try {
            ConnectPacket pkt = new ConnectPacket(
                session.getId(),
                "Kết nối tới Tankzors Spring Server thành công!"
            );
            session.sendMessage(new TextMessage(gson.toJson(pkt)));
        } catch (IOException e) {
            logger.warn("[GameWebSocketHandler] Lỗi gửi CONNECT tới {}: {}", session.getId(), e.getMessage());
        }
        logger.info("[GameWebSocketHandler] Client đã kết nối: sessionId={}, tổng={}",
            session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("[GameWebSocketHandler] Đã nhận (Phase 1 chỉ log): sessionId={}, payload={}",
            session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        logger.info("[GameWebSocketHandler] Client ngắt kết nối: sessionId={}, status={}",
            session.getId(), status);
    }

    public Map<String, WebSocketSession> getSessions() {
        return sessions;
    }
}
