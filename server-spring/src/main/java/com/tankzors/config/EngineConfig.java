package com.tankzors.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tankzors.engine.GameLoop;
import com.tankzors.engine.GameState;
import com.tankzors.engine.StatePublisherPort;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.network.InputPacketParser;

import com.google.gson.Gson;

/**
 * Khai báo bean cho engine (GameLoop + GameState) và parser.
 *
 * <p>Bean lifecycle (start/stop game loop) nằm ở {@link EngineLifecycle}
 * để tránh phụ thuộc vòng (Spring không cho @Configuration bean gọi
 * method @Bean của nó trong constructor).</p>
 */
@Configuration
public class EngineConfig {

    private static final Logger logger = LoggerFactory.getLogger(EngineConfig.class);

    @Bean
    public GameState gameState() {
        GameState state = new GameState();
        GameMap defaultMap = MapLoader.load("campaign_1");
        if (defaultMap != null) {
            state.setCurrentMap(defaultMap);
            logger.info("[EngineConfig] Đã nạp bản đồ mặc định: {}", defaultMap.getName());
        } else {
            logger.warn("[EngineConfig] Không nạp được campaign_1, game sẽ WAITING đến khi JOIN_GAME");
        }
        return state;
    }

    @Bean
    public GameLoop gameLoop(GameState gameState, StatePublisherPort statePublisher) {
        return new GameLoop(gameState, statePublisher);
    }

    @Bean
    public InputPacketParser inputPacketParser(Gson gson) {
        return new InputPacketParser(gson);
    }
}
