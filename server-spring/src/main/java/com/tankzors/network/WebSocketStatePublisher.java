package com.tankzors.network;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.Gson;
import com.tankzors.engine.StatePublisherPort;

/**
 * Adapter {@link StatePublisherPort} -> Spring WebSocket.
 *
 * <p>Được inject vào {@link com.tankzors.engine.GameLoop} qua interface
 * port. Serialize object thành JSON rồi broadcast tới toàn bộ session
 * trong {@link SessionRegistry}.</p>
 *
 * <p>Tương đương {@code MessageSender.broadcast()} của server Javalin cũ,
 * trừ việc không có {@code send()} riêng tới một WsContext (Phase 2 không
 * cần riêng lẻ; client-side rendering đủ với STATE broadcast).</p>
 */
@Component
public class WebSocketStatePublisher implements StatePublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketStatePublisher.class);

    private final SessionRegistry sessionRegistry;
    private final Gson gson;

    public WebSocketStatePublisher(SessionRegistry sessionRegistry, Gson gson) {
        this.sessionRegistry = sessionRegistry;
        this.gson = gson;
    }

    @Override
    public void broadcast(Object message) {
        if (message == null) return;
        String json = gson.toJson(message);
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessionRegistry.all()) {
            if (!session.isOpen()) continue;
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                logger.warn("[WebSocketStatePublisher] Lỗi gửi tới {}: {}", session.getId(), e.getMessage());
                sessionRegistry.unregister(session.getId());
            }
        }
        logger.debug("[WebSocketStatePublisher] broadcast {} bytes tới {} session(s)",
            json.length(), sessionRegistry.size());
    }
}
