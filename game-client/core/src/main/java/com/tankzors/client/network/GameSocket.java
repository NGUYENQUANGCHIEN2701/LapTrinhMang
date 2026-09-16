package com.tankzors.client.network;

/**
 * Abstraction cho kết nối realtime giữa client và server.
 *
 * <p>Core không biết transport cụ thể. Desktop dùng
 * {@code org.java_websocket} (xem desktop module), còn HTML5/GWT dùng
 * browser-native WebSocket qua JsInterop (xem html module).</p>
 *
 * <p>Hànhvi:</p>
 * <ul>
 *   <li>{@link #connect()} kết nối không block. Khi mở thành công, trạng thái
 *       chuyển {@code CONNECTED}.</li>
 *   <li>{@link #send(String)} đẩy raw JSON lên server. Nếu chưa kết nối,
 *       message có thể bị bỏ qua (không throw).</li>
 *   <li>{@link #poll()} lấy raw JSON server gửi về. Trả {@code null} nếu rỗng.
 *       Luôn chạy trên render thread (LibGDX main loop).</li>
 *   <li>{@link #disconnect()} đóng socket dọn dẹp. Idempotent.</li>
 * </ul>
 *
 * <p>Thread-safe: ws callback chạy trên ws thread, enqueue vào queue nội bộ;
 *       {@link #poll()} chạy an toàn trên render thread.</p>
 */
public interface GameSocket {

    void connect();

    void disconnect();

    void send(String message);

    /**
     * Lấy raw JSON server gửi về, hoặc {@code null} nếu không có message.
     */
    String poll();

    /** URL endpoint (vd {@code ws://localhost:8081/game}). */
    String url();

    /** Đã mở kết nối vật lý chưa (CONNECT đều từ server). */
    boolean isConnected();
}
