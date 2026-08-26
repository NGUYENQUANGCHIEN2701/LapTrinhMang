package com.tankzors.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tankzors.network.protocol.ErrorMessage;
import com.tankzors.network.protocol.InputMessage;
import io.javalin.websocket.WsMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Xử lý và phân giải các message JSON từ client gửi lên server.
 *
 * <p>Tuân thủ nguyên tắc Single Responsibility Principle (SRP):
 * Chỉ tập trung vào việc đọc, giải mã JSON và định tuyến (routing) message,
 * KHÔNG xử lý logic game trực tiếp.</p>
 */
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final MessageSender messageSender;
    private final Gson gson;

    // Callback lắng nghe input của người chơi (dành cho GameEngine đăng ký)
    private BiConsumer<String, InputMessage> inputListener;

    public MessageHandler(MessageSender messageSender) {
        this.messageSender = messageSender;
        this.gson = messageSender.getGson();
    }

    /**
     * Tiếp nhận message từ WebSocket client, phân tích cú pháp và xử lý.
     *
     * @param ctx ngữ cảnh tin nhắn WebSocket của Javalin
     */
    public void handleMessage(WsMessageContext ctx) {
        String rawMessage = ctx.message();
        String sessionId = ctx.sessionId();
        logger.info("📩 [MessageHandler] Nhận từ sessionId={}: {}", sessionId, rawMessage);

        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            messageSender.send(ctx, new ErrorMessage("Message không được để trống", 400));
            return;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(rawMessage).getAsJsonObject();
            String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "UNKNOWN";

            switch (type.toUpperCase()) {
                case "INPUT":
                case "MOVE_INPUT":
                case "SHOOT_INPUT":
                case "JOIN_GAME":
                    handleInputMessage(ctx, jsonObject, rawMessage);
                    break;

                case "PING":
                    handlePing(ctx);
                    break;

                default:
                    handleGenericMessage(ctx, type, rawMessage);
                    break;
            }
        } catch (Exception e) {
            logger.error("❌ [MessageHandler] Lỗi giải mã message từ {}: {}", sessionId, e.getMessage());
            messageSender.send(ctx, new ErrorMessage("Định dạng JSON không hợp lệ: " + e.getMessage(), 400));
        }
    }

    /**
     * Xử lý gói tin Input (MOVE, SHOOT, JOIN...).
     */
    private void handleInputMessage(WsMessageContext ctx, JsonObject jsonObject, String rawMessage) {
        InputMessage inputMessage = gson.fromJson(rawMessage, InputMessage.class);

        // Nếu type là MOVE_INPUT / SHOOT_INPUT / JOIN_GAME từ client TypeScript
        if (inputMessage.getAction() == null) {
            inputMessage.setAction(inputMessage.getType());
        }

        logger.debug("🎮 [MessageHandler] Input parsed: {}", inputMessage);

        // Chuyển tiếp cho GameEngine nếu đã đăng ký listener
        if (inputListener != null) {
            inputListener.accept(ctx.sessionId(), inputMessage);
        }

        // Gửi ACK phản hồi xác nhận cho client (đáp ứng tiêu chí nghiệm thu Task 1.4)
        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "ACK");
        ack.put("status", "RECEIVED");
        ack.put("action", inputMessage.getAction());
        ack.put("timestamp", System.currentTimeMillis());
        messageSender.send(ctx, ack);
    }

    /**
     * Xử lý ping kiểm tra kết nối từ client.
     */
    private void handlePing(WsMessageContext ctx) {
        Map<String, Object> pong = new HashMap<>();
        pong.put("type", "PONG");
        pong.put("timestamp", System.currentTimeMillis());
        messageSender.send(ctx, pong);
    }

    /**
     * Xử lý các tin nhắn thông thường khác và phản hồi echo.
     */
    private void handleGenericMessage(WsMessageContext ctx, String type, String rawMessage) {
        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "ACK");
        ack.put("receivedType", type);
        ack.put("message", "Server đã nhận message thành công");
        ack.put("timestamp", System.currentTimeMillis());
        messageSender.send(ctx, ack);
    }

    /**
     * Đăng ký listener nhận input từ client (GameEngine sẽ gọi phương thức này).
     *
     * @param listener hàm nhận sessionId và InputMessage
     */
    public void setInputListener(BiConsumer<String, InputMessage> listener) {
        this.inputListener = listener;
    }
}
