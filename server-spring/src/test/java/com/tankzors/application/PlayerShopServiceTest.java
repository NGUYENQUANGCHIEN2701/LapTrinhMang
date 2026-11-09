package com.tankzors.application;

import com.tankzors.progress.PlayerTotalsRepository;
import com.tankzors.result.GameRunService;
import com.tankzors.result.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class PlayerShopServiceTest {
    @Autowired PlayerShopService shop;
    @Autowired GameRunService runs;
    @Autowired PlayerTotalsRepository totals;
    @Autowired PlayerProgressService progress;
    private String player, run;
    @BeforeEach void init() {
        player = "shop-" + UUID.randomUUID(); run = runs.issue(player, "campaign_1").getRunId();
        shop.getShopData(player);
        var wallet = totals.findByPlayerId(player).orElseThrow(); wallet.setTotalCoins(1000); wallet.setTotalStars(100);
        totals.save(wallet);
    }
    private PlayerShopService.Operation op(String type, Integer item, String tech, Integer amount) {
        return new PlayerShopService.Operation(type, item, tech, amount, null, null);
    }
    private PlayerShopService.SyncRequest request(long version, PlayerShopService.Operation... ops) {
        return new PlayerShopService.SyncRequest(run, UUID.randomUUID().toString(), version, List.of(ops));
    }
    @Test void buyAndRefundUseServerPriceAndCannotMintMoney() {
        var result = shop.syncShopData(player, request(0, op("OPEN", null, null, null),
                op("BUY", 17, null, null), op("SELL", 17, null, null)));
        assertEquals(1000, result.totalCoins()); assertEquals(1, result.revision());
        assertThrows(IllegalArgumentException.class, () -> shop.syncShopData(player,
                request(1, op("SELL", 17, null, null))));
    }
    @Test void repeatedRequestDoesNotSpendTwice() {
        var req = request(0, op("BUY", 17, null, null)); var first = shop.syncShopData(player, req);
        var retry = shop.syncShopData(player, req);
        assertEquals(965, first.totalCoins()); assertEquals(first, retry);
    }
    @Test void staleRequestCannotOverwriteNewerState() {
        shop.syncShopData(player, request(0, op("BUY", 17, null, null)));
        assertThrows(ResultService.ConflictException.class, () -> shop.syncShopData(player, request(0, op("BUY", 17, null, null))));
        assertEquals(965, shop.getShopData(player).totalCoins());
    }
    @Test void changedPayloadUnderSameRequestIdConflicts() {
        var req = request(0, op("BUY", 17, null, null)); shop.syncShopData(player, req);
        var other = new PlayerShopService.SyncRequest(run, req.requestId(), 0L, List.of(op("BUY", 19, null, null)));
        assertThrows(ResultService.ConflictException.class, () -> shop.syncShopData(player, other));
    }
    @Test void lockedItemAndOverspendingAreRejectedWithoutPartialWrite() {
        assertThrows(IllegalArgumentException.class, () -> shop.syncShopData(player, request(0, op("BUY", 1, null, null))));
        assertThrows(IllegalArgumentException.class, () -> shop.syncShopData(player,
                request(0, op("BUY", -1, null, null), op("BUY", 23, null, null))));
        assertEquals(1000, shop.getShopData(player).totalCoins());
    }
    @Test void researchChargesStarsAndUnlocksPurchase() {
        var result = shop.syncShopData(player, request(0, op("RESEARCH", null, "shellResearch", null), op("BUY", 1, null, null)));
        assertEquals(95, result.totalStars()); assertEquals(985, result.totalCoins());
        assertTrue(result.inventoryJson().contains("\"1\":10"));
    }
    @Test void consumingCannotIncreaseInventoryOrConsumePermanentEquipment() {
        assertThrows(IllegalArgumentException.class, () -> shop.syncShopData(player, request(0, op("CONSUME", 0, null, -1))));
        assertThrows(IllegalArgumentException.class, () -> shop.syncShopData(player, request(0, op("CONSUME", 0, null, 100))));
        var out = shop.syncShopData(player, request(0, op("CONSUME", 0, null, 4)));
        assertTrue(out.inventoryJson().contains("\"0\":95"));
    }
    @Test void cannotUseOtherPlayersRun() {
        String other = "other-" + UUID.randomUUID();
        assertThrows(ResultService.ConflictException.class, () -> shop.syncShopData(other, request(0, op("BUY", 17, null, null))));
    }
    @Test void pickupIsDerivedFromMapAndCannotBeClaimedTwice() {
        progress.markVictory(player, "campaign_1"); progress.markVictory(player, "campaign_2");
        run = runs.issue(player, "campaign_3").getRunId();
        var map = com.tankzors.map.MapLoader.load("campaign_3");
        int col = -1, row = -1;
        for (int y = 0; y < map.getGrid().length; y++) for (int x = 0; x < map.getGrid()[y].length; x++)
            if (map.getGrid()[y][x] == 11) { col = x; row = y; }
        assertTrue(col >= 0);
        var pickup = new PlayerShopService.Operation("PICKUP", null, null, null, col, row);
        var out = shop.syncShopData(player, request(0, pickup)); assertEquals(101, out.totalStars());
        assertThrows(ResultService.ConflictException.class, () -> shop.syncShopData(player, request(1, pickup)));
    }
    @Test void concurrentIdenticalPurchaseIsAppliedOnce() throws Exception {
        var req = request(0, op("BUY", 17, null, null));
        var gate = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<PlayerShopService.ShopData> task = () -> { gate.await(); return shop.syncShopData(player, req); };
            var a = pool.submit(task); var b = pool.submit(task); gate.countDown();
            assertEquals(a.get(10, java.util.concurrent.TimeUnit.SECONDS), b.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(965, shop.getShopData(player).totalCoins());
    }
    @Test void emptyArsenalRecoveryIsServerIssuedAndNotRepeated() {
        var wallet = totals.findByPlayerId(player).orElseThrow(); wallet.setTotalCoins(0); totals.save(wallet);
        shop.syncShopData(player, request(0, op("CONSUME", 0, null, 99)));
        var recovered = shop.beginRun(player);
        assertTrue(recovered.inventoryJson().contains("\"0\":99"));
        assertEquals(recovered.revision(), shop.beginRun(player).revision());
    }
    @Test void newlyUnlockedAmmoCanBeRefundedDuringTheSameVisit() {
        var out = shop.syncShopData(player, request(0, op("OPEN", null, null, null),
                op("RESEARCH", null, "shellResearch", null), op("BUY", 1, null, null), op("SELL", 1, null, null)));
        assertEquals(1000, out.totalCoins()); assertEquals(95, out.totalStars());
        assertTrue(out.inventoryJson().contains("\"1\":0"));
    }
}
