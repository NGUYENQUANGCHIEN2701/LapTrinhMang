package com.tankzors.client.network;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket bridge đến Tankzors server.
 *
 * <p>Dùng thư viện {@code org.java_websocket} (TooTallNate) vì nó chạy được
 * trên cả desktop (LWJGL3) và vòng đời server HTML (phase HTML5 sau này sẽ
 * swap sang browser native WebSocket qua JsInterop).</p>
 *
 * <p>Tương đương {@code WebSocketClient.ts} của React client. Hỗ trợ
 * auto-reconnect 3 lần cách nhau 2 giây (parity với React).</p>
 *
 * <p>Thread-safe: ws callback chạy trên ws thread, enqueue raw message vào
 * queue để GameClient thread poll trong {@code render()}.</p>
 */
public class WebSocketBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketBridge.class);

    public enum Status { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final String url;
    private final ConcurrentLinkedQueue<String> incomingQueue = new ConcurrentLinkedQueue<>();

    private volatile Status status = Status.DISCONNECTED;
    private volatile String sessionId;
    private volatile boolean intentionalClose = false;

    private int reconnectAttempts = 0;
    private InnerClient client;

    public WebSocketBridge(String url) {
        this.url = url;
    }

    public void connect() {
        if (status == Status.CONNECTING || status == Status.CONNECTED) return;
        intentionalClose = false;
        status = Status.CONNECTING;
        spawnClient();
    }

    public void disconnect() {
        intentionalClose = true;
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
        status = Status.DISCONNECTED;
    }

    private void spawnClient() {
        try {
            client = new InnerClient(new URI(url));
            client.connect();
        } catch (Exception e) {
            logger.error("[WebSocketBridge] Lỗi tạo client tới {}: {}", url, e.getMessage());
            status = Status.ERROR;
            handleReconnect();
        }
    }

    private void handleReconnect() {
        if (intentionalClose) return;
        if (reconnectAttempts < MAX_RETRIES) {
            reconnectAttempts++;
            status = Status.CONNECTING;
            logger.info("[WebSocketBridge] Reconnect {}/{} sau {}ms", reconnectAttempts, MAX_RETRIES, RETRY_DELAY_MS);
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
            logger.error("[WebSocketBridge] Vượt quá {} reconnect attempts", MAX_RETRIES);
            status = Status.ERROR;
        }
    }

    /**
     * Poll incoming raw JSON. GameClient thread gọi mỗi frame.
     */
    public String poll() {
        return incomingQueue.poll();
    }

    public void sendRaw(String json) {
        if (client != null && client.isOpen()) {
            client.send(json);
        } else {
            logger.warn("[WebSocketBridge] Không gửi được: status={}", status);
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String id) {
        this.sessionId = id;
    }

    public boolean isConnected() {
        return status == Status.CONNECTED;
    }

    /**
     * Inner WebSocketClient override callback để đưa raw message vào queue.
     */
    private class InnerClient extends WebSocketClient {
        public InnerClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            reconnectAttempts = 0;
            status = Status.CONNECTED;
            logger.info("[WebSocketBridge] Đã kết nối tới {}", url);
        }

        @Override
        public void onMessage(String message) {
            incomingQueue.offer(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            status = Status.DISCONNECTED;
            logger.warn("[WebSocketBridge] Đóng code={} reason={}", code, reason);
            handleReconnect();
        }

        @Override
        public void onError(Exception e) {
            status = Status.ERROR;
            logger.error("[WebSocketBridge] Lỗi: {}", e.getMessage());
        }
    }
}
