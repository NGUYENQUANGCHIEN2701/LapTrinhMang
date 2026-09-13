package com.tankzors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.tankzors.network.GameWebSocketHandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 1 smoke test: Spring Boot context load thành công + bean wiring
 * cơ bản. Phase 2 sẽ thay bằng WebSocket integration test thực.
 */
@SpringBootTest
class TankzorsApplicationContextTest {

    @Autowired
    ApplicationContext applicationContext;

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
}
