package com.tankzors.client.session;

import com.tankzors.client.network.GameSocket;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import static org.junit.jupiter.api.Assertions.*;

class HostGameSessionTest {
    @Test void relayStartPacketCreatesBothPlayersAndUsesIssuedRunId() {
        ArrayDeque<String> incoming = new ArrayDeque<>();
        GameSocket socket = new GameSocket() {
            public void connect() {} public void disconnect() {} public void send(String message) {}
            public String poll() { return incoming.poll(); } public String url() { return "/coop"; }
            public boolean isConnected() { return true; }
        };
        HostGameSession host = new HostGameSession(socket, "user:host", "campaign_1", "stale-run", "v1", null);
        host.loadMapJson("{\"name\":\"Coop\",\"width\":5,\"height\":4,\"tileSize\":40,\"enemies\":[],\"grid\":[[1,1,1,1,1],[1,3,0,3,1],[1,0,9,0,1],[1,1,1,1,1]]}");
        incoming.add("{\"version\":\"v2\",\"type\":\"MATCH_STARTED\",\"runId\":\"issued-coop\",\"payload\":{\"runId\":\"issued-coop\",\"levelId\":\"campaign_1\",\"mapVersion\":\"v1\",\"members\":[{\"playerId\":\"user:host\",\"host\":true},{\"playerId\":\"user:guest\",\"host\":false}]}}");
        host.update(0);
        assertEquals("issued-coop", host.getRunId());
        assertEquals(2, host.snapshot().tanks.stream().filter(tank -> tank.isPlayer).count());
        assertTrue(host.snapshot().tanks.stream().anyMatch(tank -> "user:guest".equals(tank.id)));
    }
}
