package com.tankzors.client.html;

import com.tankzors.client.network.GameSocket;

/**
 * HTML5/GWT {@link GameSocket} dung browser-native WebSocket qua JSNI.
 *
 * <p>KHONG dung {@code org.java_websocket} (desktop lib khong chay trong trinh duyet).</p>
 *
 * <p>Theo plan section 7: ho tro onopen/onmessage/onerror/onclose/send +
 * reconnect co gioi han + dong socket khi dispose.</p>
 *
 * <p>Thread model GWT don tuyen: callback {@code onmessage} enqueue thang
 * vao list noi bo; {@link #poll()} doc list tren render loop (cung tuyen).</p>
 */
public class BrowserWebSocket implements GameSocket {

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private final String url;
    private final java.util.ArrayList<String> incoming = new java.util.ArrayList<>();

    private boolean connected;
    private boolean intentionalClose;
    private int reconnectAttempts;

    // Js handle (giu reference tranh bi GC native websocket).
    private Object socketRef;

    public BrowserWebSocket(String url) {
        this.url = url;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public void connect() {
        intentionalClose = false;
        spawnSocket();
    }

    @Override
    public void disconnect() {
        intentionalClose = true;
        connected = false;
        if (socketRef != null) {
            closeNative(socketRef);
            socketRef = null;
        }
    }

    @Override
    public void send(String message) {
        if (socketRef != null) {
            sendNative(socketRef, message);
        }
    }

    @Override
    public String poll() {
        if (incoming.isEmpty()) return null;
        return incoming.remove(0);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    private void spawnSocket() {
        socketRef = createNative(this, url);
    }

    private void scheduleReconnect() {
        if (intentionalClose) return;
        if (reconnectAttempts >= MAX_RETRIES) return;
        reconnectAttempts++;
        scheduleNative(this, RETRY_DELAY_MS);
    }

    // ===== callbacks invoked tu JSNI =====

    void onOpen() {
        reconnectAttempts = 0;
        connected = true;
    }

    void onMessage(String data) {
        if (data != null && !data.isEmpty()) {
            incoming.add(data);
        }
    }

    void onClose() {
        connected = false;
        socketRef = null;
        scheduleReconnect();
    }

    // ===== JSNI bridges =====
    // Luu y: ben trong khoi JSNI chi dung comment kieu JS don dong "//".

    private static native Object createNative(BrowserWebSocket self, String url) /*-{
        var ws = new WebSocket(url);
        ws.onopen = function() { self.@com.tankzors.client.html.BrowserWebSocket::onOpen()(); };
        ws.onmessage = function(ev) {
            var data = (ev && ev.data != null) ? String(ev.data) : "";
            self.@com.tankzors.client.html.BrowserWebSocket::onMessage(Ljava/lang/String;)(data);
        };
        ws.onerror = function() { // loi: cho onclose reconnect
        };
        ws.onclose = function() { self.@com.tankzors.client.html.BrowserWebSocket::onClose()(); };
        return ws;
    }-*/;

    private static native void sendNative(Object ws, String data) /*-{
        try { ws.send(data); } catch (e) { // bo qua khi chua open
        }
    }-*/;

    private static native void closeNative(Object ws) /*-{
        try { ws.close(); } catch (e) {}
    }-*/;

    private static native void scheduleNative(BrowserWebSocket self, int delayMs) /*-{
        $wnd.setTimeout(function() {
            self.@com.tankzors.client.html.BrowserWebSocket::connect()();
        }, delayMs);
    }-*/;
}
