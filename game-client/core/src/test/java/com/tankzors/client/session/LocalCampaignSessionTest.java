package com.tankzors.client.session;

import com.tankzors.client.gameplay.GameplayConfig;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.model.TankSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalCampaignSessionTest {

    private static final String MAP_JSON = """
            {
              "name": "Input regression",
              "difficulty": "EASY",
              "width": 7,
              "height": 5,
              "tileSize": 40,
              "enemies": [
                {"type": "AI", "difficulty": "EASY", "spawnX": 5, "spawnY": 2}
              ],
              "grid": [
                [1,1,1,1,1,1,1],
                [1,0,0,0,0,0,1],
                [1,0,3,0,0,4,1],
                [1,0,0,0,0,0,1],
                [1,1,1,1,1,1,1]
              ]
            }
            """;

    /** MapSource mô phỏng REST bất đồng bộ: cache lời gọi requestLevel để test kích callback. */
    private static final class FakeAsyncMapSource implements MapSource {
        LevelCallback pending;
        String requestedLevel;

        @Override public String loadMapJson(String levelId) { return null; }

        @Override
        public void requestLevel(String levelId, LevelCallback callback) {
            requestedLevel = levelId;
            pending = callback; // test sẽ kích onLevelLoaded/onLoadError khi muốn
        }
    }

    private static TankSnapshot player(GameStateSnapshot snapshot) {
        return snapshot.tanks.stream()
                .filter(tank -> tank.isPlayer)
                .findFirst()
                .orElseThrow();
    }

    // ===== Reward: VICTORY mang stars/coins map, DEFEAT = 0 =====

    /**
     * Map KHÔNG có AI + khai báo reward. Khi session bootstrap xong và step một
     * frame, simulation tự detect "kh còn AI" → VICTORY ngay (xem GameSimulation
     * cleanupDeadTanks: hasAI=false → VICTORY). Đây là cách deterministic nhất để
     * thúc đẩy terminal mà không cần truy cập nội bộ GameSimulation.
     */
    private static final String MAP_JSON_VICTORY_REWARDS = """
            {
              "name": "Victory reward test",
              "difficulty": "EASY",
              "width": 7, "height": 5, "tileSize": 40,
              "enemies": [],
              "rewards": {"stars": 3, "coins": 50},
              "grid": [
                [1,1,1,1,1,1,1],
                [1,0,0,0,0,0,1],
                [1,0,3,0,0,0,1],
                [1,0,0,0,0,0,1],
                [1,1,1,1,1,1,1]
              ]
            }
            """;

    /** Map có AI (nhưng ta sẽ không kết thúc qua DEFEAT-manía ở đây) + reward. */
    private static final String MAP_JSON_REWARDS = """
            {
              "name": "Reward test",
              "difficulty": "EASY",
              "width": 7, "height": 5, "tileSize": 40,
              "enemies": [{"type": "AI", "difficulty": "EASY", "spawnX": 5, "spawnY": 2}],
              "rewards": {"stars": 3, "coins": 50},
              "grid": [
                [1,1,1,1,1,1,1],
                [1,0,0,0,0,0,1],
                [1,0,3,0,0,4,1],
                [1,0,0,0,0,0,1],
                [1,1,1,1,1,1,1]
              ]
            }
            """;

    @Test
    void victoryMatchResultCarriesMapReward() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        assertEquals("PLAYING", session.status());

        // không có AI → step 1 frame tự VICTORY.
        session.update((float) GameplayConfig.STEP_SECONDS);

        assertEquals("VICTORY", session.status(), "Map không có AI → VICTORY ngay");
        assertTrue(session.canOpenShop(), "Shop chỉ mở sau khi thắng");
        assertNotNull(session.matchResult());
        assertEquals(3, session.matchResult().starsEarned, "VICTORY nhận stars theo map");
        assertEquals(50, session.matchResult().coinsEarned, "VICTORY nhận coins theo map");
    }

    @Test
    void snapshotCarriesRewardAndDebriefFieldsOnVictory() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        session.update((float) GameplayConfig.STEP_SECONDS);

        GameStateSnapshot snap = session.snapshot();
        assertEquals("VICTORY", snap.gameStatus);
        assertEquals(3, snap.starsEarned, "Snapshot phải cho reward để renderer vẽ debrief");
        assertEquals(50, snap.coinsEarned);
        assertTrue(snap.elapsedSeconds >= 0);
    }

    @Test
    void restartClearsPriorMatchResult() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        session.update((float) GameplayConfig.STEP_SECONDS);
        assertEquals("VICTORY", session.status());
        assertNotNull(session.matchResult());

        // Restart phải reset matchResult + re-bootstrap (runId mới).
        session.restart();
        assertNull(session.matchResult(), "Restart phải xóa matchResult cũ (chống reward trùng)");
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_REWARDS, "run-2", "v1");
        assertEquals("PLAYING", session.status());
        assertFalse(session.canOpenShop(), "Không được mở shop khi màn mới đang chạy");
        assertNull(session.matchResult(), "Chưa kết thúc màn mới → chưa có matchResult");
    }

    // ===== Existing tests (compat path: changeLevel sync) =====

    @Test
    void asyncBootstrapStartsSessionAndAcceptsMovementInput() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);

        session.start();
        assertEquals("campaign_1", source.requestedLevel, "start() phải yêu cầu level ban đầu");
        // Bootstrap async về thành công.
        source.pending.onLevelLoaded("campaign_1", MAP_JSON, "run-1", "v1");

        assertEquals("PLAYING", session.status(),
                "Bootstrap async phải mở khóa input sau khi map được nạp");
        float initialX = player(session.snapshot()).x;

        session.onMove(Direction.RIGHT, true);
        // Giữ qua ngưỡng phân biệt bấm nhanh/giữ để bắt đầu chạy sau khi xoay.
        for (int i = 0; i < 10; i++) {
            session.update((float) GameplayConfig.STEP_SECONDS);
        }

        assertTrue(player(session.snapshot()).x > initialX,
                "Input RIGHT phải được simulation xử lý sau bootstrap");
    }

    @Test
    void invalidBootstrapMapKeepsSessionWaitingThenLoadError() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);

        session.start();
        // Map JSON lỗi → session vào LOAD_ERROR, không crash, không snapshot rỗng.
        source.pending.onLevelLoaded("campaign_1", "{]", "run-2", "v1");

        assertEquals("LOAD_ERROR", session.status(),
                "Map không hợp lệ → session báo lỗi thay vì kẹt WAITING");
        GameStateSnapshot snap = session.snapshot();
        assertEquals("LOAD_ERROR", snap.transitionStatus);
    }

    // ===== Regression: next level / restart / double-request / stale callback =====

    @Test
    void nextLevelAfterVictoryReloadsMapAndKeepsPlaying() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        session.update((float) GameplayConfig.STEP_SECONDS);
        assertEquals("VICTORY", session.status());

        // Giả lập thắng → next level (N).
        session.nextLevel();
        // Trong khi đang tải: status LOADING, input bị chặn, snapshot giữ overlay.
        assertEquals("LOADING", session.status());
        GameStateSnapshot loadingSnap = session.snapshot();
        assertEquals("LOADING", loadingSnap.transitionStatus);

        // Bootstrap màn 2 về.
        source.pending.onLevelLoaded("campaign_2", MAP_JSON, "run-2", "v2");
        assertEquals("PLAYING", session.status());
        assertEquals("campaign_2", session.getLevelId());
        assertEquals("run-2", session.getRunId());
        assertNotNull(player(session.snapshot()), "Có player tank sau khi sang màn mới");
    }

    @Test
    void nextLevelIsBlockedUntilVictory() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON, "run-1", "v1");

        session.nextLevel();

        assertEquals("PLAYING", session.status());
        assertEquals("campaign_1", source.requestedLevel);
    }

    @Test
    void remainingDefaultAmmoCarriesIntoNextLevelDespiteBootstrapInventory() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoadedWithShop("campaign_1", MAP_JSON_VICTORY_REWARDS,
                "run-1", "v1", 0, 0, "{}", "{\"0\":100}");
        session.getShopState().setItemCount(0, 73);
        session.update((float) GameplayConfig.STEP_SECONDS);
        assertEquals("VICTORY", session.status());

        session.nextLevel();
        source.pending.onLevelLoadedWithShop("campaign_2", MAP_JSON,
                "run-2", "v2", 0, 0, "{}", "{\"0\":100}");

        assertEquals(73, session.snapshot().activeWeaponAmmo,
                "Bootstrap màn kế không được làm mới số đạn còn lại");
    }

    @Test
    void restartRebootstrapIssuesNewRunId() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON, "run-1", "v1");
        String firstRun = session.getRunId();

        session.restart();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON, "run-retry", "v1");
        assertEquals("PLAYING", session.status());
        assertFalse(session.getRunId().equals(firstRun),
                "Restart phải nhận runId mới để tránh trùng kết quả idempotency");
    }

    @Test
    void inventoryMenuPausesAndRestartRestoresLevelStartInventory() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON, "run-1", "v1");

        session.openInventoryMenu();
        assertTrue(session.isInventoryMenuOpen());
        GameStateSnapshot openMenu = session.snapshot();
        assertArrayEquals(new int[]{0, 19, 17}, openMenu.inventoryItemIds,
                "HUD Q phải liệt kê Round, Repair, Speed ngay trong túi hiện tại");
        assertArrayEquals(new int[]{100, 3, 2}, openMenu.inventoryItemCounts);
        int elapsed = openMenu.elapsedSeconds;
        session.update(5f);
        assertEquals(elapsed, session.snapshot().elapsedSeconds, "Menu Q phải pause simulation");

        // Menu mặc định: Round, Repair, Speed.
        session.moveInventorySelection(2);
        session.confirmInventorySelection();
        assertFalse(session.isInventoryMenuOpen());
        assertEquals(1, session.snapshot().speedCount);
        assertTrue(session.snapshot().speedBoostSecondsRemaining > 19f);

        session.restart();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON, "run-2", "v1");
        assertEquals(2, session.snapshot().speedCount,
                "Chơi lại phải khôi phục túi đồ tại thời điểm bắt đầu màn");
        assertEquals(0f, session.snapshot().speedBoostSecondsRemaining);
    }

    @Test
    void loadErrorKeepsOldWorldIntact() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        session.update((float) GameplayConfig.STEP_SECONDS);
        String prevRun = session.getRunId();
        assertNotNull(player(session.snapshot()));

        // nextLevel lỗi → không bị world rỗng, vẫn ở màn 1.
        session.nextLevel();
        source.pending.onLevelLoaded("campaign_2", "{]", "run-bad", "v1");
        assertEquals("LOAD_ERROR", session.status());
        assertEquals("campaign_1", session.getLevelId(), "Giữ nguyên màn hiện tại khi tải lỗi");
        assertEquals(prevRun, session.getRunId());
        // Snapshot vẫn có world cũ (player còn).
        assertNotNull(player(session.snapshot()));
    }

    @Test
    void pressingNextMultipleTimesOnlyFiresOneRequest() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        source.pending.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        session.update((float) GameplayConfig.STEP_SECONDS);

        session.nextLevel();
        MapSource.LevelCallback firstCallback = source.pending;
        String firstTarget = source.requestedLevel;
        // Nhấn N thêm 3 lần khi đang LOADING_NEXT → phải bị edge guard chặn.
        session.nextLevel();
        session.nextLevel();
        session.nextLevel();
        MapSource.LevelCallback after = source.pending;
        assertEquals(firstCallback, after, "Không phát request thứ hai khi đang tải");
        assertEquals(firstTarget, source.requestedLevel);
    }

    @Test
    void staleCallbackDoesNotOverwriteCurrentSession() {
        FakeAsyncMapSource source = new FakeAsyncMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "player", "campaign_1", source, result -> true);
        session.start();
        MapSource.LevelCallback initialCallback = source.pending;
        initialCallback.onLevelLoaded("campaign_1", MAP_JSON_VICTORY_REWARDS, "run-1", "v1");
        session.update((float) GameplayConfig.STEP_SECONDS);
        assertEquals("VICTORY", session.status());

        // User nextLevel sang campaign_2 rồi nextLevel nhanh sang campaign_3
        // (đơn giản: tưởng tượng 1 request cũ về muộn với token đã lỗi thời).
        session.nextLevel();
        MapSource.LevelCallback cb2 = source.pending;
        // Trước khi cb2 về, user đổi ý nextLevel... nhưng cùng đang LOADING_NEXT sẽ bị chặn,
        // nên ta mô phỏng restart (chuyển hướng đích tải khác).
        session.restart();                       // đích mới = campaign_1
        MapSource.LevelCallback cbRestart = source.pending;

        // Callback cũ (campaign_2) về muộn → phải bị reject, không ghi đè.
        cb2.onLevelLoaded("campaign_2", MAP_JSON, "run-stale", "stale");
        assertEquals("campaign_1", session.getLevelId(),
                "Callback của request cũ không được ghi đè session hiện tại");

        // Callback hợp lệ (restart) xử lý bình thường.
        cbRestart.onLevelLoaded("campaign_1", MAP_JSON, "run-fresh", "v1");
        assertEquals("PLAYING", session.status());
        assertEquals("run-fresh", session.getRunId());
    }
}
