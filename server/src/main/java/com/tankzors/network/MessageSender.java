package com.tankzors.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tankzors.network.protocol.GameMessage;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chịu trách nhiệm chuyển đổi (Serialize) đối tượng message thành JSON và gửi đi.
 *
 * <p>Tuân thủ nguyên tắc Single Responsibility Principle (SRP):
 * Chỉ tập trung vào việc định dạng JSON và giao tiếp qua ConnectionManager/WsContext.</p>
 */
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final ConnectionManager connectionManager;
    private final Gson gson;

    public MessageSender(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.gson = new GsonBuilder().create();
    }

    /**
     * Gửi message trực tiếp qua một WsContext.
     *
     * @param ctx     ngữ cảnh WebSocket
     * @param message đối tượng message cần gửi
     */
    public void send(WsContext ctx, Object message) {
        if (ctx == null || !ctx.session.isOpen()) return;
        try {
            String json = gson.toJson(message);
            ctx.send(json);
            logger.debug("📤 [MessageSender] Gửi tới {}: {}", ctx.sessionId(), json);
        } catch (Exception e) {
            logger.error("❌ [MessageSender] Lỗi gửi message tới {}: {}", ctx.sessionId(), e.getMessage());
        }
    }

    /**
     * Gửi message tới một client theo sessionId qua ConnectionManager.
     *
     * @param sessionId ID phiên của người nhận
     * @param message   đối tượng message
     */
    public void sendTo(String sessionId, Object message) {
        try {
            String json = gson.toJson(message);
            connectionManager.sendTo(sessionId, json);
            logger.debug("📤 [MessageSender] Gửi tới {}: {}", sessionId, json);
        } catch (Exception e) {
            logger.error("❌ [MessageSender] Lỗi gửi message tới {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Broadcast message tới toàn bộ client đang kết nối.
     *
     * @param message đối tượng message
     */
    public void broadcast(Object message) {
        try {
            String json = gson.toJson(message);
            connectionManager.broadcast(json);
            logger.debug("📢 [MessageSender] Broadcast: {}", json);
        } catch (Exception e) {
            logger.error("❌ [MessageSender] Lỗi broadcast: {}", e.getMessage());
        }
    }

    /**
     * Broadcast message tới toàn bộ client ngoại trừ một client chỉ định.
     *
     * @param excludeSessionId ID phiên loại trừ
     * @param message          đối tượng message
     */
    public void broadcastExcept(String excludeSessionId, Object message) {
        try {
            String json = gson.toJson(message);
            connectionManager.broadcastExcept(excludeSessionId, json);
        } catch (Exception e) {
            logger.error("❌ [MessageSender] Lỗi broadcastExcept: {}", e.getMessage());
        }
    }

    public Gson getGson() {
        return gson;
    }
}
