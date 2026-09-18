package com.tankzors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.tankzors.engine.GameLoop;
import com.tankzors.network.GameWebSocketHandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 smoke test: Spring Boot context load thành công + bean wiring
 * đầy đủ. Phase 2 phải assert GameLoop bean tồn tại và đang chạy.
 *
 * <p>Phase 4: phải chạy với profile {@code test} (H2) vì default profile
 * là PostgreSQL — không có khi CI.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
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
    void gameLoopBeanPresentButSchedulerOffByDefault() {
        // Bước F: scheduler engine TẮT mặc định (tankzors.engine.scheduler.enabled false).
        // Bean vẫn còn (test simulation cũ cần), nhưng KHÔNG chạy thread.
        assertNotNull(gameLoop, "GameLoop bean vẫn phải tồn tại (test simulation cần)");
        assertTrue(!gameLoop.isRunning(), "Scheduler engine phải off sau Bước F");
    }
}
