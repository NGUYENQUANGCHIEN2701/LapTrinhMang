package com.tankzors.engine;

import com.tankzors.entity.AITank;
import com.tankzors.entity.PlayerTank;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.network.protocol.InputMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CampaignProgressionTest {

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
    @DisplayName("Nạp màn chơi động khi nhận JOIN_GAME với levelId khác nhau")
    public void testDynamicLevelLoading() {
        // Tham gia Màn 1
        InputMessage joinM1 = new InputMessage("JOIN_GAME");
        joinM1.setGameMode("campaign");
        joinM1.setLevelId("campaign_1");
        joinM1.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinM1);

        assertEquals("campaign_1", gameLoop.getCurrentLevelId());
        assertEquals("Màn 1 - Khu vực tập luyện", ((GameMap) gameState.getCurrentMap()).getName());
        long aiCountM1 = gameState.getAllTanks().stream().filter(t -> t instanceof AITank).count();
        assertEquals(1, aiCountM1, "Màn 1 phải có 1 AI");

        // Chuyển sang Màn 2
        InputMessage joinM2 = new InputMessage("JOIN_GAME");
        joinM2.setGameMode("campaign");
        joinM2.setLevelId("campaign_2");
        joinM2.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinM2);

        assertEquals("campaign_2", gameLoop.getCurrentLevelId());
        assertEquals("Màn 2 - Mê cung chiến hào", ((GameMap) gameState.getCurrentMap()).getName());
        long aiCountM2 = gameState.getAllTanks().stream().filter(t -> t instanceof AITank).count();
        assertEquals(2, aiCountM2, "Màn 2 phải có 2 AI");

        // Chuyển sang Màn 3
        InputMessage joinM3 = new InputMessage("JOIN_GAME");
        joinM3.setGameMode("campaign");
        joinM3.setLevelId("campaign_3");
        joinM3.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinM3);

        assertEquals("campaign_3", gameLoop.getCurrentLevelId());
        assertEquals("Màn 3 - Pháo đài tử thủ", ((GameMap) gameState.getCurrentMap()).getName());
        long aiCountM3 = gameState.getAllTanks().stream().filter(t -> t instanceof AITank).count();
        assertEquals(3, aiCountM3, "Màn 3 phải có 3 AI");
    }

    @Test
    @DisplayName("Khi toàn bộ AI bị tiêu diệt, gameStatus phải chuyển thành VICTORY")
    public void testVictoryConditionWhenAllAIsDestroyed() {
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        joinMsg.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinMsg);

        PlayerTank player = (PlayerTank) gameState.getTank("session-1");
        assertNotNull(player);
        assertTrue(player.isAlive());

        List<AITank> aiList = gameState.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(t -> (AITank) t)
                .toList();
        assertEquals(1, aiList.size());

        // Bắn hạ xe AI
        AITank ai = aiList.get(0);
        ai.takeDamage(100);
        assertFalse(ai.isAlive());

        // Chạy tick để dọn xác và kiểm tra điều kiện thắng
        gameLoop.step(0.033);

        assertEquals(GameState.Status.VICTORY, gameState.getStatus(),
                "Khi toàn bộ AI bị tiêu diệt và người chơi còn sống, trạng thái phải là VICTORY");
    }

    @Test
    @DisplayName("Khi người chơi chết, gameStatus phải chuyển thành DEFEAT")
    public void testDefeatConditionWhenPlayerDestroyed() {
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        joinMsg.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinMsg);

        PlayerTank player = (PlayerTank) gameState.getTank("session-1");
        assertNotNull(player);

        player.takeDamage(100);
        assertFalse(player.isAlive());

        gameLoop.step(0.033);

        assertEquals(GameState.Status.DEFEAT, gameState.getStatus(),
                "Khi người chơi tử trận, trạng thái phải là DEFEAT");
    }

    @Test
    @DisplayName("Sau khi VICTORY, người chơi chọn sang Màn 2 thì game khởi động lại ở trạng thái PLAYING với Màn 2")
    public void testProgressionAfterVictory() {
        // 1. Chơi màn 1 và thắng
        InputMessage joinM1 = new InputMessage("JOIN_GAME");
        joinM1.setGameMode("campaign");
        joinM1.setLevelId("campaign_1");
        joinM1.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinM1);

        AITank ai = (AITank) gameState.getAllTanks().stream()
                .filter(t -> t instanceof AITank).findFirst().orElseThrow();
        ai.takeDamage(100);
        gameLoop.step(0.033);
        assertEquals(GameState.Status.VICTORY, gameState.getStatus());

        // 2. Client gửi JOIN_GAME sang màn 2
        InputMessage joinM2 = new InputMessage("JOIN_GAME");
        joinM2.setGameMode("campaign");
        joinM2.setLevelId("campaign_2");
        joinM2.setPlayerName("Player1");
        gameLoop.applyInput("session-1", joinM2);

        // 3. Trạng thái trở lại PLAYING với bản đồ Màn 2 và 2 AI mới
        assertEquals(GameState.Status.PLAYING, gameState.getStatus());
        assertEquals("campaign_2", gameLoop.getCurrentLevelId());
        long newAiCount = gameState.getAllTanks().stream().filter(t -> t instanceof AITank).count();
        assertEquals(2, newAiCount);

        PlayerTank freshPlayer = (PlayerTank) gameState.getTank("session-1");
        assertNotNull(freshPlayer);
        assertEquals(100, freshPlayer.getHP());
    }
}
