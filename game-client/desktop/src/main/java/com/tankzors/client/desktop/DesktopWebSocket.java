package com.tankzors.client.desktop;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tankzors.client.network.GameSocket;

/**
 * Desktop {@link GameSocket} dùng thư viện {@code org.java_websocket}
 * (TooTallNate). Chạy trên desktop/LWJGL3. KHÔNG dùng trong target HTML.
 *
 * <p>Thread-safe: ws callback chạy trên ws thread, enqueue message vào queue;
 * {@link #poll()} chạy an toàn trên render thread.</p>
 *
 * <p>Hỗ trợ auto-reconnect 3 lần, cách nhau 2 giây.</p>
 */
public class DesktopWebSocket implements GameSocket {

    private static final Logger logger = LoggerFactory.getLogger(DesktopWebSocket.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final String url;
    private final ConcurrentLinkedQueue<String> incomingQueue = new ConcurrentLinkedQueue<>();

    private volatile boolean connected = false;
    private volatile boolean intentionalClose = false;

    private int reconnectAttempts = 0;
    private InnerClient client;

    public DesktopWebSocket(String url) {
        this.url = url;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public void connect() {
        if (connected) return;
        intentionalClose = false;
        spawnClient();
    }

    @Override
    public void disconnect() {
        intentionalClose = true;
        connected = false;
        if (client != null) {
            try { client.close(); } catch (Exception ignored) { }
        }
    }

    @Override
    public void send(String message) {
        if (client != null && client.isOpen()) {
            client.send(message);
        } else {
            logger.warn("[DesktopWebSocket] Not sending, socket closed");
        }
    }

    @Override
    public String poll() {
        return incomingQueue.poll();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    private void spawnClient() {
        try {
            client = new InnerClient(new URI(url));
            client.connect();
        } catch (Exception e) {
            logger.error("[DesktopWebSocket] Error creating client to {}: {}", url, e.getMessage());
            handleReconnect();
        }
    }

    private void handleReconnect() {
        if (intentionalClose) return;
        if (reconnectAttempts < MAX_RETRIES) {
            reconnectAttempts++;
            logger.info("[DesktopWebSocket] Reconnect {}/{} after {}ms", reconnectAttempts, MAX_RETRIES, RETRY_DELAY_MS);
            new Thread(() -> {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                spawnClient();
            }, "ws-reconnect").start();
        } else {
            logger.error("[DesktopWebSocket] Gave up after {} retries", MAX_RETRIES);
        }
    }

    /** Inner WebSocketClient; override callback để đưa raw message vào queue. */
    private class InnerClient extends WebSocketClient {
        public InnerClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            reconnectAttempts = 0;
            connected = true;
            logger.info("[DesktopWebSocket] Connected to {}", url);
        }

        @Override
        public void onMessage(String message) {
            incomingQueue.offer(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            connected = false;
            logger.warn("[DesktopWebSocket] Closed code={} reason={}", code, reason);
            handleReconnect();
        }

        @Override
        public void onError(Exception e) {
            logger.error("[DesktopWebSocket] Error: {}", e.getMessage());
        }
    }
}
