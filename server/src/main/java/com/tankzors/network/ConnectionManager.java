package com.tankzors.network;

import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh sách các kết nối WebSocket đang hoạt động.
 *
 * <p>Tuân thủ nguyên tắc Single Responsibility Principle (SRP):
 * Chỉ tập trung vào việc quản lý danh sách session, thêm/xóa session
 * và gửi dữ liệu thô qua các session kết nối.</p>
 */
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    // Lưu trữ thread-safe danh sách kết nối: SessionId -> WsContext
    private final Map<String, WsContext> sessions = new ConcurrentHashMap<>();

    /**
     * Thêm một kết nối mới vào danh sách quản lý.
     *
     * @param ctx ngữ cảnh WebSocket của client vừa kết nối
     */
    public void addConnection(WsContext ctx) {
        if (ctx == null) return;
        String sessionId = ctx.sessionId();
        sessions.put(sessionId, ctx);
        logger.info("🟢 [ConnectionManager] Client đã kết nối: sessionId={}, Tổng số clients={}",
                sessionId, sessions.size());
    }

    /**
     * Xóa kết nối khỏi danh sách khi client ngắt kết nối.
     *
     * @param ctx ngữ cảnh WebSocket của client vừa ngắt kết nối
     */
    public void removeConnection(WsContext ctx) {
        if (ctx == null) return;
        removeConnection(ctx.sessionId());
    }

    /**
     * Xóa kết nối theo sessionId.
     *
     * @param sessionId ID của phiên kết nối cần xóa
     */
    public void removeConnection(String sessionId) {
        if (sessionId == null) return;
        WsContext removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("🔴 [ConnectionManager] Client đã ngắt kết nối: sessionId={}, Tổng số clients={}",
                    sessionId, sessions.size());
        }
    }

    /**
     * Lấy ngữ cảnh kết nối của một client theo sessionId.
     *
     * @param sessionId ID của phiên kết nối
     * @return WsContext tương ứng, hoặc null nếu không tồn tại
     */
    public WsContext getConnection(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    /**
     * Lấy toàn bộ danh sách các kết nối đang hoạt động.
     *
     * @return Collection chứa tất cả WsContext hiện tại
     */
    public Collection<WsContext> getAllConnections() {
        return sessions.values();
    }

    /**
     * Lấy số lượng kết nối hiện tại.
     *
     * @return số lượng client đang kết nối
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * Gửi chuỗi tin nhắn tới một client cụ thể.
     *
     * @param sessionId ID của client nhận tin nhắn
     * @param message   chuỗi JSON cần gửi
     * @return true nếu gửi thành công, false nếu client không tồn tại hoặc lỗi
     */
    public boolean sendTo(String sessionId, String message) {
        WsContext ctx = getConnection(sessionId);
        if (ctx != null && ctx.session.isOpen()) {
            try {
                ctx.send(message);
                return true;
            } catch (Exception e) {
                logger.error("❌ Lỗi khi gửi tin nhắn tới sessionId={}: {}", sessionId, e.getMessage());
                removeConnection(sessionId);
            }
        }
        return false;
    }

    /**
     * Gửi tin nhắn tới toàn bộ các client đang kết nối (Broadcast).
     *
     * @param message chuỗi JSON cần phát sóng
     */
    public void broadcast(String message) {
        for (Map.Entry<String, WsContext> entry : sessions.entrySet()) {
            WsContext ctx = entry.getValue();
            if (ctx.session.isOpen()) {
                try {
                    ctx.send(message);
                } catch (Exception e) {
                    logger.error("❌ Lỗi broadcast tới sessionId={}: {}", entry.getKey(), e.getMessage());
                    removeConnection(entry.getKey());
                }
            }
        }
    }

    /**
     * Gửi tin nhắn tới toàn bộ client ngoại trừ một client chỉ định.
     *
     * @param excludeSessionId ID của client không nhận tin nhắn
     * @param message          chuỗi JSON cần phát sóng
     */
    public void broadcastExcept(String excludeSessionId, String message) {
        for (Map.Entry<String, WsContext> entry : sessions.entrySet()) {
            if (entry.getKey().equals(excludeSessionId)) continue;
            WsContext ctx = entry.getValue();
            if (ctx.session.isOpen()) {
                try {
                    ctx.send(message);
                } catch (Exception e) {
                    logger.error("❌ Lỗi broadcast tới sessionId={}: {}", entry.getKey(), e.getMessage());
                    removeConnection(entry.getKey());
                }
            }
        }
    }
}
