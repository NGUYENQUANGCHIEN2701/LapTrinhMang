package com.tankzors.network;

import com.tankzors.identity.PlayerIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Handshake interceptor cho /coop endpoint: resolve player identity từ HTTP
 * (session/header) — handshake HTTP có cookie JSESSIONID — rồi lưu vào WS
 * session attributes cho {@link RoomRelayHandler}. Đồng thời đăng ký vào
 * {@link SessionHolder}.
 */
@Component
public class RelayHandshakeInterceptor implements HandshakeInterceptor {

    private final PlayerIdentityResolver identity;

    public RelayHandshakeInterceptor(PlayerIdentityResolver identity) {
        this.identity = identity;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servlet) {
            HttpServletRequest req = servlet.getServletRequest();
            String playerId = identity.resolve(req);
            attributes.put("tankzors.playerId", playerId);
        } else {
            attributes.put("tankzors.playerId", "anon");
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // SessionHolder.put/get cần session id; ta không có ở đây (session chưa tạo).
        // Việc register diễn ra sau via RoomRelayHandler.afterConnectionEstablished.
    }
}
