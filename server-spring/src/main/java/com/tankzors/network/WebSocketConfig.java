package com.tankzors.network;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Đăng ký endpoints WebSocket.
 * <ul>
 *   <li>{@code /game}: v1 server-authoritative (Bước F sẽ gỡ).</li>
 *   <li>{@code /coop}: v2 relay co-op (Bước E). Handshake resolve identity từ HTTP session.</li>
 * </ul>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final RoomRelayHandler roomRelayHandler;
    private final RelayHandshakeInterceptor relayHandshakeInterceptor;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler,
                           RoomRelayHandler roomRelayHandler,
                           RelayHandshakeInterceptor relayHandshakeInterceptor) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.roomRelayHandler = roomRelayHandler;
        this.relayHandshakeInterceptor = relayHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/game").setAllowedOrigins("*");
        registry.addHandler(roomRelayHandler, "/coop")
                .addInterceptors(relayHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
