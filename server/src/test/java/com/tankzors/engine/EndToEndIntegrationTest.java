package com.tankzors.engine;

import com.tankzors.entity.AITank;
import com.tankzors.entity.Direction;
import com.tankzors.entity.PlayerTank;
import com.tankzors.entity.Position;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ITank;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.network.protocol.InputMessage;
import com.tankzors.network.protocol.StateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task [1.14] Tích hợp end-to-end: client gửi lệnh → server xử lý → client hiển thị.
 *
 * Kiểm tra toàn bộ 5 bước của kịch bản test:
 * 1. Mở Campaign → nạp bản đồ 1 tank xanh (Player) và 1 tank đỏ (AI).
 * 2. Bấm W → tank xanh đi lên, bị chặn bởi tường không xuyên tường.
 * 3. Bấm Space → đạn bay ra trúng tank đỏ → tank đỏ mất máu (100 -> 75).
 * 4. Bắn hết máu → tank đỏ bị tiêu diệt và biến mất → trạng thái VICTORY.
 * 5. Ngắt kết nối → dọn dẹp game state và chuyển về WAITING.
 */
public class EndToEndIntegrationTest {

    private GameState gameState;
    private GameLoop gameLoop;

    @BeforeEach
    public void setUp() {
        gameState = new GameState();
        GameMap map = MapLoader.load("campaign_1");
        assertNotNull(map, "Bản đồ campaign_1 phải tồn tại");
        gameState.setCurrentMap(map);
        gameLoop = new GameLoop(gameState, null);
    }

    @Test
    @DisplayName("Bước 1: Player tham gia Campaign Màn 1 → Sinh 1 tank người chơi và 1 tank AI")
    public void testStep1JoinGameSpawnsPlayerAndEnemy() {
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        joinMsg.setPlayerName("Hero");

        gameLoop.applyInput("session-hero", joinMsg);
        gameLoop.step(0.033);

        assertEquals(GameState.Status.PLAYING, gameState.getStatus());

        // Kiểm tra Player Tank
        ITank playerTank = gameState.getTank("session-hero");
        assertNotNull(playerTank, "Player tank phải được tạo trong gameState");
        assertTrue(playerTank instanceof PlayerTank);
        assertEquals(100, playerTank.getHP());
        assertEquals(100.0, playerTank.getPosition().getX(), 1.0);
        assertEquals(100.0, playerTank.getPosition().getY(), 1.0);

        // Kiểm tra AI Tank
        List<AITank> aiTanks = gameState.getAllTanks().stream()
                .filter(t -> t instanceof AITank)
                .map(t -> (AITank) t)
                .toList();
        assertEquals(1, aiTanks.size(), "Bản đồ Màn 1 chỉ có đúng 1 xe địch");
        AITank ai = aiTanks.get(0);
        assertEquals(100, ai.getHP());
        assertTrue(ai.isAlive());
    }

    @Test
    @DisplayName("Bước 2: Bấm W đi lên → Tank xanh di chuyển và bị chặn bởi tường biên, không xuyên tường")
    public void testStep2MoveUpStopsAtWall() {
        // 1. Join game
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        gameLoop.applyInput("session-hero", joinMsg);

        PlayerTank player = (PlayerTank) gameState.getTank("session-hero");
        double startY = player.getPosition().getY();

        // 2. Bấm phím W (Direction UP, moving = true)
        InputMessage moveUp = new InputMessage("MOVE_INPUT");
        moveUp.setDirection(Direction.UP);
        moveUp.setMoving(true);
        gameLoop.applyInput("session-hero", moveUp);

        // Chạy 5 tick di chuyển
        for (int i = 0; i < 5; i++) {
            gameLoop.step(0.033);
        }

        // Vị trí Y phải giảm (đi lên phía trên)
        assertTrue(player.getPosition().getY() < startY, "Tank phải đi lên phía trên khi nhấn W");

        // Chạy thêm 50 tick để tank đâm vào tường trên cùng (hàng 0 là tường từ Y=0..40)
        for (int i = 0; i < 50; i++) {
            gameLoop.step(0.033);
        }

        // Tường ô 0 chiếm từ 0 đến 40. Tank kích thước 40x40 (bán kính 20)
        // Vị trí tâm tank Y tối thiểu không thể < 40 + 20 = 60
        assertTrue(player.getPosition().getY() >= 60.0 - 1e-3,
                "Tank không được xuyên qua tường trên cùng (Y hiện tại: " + player.getPosition().getY() + ")");

        // 3. Nhả phím W (moving = false)
        InputMessage stopUp = new InputMessage("MOVE_INPUT");
        stopUp.setDirection(Direction.UP);
        stopUp.setMoving(false);
        gameLoop.applyInput("session-hero", stopUp);
        gameLoop.step(0.033);

        double stoppedY = player.getPosition().getY();
        gameLoop.step(0.033);
        assertEquals(stoppedY, player.getPosition().getY(), 1e-3, "Tank phải đứng yên sau khi nhả phím");
    }

    @Test
    @DisplayName("Bước 3: Bấm Space bắn đạn → Đạn bay trúng xe địch → Xe địch mất 25 máu")
    public void testStep3ShootBulletDamagesEnemy() {
        // 1. Join game
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        gameLoop.applyInput("session-hero", joinMsg);

        PlayerTank player = (PlayerTank) gameState.getTank("session-hero");
        AITank ai = (AITank) gameState.getAllTanks().stream().filter(t -> t instanceof AITank).findFirst().orElseThrow();

        // Đặt người chơi ngay phía trên xe địch (khoảng cách 50px) và quay mặt xuống dưới (180 độ)
        player.getPosition().setX(ai.getPosition().getX());
        player.getPosition().setY(ai.getPosition().getY() - 50);
        player.rotate(180);

        // 2. Gửi lệnh SHOOT
        InputMessage shootMsg = new InputMessage("SHOOT_INPUT");
        gameLoop.applyInput("session-hero", shootMsg);
        gameLoop.step(0.033);

        // Xác nhận đạn của người chơi đã được tạo trong gameState
        IBullet playerBullet = gameState.getAllBullets().stream()
                .filter(b -> "session-hero".equals(b.getOwnerId()))
                .findFirst()
                .orElse(null);
        assertNotNull(playerBullet, "Phải có viên đạn của người chơi");

        // 3. Chạy thêm vài tick để đạn chạm vào xe địch
        for (int i = 0; i < 10; i++) {
            gameLoop.step(0.033);
        }

        // Xe địch nhận 25 sát thương -> máu giảm từ 100 xuống 75
        assertEquals(75, ai.getHP(), "Máu của xe địch phải giảm 25 sau khi bị bắn trúng");
        assertFalse(playerBullet.isActive(), "Viên đạn phải bị hủy sau khi va chạm với xe tăng");
    }

    @Test
    @DisplayName("Bước 4: Bắn hết máu (HP = 0) → Tank địch bị phá hủy biến mất và thắng trận (VICTORY)")
    public void testStep4DestroyEnemyAchievesVictory() {
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        gameLoop.applyInput("session-hero", joinMsg);

        PlayerTank player = (PlayerTank) gameState.getTank("session-hero");
        AITank ai = (AITank) gameState.getAllTanks().stream().filter(t -> t instanceof AITank).findFirst().orElseThrow();

        // Đặt người chơi ngay trước mặt xe địch
        player.getPosition().setX(ai.getPosition().getX());
        player.getPosition().setY(ai.getPosition().getY() - 50);
        player.rotate(180);

        // Đưa máu AI về 25 để viên đạn tiếp theo kết liễu
        ai.takeDamage(75);
        assertEquals(25, ai.getHP());

        // Bắn viên đạn kết liễu
        player.setShootCooldown(0);
        InputMessage shootMsg = new InputMessage("SHOOT_INPUT");
        gameLoop.applyInput("session-hero", shootMsg);

        // Chạy các tick để đạn va chạm và tiêu diệt AI
        for (int i = 0; i < 10; i++) {
            gameLoop.step(0.033);
        }

        // Xe địch phải bị xóa khỏi game state
        assertNull(gameState.getTank(ai.getId()), "Xe địch chết phải được dọn khỏi gameState");
        assertTrue(gameState.getScore() >= 100, "Người chơi phải nhận được ít nhất 100 điểm khi hạ gục xe địch");
        assertEquals(GameState.Status.VICTORY, gameState.getStatus(), "Game phải chuyển sang trạng thái VICTORY");
    }

    @Test
    @DisplayName("Bước 5: Ngắt kết nối (LEAVE_GAME) → Tank người chơi bị xóa và game về WAITING")
    public void testStep5DisconnectCleansUpSession() {
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        gameLoop.applyInput("session-hero", joinMsg);
        assertEquals(GameState.Status.PLAYING, gameState.getStatus());

        // Client ngắt kết nối
        InputMessage leaveMsg = new InputMessage("LEAVE_GAME");
        gameLoop.applyInput("session-hero", leaveMsg);

        assertNull(gameState.getTank("session-hero"), "Tank người chơi phải bị xóa sau khi ngắt kết nối");
        assertEquals(GameState.Status.WAITING, gameState.getStatus(), "Game phải chuyển về WAITING");
    }

    @Test
    @DisplayName("Đóng gói trạng thái StateMessage chứa đầy đủ dữ liệu map, tanks, bullets cho client")
    public void testStateMessageSerialization() {
        InputMessage joinMsg = new InputMessage("JOIN_GAME");
        joinMsg.setGameMode("campaign");
        joinMsg.setLevelId("campaign_1");
        gameLoop.applyInput("session-hero", joinMsg);

        StateMessage msg = gameState.toStateMessage();
        assertNotNull(msg);
        assertEquals("STATE", msg.getType());
        assertEquals("PLAYING", msg.getGameStatus());
        assertNotNull(msg.getMap());
        assertEquals(20, msg.getMap().get("width"));
        assertEquals(15, msg.getMap().get("height"));
        assertFalse(msg.getTanks().isEmpty());
    }
}
