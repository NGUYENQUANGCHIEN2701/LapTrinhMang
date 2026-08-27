package com.tankzors;

import com.tankzors.engine.GameLoop;
import com.tankzors.engine.GameState;
import com.tankzors.network.MessageHandler;
import com.tankzors.network.MessageSender;
import com.tankzors.network.WebSocketServer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Khởi tạo ứng dụng Javalin
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        });

        // REST API kiểm tra trạng thái server (Health check)
        app.get("/", ctx -> ctx.result("Tankzors Server is running"));
        app.get("/api/health", ctx -> ctx.json("{\"status\":\"UP\",\"port\":" + ServerConfig.PORT + "}"));

        // Khởi tạo và đăng ký WebSocket Server tại /game
        WebSocketServer webSocketServer = new WebSocketServer();
        webSocketServer.registerEndpoints(app);

        // ========== GAME ENGINE ==========

        // Khởi tạo GameState và GameLoop
        GameState gameState = new GameState();
        MessageSender messageSender = webSocketServer.getMessageSender();
        GameLoop gameLoop = new GameLoop(gameState, messageSender);

        // Kết nối input từ network -> game loop queue
        MessageHandler messageHandler = webSocketServer.getMessageHandler();
        messageHandler.setInputListener((sessionId, inputMessage) -> {
            gameLoop.queueInput(sessionId, inputMessage);
        });

        // Khởi động game loop (30Hz)
        gameLoop.start();

        // ========== GRACEFUL SHUTDOWN (Ctrl+C) ==========

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[Shutdown] Nhận tín hiệu tắt server...");
            gameLoop.stop();
            app.stop();
            logger.info("[Shutdown] Server đã dừng sạch.");
        }, "Shutdown-Hook"));

        // Bắt đầu lắng nghe kết nối
        app.start(ServerConfig.PORT);

        logger.info("=================================================");
        logger.info("Tankzors Server started on port {}", ServerConfig.PORT);
        logger.info("HTTP API:       http://localhost:{}", ServerConfig.PORT);
        logger.info("WebSocket Game: ws://localhost:{}/game", ServerConfig.PORT);
        logger.info("Game Loop:      {}Hz ({}ms/tick)", ServerConfig.TICK_RATE, ServerConfig.TICK_INTERVAL_MS);
        logger.info("=================================================");
    }
}
