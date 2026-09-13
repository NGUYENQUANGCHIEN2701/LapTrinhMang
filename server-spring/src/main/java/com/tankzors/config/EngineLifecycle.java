package com.tankzors.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.tankzors.ServerConfig;
import com.tankzors.engine.GameLoop;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Lifecycle wrapper: start/stop {@link GameLoop} khi context up/down.
 * Tách riêng khỏi {@link EngineConfig} để tránh phụ thuộc vòng khi Spring
 * dựng bean.
 */
@Configuration
public class EngineLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(EngineLifecycle.class);

    private final GameLoop gameLoop;

    public EngineLifecycle(GameLoop gameLoop) {
        this.gameLoop = gameLoop;
    }

    @PostConstruct
    public void start() {
        gameLoop.start();
        logger.info("[EngineLifecycle] GameLoop started @ {}Hz ({}ms/tick)",
            ServerConfig.TICK_RATE, ServerConfig.TICK_INTERVAL_MS);
    }

    @PreDestroy
    public void stop() {
        gameLoop.stop();
        logger.info("[EngineLifecycle] GameLoop stopped");
    }
}
