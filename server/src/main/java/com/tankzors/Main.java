package com.tankzors;

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

        // Bắt đầu lắng nghe kết nối
        app.start(ServerConfig.PORT);

        logger.info("=================================================");
        logger.info("🚀 Tankzors Server started on port {}", ServerConfig.PORT);
        logger.info("🌐 HTTP API:       http://localhost:{}", ServerConfig.PORT);
        logger.info("🔌 WebSocket Game: ws://localhost:{}/game", ServerConfig.PORT);
        logger.info("=================================================");
    }
}
