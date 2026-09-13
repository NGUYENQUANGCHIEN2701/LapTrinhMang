package com.tankzors.network;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;

/**
 * Cung cấp bean cho {@link GameWebSocketHandler} - Phase 1 chỉ inject Gson,
 * Phase 2 sẽ inject thêm GameSessionService + input port.
 */
@Configuration
public class GameHandlerConfig {

    @Bean
    public GameWebSocketHandler gameWebSocketHandler(Gson gson) {
        return new GameWebSocketHandler(gson);
    }
}
