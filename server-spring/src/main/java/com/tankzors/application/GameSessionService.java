package com.tankzors.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tankzors.engine.GameLoop;
import com.tankzors.network.protocol.InputMessage;

/**
 * Service application: queue input từ WebSocket vào {@link GameLoop}.
 *
 * <p>Tương đương {@code MessageHandler.setInputListener} của server Javalin
 * cũ, giờ là service có tên rõ ràng và có thể inject vào WebSocket handler.</p>
 *
 * <p>Phase 2 không nắm thêm logic game ở đây - SRP. Phase 4 có thể thêm
 * unlock/leaderboard port sau khi có PostgreSQL.</p>
 */
@Service
public class GameSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GameSessionService.class);

    private final GameLoop gameLoop;

    public GameSessionService(GameLoop gameLoop) {
        this.gameLoop = gameLoop;
    }

    public void handleInput(String sessionId, InputMessage input) {
        logger.debug("[GameSessionService] sessionId={} action={} direction={} moving={} levelId={}",
            sessionId,
            input.getAction(),
            input.getDirection(),
            input.getMoving(),
            input.getLevelId());
        gameLoop.queueInput(sessionId, input);
    }

    public void handleDisconnect(String sessionId) {
        logger.debug("[GameSessionService] disconnect sessionId={}", sessionId);
        gameLoop.queueInput(sessionId, new InputMessage("LEAVE_GAME"));
    }
}
