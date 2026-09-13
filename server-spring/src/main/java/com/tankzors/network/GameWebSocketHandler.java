package com.tankzors.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.tankzors.application.GameSessionService;
import com.tankzors.network.protocol.ErrorMessage;
import com.tankzors.network.protocol.InputMessage;

/**
 * Phase 2 WebSocket handler: route packet vào {@link GameSessionService}.
 *
 * <p>Tương đương {@code MessageHandler.handleMessage} của server Javalin cũ,
 * với {@link InputPacketParser} flat hoá payload trước khi bind vào
 * {@link InputMessage}.</p>
 *
 * <p>Wire format giữ nguyên v1 (tham khảo {@code docs/protocol-v1.md}).
 * Vì check parity với client React cũ, mọi alias (INPUT/JOIN_GAME/MOVE_INPUT/
 * SHOOT_INPUT/STOP/LEAVE_GAME) đều được chấp nhận.</p>
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final Gson gson;
    private final SessionRegistry sessionRegistry;
    private final InputPacketParser inputParser;
    private final GameSessionService gameSessionService;

    public GameWebSocketHandler(
        Gson gson,
        SessionRegistry sessionRegistry,
        InputPacketParser inputParser,
        GameSessionService gameSessionService
    ) {
        this.gson = gson;
        this.sessionRegistry = sessionRegistry;
        this.inputParser = inputParser;
        this.gameSessionService = gameSessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionRegistry.register(session);
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "CONNECT");
        welcome.put("sessionId", session.getId());
        welcome.put("message", "Kết nối tới Tankzors Server thành công!");
        welcome.put("timestamp", System.currentTimeMillis());
        sendJson(session, welcome);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String raw = message.getPayload();
        String sessionId = session.getId();
        logger.info("[GameWebSocketHandler] << sessionId={} payload={}", sessionId, raw);

        if (raw == null || raw.isBlank()) {
            sendJson(session, new ErrorMessage("Message không được để trống", 400));
            return;
        }

        try {
            // Parse để xem type (chưa bind InputMessage, vì có thể là PING)
            var typeNode = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            String type = typeNode.has("type") ? typeNode.get("type").getAsString().toUpperCase() : "UNKNOWN";

            switch (type) {
                case "PING":
                    Map<String, Object> pong = new HashMap<>();
                    pong.put("type", "PONG");
                    pong.put("timestamp", System.currentTimeMillis());
                    sendJson(session, pong);
                    return;

                case "INPUT":
                case "JOIN_GAME":
                case "MOVE_INPUT":
                case "SHOOT_INPUT":
                case "MOVE":
                case "SHOOT":
                case "STOP":
                case "LEAVE_GAME":
                    InputMessage input = inputParser.parse(raw);
                    gameSessionService.handleInput(sessionId, input);
                    sendAck(session, input.getAction() != null ? input.getAction() : type);
                    return;

                default:
                    Map<String, Object> ack = new HashMap<>();
                    ack.put("type", "ACK");
                    ack.put("receivedType", type);
                    ack.put("message", "Server đã nhận message thành công");
                    ack.put("timestamp", System.currentTimeMillis());
                    sendJson(session, ack);
            }
        } catch (Exception e) {
            logger.warn("[GameWebSocketHandler] Lỗi parse từ {}: {}", sessionId, e.getMessage());
            sendJson(session, new ErrorMessage("Định dạng JSON không hợp lệ: " + e.getMessage(), 400));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionRegistry.unregister(session.getId());
        gameSessionService.handleDisconnect(session.getId());
    }

    private void sendAck(WebSocketSession session, String action) throws IOException {
        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "ACK");
        ack.put("status", "RECEIVED");
        ack.put("action", action);
        ack.put("timestamp", System.currentTimeMillis());
        sendJson(session, ack);
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        if (session.isOpen()) {
            synchronized (session) {
                session.sendMessage(new TextMessage(gson.toJson(payload)));
            }
        }
    }
}
