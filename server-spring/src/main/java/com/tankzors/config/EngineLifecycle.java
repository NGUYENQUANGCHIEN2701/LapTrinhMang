package com.tankzors.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import com.tankzors.ServerConfig;
import com.tankzors.engine.GameLoop;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Lifecycle wrapper: start/stop {@link GameLoop} khi context up/down.
 *
 * <p><b>Bước F (client-authoritative)</b>: scheduler engine mặc định TẮT trong
 * production (tankzors.engine.scheduler.enabled=false). Backend nay chỉ phục vụ
 * website + relay co-op + persistence; KHÔNG chạy scheduled game-loop thread,
 * AI, collision hay tính điểm (luật gameplay đã chuyển sang client core).
 * Bean vẫn còn để test simulation cũ chạy được; chỉ không tự start thread.</p>
 */
@Configuration
@ConditionalOnProperty(name = "tankzors.engine.scheduler.enabled", havingValue = "true")
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

