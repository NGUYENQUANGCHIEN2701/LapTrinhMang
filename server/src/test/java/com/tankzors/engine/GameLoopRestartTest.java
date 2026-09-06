package com.tankzors.engine;

import com.tankzors.entity.AITank;
import com.tankzors.entity.PlayerTank;
import com.tankzors.interfaces.ITank;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.network.protocol.InputMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameLoopRestartTest {

    private GameState gameState;
    private GameLoop gameLoop;

    @BeforeEach
    public void setUp() {
        gameState = new GameState();
        GameMap map = MapLoader.load("campaign_1");
        assertNotNull(map);
        gameState.setCurrentMap(map);

        gameLoop = new GameLoop(gameState, null);
    }

    @Test
    @DisplayName("Khi player bị tiêu diệt và F5 vào lại game (JOIN_GAME mới), AI phải reset về vị trí ban đầu và đầy máu")
    public void testAIResetsWhenPlayerRejoinsAfterDeath() {
        // 1. Player 1 tham gia game
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        joinMsg.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinMsg);

        // Kiểm tra Player và AI đã được tạo
        PlayerTank player1 = (PlayerTank) gameState.getTank("session-1");
        assertNotNull(player1);
        assertEquals(100, player1.getHP());

        List<AITank> aiTanks = gameState.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(t -> (AITank) t)
                .toList();
        assertEquals(1, aiTanks.size(), "Phải có 1 xe địch được spawn trong map campaign_1");
        AITank ai = aiTanks.get(0);
        assertEquals(100, ai.getHP());
        double initialAiX = ai.getPosition().getX();
        double initialAiY = ai.getPosition().getY();

        // 2. Mô phỏng xe địch bị bắn mất máu và di chuyển sang vị trí khác
        ai.takeDamage(50);
        ai.getPosition().setX(initialAiX - 50);
        ai.getPosition().setY(initialAiY - 50);
        assertEquals(50, ai.getHP());

        // 3. Mô phỏng Player bị tiêu diệt hoàn toàn
        player1.takeDamage(100);
        assertFalse(player1.isAlive());

        // Chạy 1 tick để gameLoop dọn dẹp tank chết
        gameLoop.step(0.033);

        // Player đã bị dọn khỏi gameState và status chuyển sang DEFEAT
        assertNull(gameState.getTank("session-1"));
        assertEquals(GameState.Status.DEFEAT, gameState.getStatus(), "Game phải chuyển sang DEFEAT khi người chơi chết");

        // 4. Player F5 lại trang web -> Client tạo session mới và gửi JOIN_GAME
        InputMessage rejoinMsg = new InputMessage("JOIN_GAME");
        rejoinMsg.setGameMode("campaign");
        rejoinMsg.setLevelId("campaign_1");
        rejoinMsg.setPlayerName("Player1");
        gameLoop.applyInput("session-2", rejoinMsg);

        // 5. Xác minh:
        // - Trạng thái game là PLAYING
        assertEquals(GameState.Status.PLAYING, gameState.getStatus());

        // - Player mới được spawn tại vị trí ban đầu với 100 HP
        PlayerTank player2 = (PlayerTank) gameState.getTank("session-2");
        assertNotNull(player2);
        assertEquals(100, player2.getHP());

        // - Xe địch AI cũ đã bị xóa, xe địch AI mới xuất hiện tại vị trí ban đầu với 100 HP!
        List<AITank> resetAiTanks = gameState.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(t -> (AITank) t)
                .toList();
        assertEquals(1, resetAiTanks.size(), "Phải chỉ có 1 xe địch mới");
        AITank freshAi = resetAiTanks.get(0);
        assertEquals(100, freshAi.getHP(), "Máu của xe địch phải được reset về 100 đầy đủ");
        assertEquals(initialAiX, freshAi.getPosition().getX(), 1e-3, "Toạ độ X của xe địch phải quay về vị trí spawn ban đầu");
        assertEquals(initialAiY, freshAi.getPosition().getY(), 1e-3, "Toạ độ Y của xe địch phải quay về vị trí spawn ban đầu");
    }
}
