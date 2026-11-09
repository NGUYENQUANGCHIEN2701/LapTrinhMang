package com.tankzors.network;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registers the co-op relay. Gameplay simulation remains on the host client. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomRelayHandler roomRelayHandler;
    private final RelayHandshakeInterceptor relayHandshakeInterceptor;

    public WebSocketConfig(RoomRelayHandler roomRelayHandler,
                           RelayHandshakeInterceptor relayHandshakeInterceptor) {
        this.roomRelayHandler = roomRelayHandler;
        this.relayHandshakeInterceptor = relayHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roomRelayHandler, "/coop")
                .addInterceptors(relayHandshakeInterceptor);
    }
}
