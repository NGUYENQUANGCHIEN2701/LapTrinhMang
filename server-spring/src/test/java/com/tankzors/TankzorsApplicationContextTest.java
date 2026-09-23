package com.tankzors;

import com.tankzors.network.RoomRelayHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class TankzorsApplicationContextTest {

    @Autowired ApplicationContext applicationContext;
    @Autowired RoomRelayHandler roomRelayHandler;

    @Test
    void contextLoadsWithCoopRelay() {
        assertNotNull(applicationContext);
        assertNotNull(roomRelayHandler);
    }
}
