package com.tankzors.network;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình và khởi tạo WebSocket Server trên nền Javalin.
 *
 * <p>Định tuyến endpoint {@code /game} và điều phối các sự kiện kết nối
 * tới ConnectionManager, MessageHandler và MessageSender.</p>
 */
public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final ConnectionManager connectionManager;
    private final MessageSender messageSender;
    private final MessageHandler messageHandler;

    public WebSocketServer() {
        this.connectionManager = new ConnectionManager();
        this.messageSender = new MessageSender(this.connectionManager);
        this.messageHandler = new MessageHandler(this.messageSender);
    }

    public WebSocketServer(ConnectionManager connectionManager, MessageSender messageSender, MessageHandler messageHandler) {
        this.connectionManager = connectionManager;
        this.messageSender = messageSender;
        this.messageHandler = messageHandler;
    }

    /**
     * Đăng ký WebSocket endpoint {@code /game} vào ứng dụng Javalin.
     *
     * @param app đối tượng Javalin
     */
    public void registerEndpoints(Javalin app) {
        app.ws("/game", ws -> {
            // Khi có một client kết nối đến
            ws.onConnect(ctx -> {
                connectionManager.addConnection(ctx);

                // Gửi thông điệp chào mừng (Welcome/Connect packet)
                Map<String, Object> welcome = new HashMap<>();
                welcome.put("type", "CONNECT");
                welcome.put("sessionId", ctx.sessionId());
                welcome.put("message", "Kết nối tới Tankzors Server thành công!");
                welcome.put("timestamp", System.currentTimeMillis());
                messageSender.send(ctx, welcome);
            });

            // Khi nhận được message JSON từ client
            ws.onMessage(ctx -> {
                messageHandler.handleMessage(ctx);
            });

            // Khi client ngắt kết nối
            ws.onClose(ctx -> {
                connectionManager.removeConnection(ctx);
                messageHandler.handleDisconnect(ctx.sessionId());
            });

            // Khi có lỗi kết nối
            ws.onError(ctx -> {
                logger.error("⚠️ [WebSocketServer] Lỗi socket tại session {}: {}",
                        ctx.sessionId(), ctx.error() != null ? ctx.error().getMessage() : "Unknown error");
            });
        });

        logger.info("⚡ [WebSocketServer] Đã đăng ký WebSocket endpoint /game");
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
}
