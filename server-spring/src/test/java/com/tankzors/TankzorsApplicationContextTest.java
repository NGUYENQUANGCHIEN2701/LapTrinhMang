package com.tankzors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.tankzors.engine.GameLoop;
import com.tankzors.network.GameWebSocketHandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 smoke test: Spring Boot context load thành công + bean wiring
 * đầy đủ. Phase 2 phải assert GameLoop bean tồn tại và đang chạy.
 */
@SpringBootTest
class TankzorsApplicationContextTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    GameLoop gameLoop;

    @Autowired
    GameWebSocketHandler gameWebSocketHandler;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void gameWebSocketHandlerIsRegistered() {
        assertNotNull(gameWebSocketHandler);
    }

    @Test
    void gameLoopStarted() {
        assertNotNull(gameLoop);
        assertTrue(gameLoop.isRunning(), "GameLoop phải running sau context load");
    }
}
