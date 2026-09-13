package com.tankzors.network;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.tankzors.protocol.ProtocolConstants;

/**
 * Phase 1: đăng ký path {@code /game} với handler placeholder.
 *
 * <p>Phase 2 sẽ inject {@code GameWebSocketHandler} thực, kết nối tới
 * engine + session service + shared-protocol packet. Hiện tại chỉ khai
 * báo để Spring Boot có endpoint session handshake nhưng chưa xử lý
 * message (Phase 1 chỉ nghiệm thu build pass).</p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, ProtocolConstants.WS_PATH).setAllowedOrigins("*");
    }
}
