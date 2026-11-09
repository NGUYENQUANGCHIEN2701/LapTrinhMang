package com.tankzors.client.shop;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class EconomyJournalTest {
    @Test void journalRecordsTransactionsRatherThanWalletSnapshots() {
        PlayerShopState state = new PlayerShopState(); state.setCoins(100); state.enableOperationRecording();
        state.beginShopVisit(); assertTrue(state.buy(ShopItem.SPEED_UP)); assertTrue(state.sell(ShopItem.SPEED_UP));
        assertTrue(state.consumeItem(19)); state.recordPickup(2, 3);
        JsonValue ops = new JsonReader().parse(state.drainOperations());
        assertEquals(5, ops.size); assertEquals("OPEN", ops.get(0).getString("type"));
        assertEquals("BUY", ops.get(1).getString("type")); assertEquals("SELL", ops.get(2).getString("type"));
        assertEquals("CONSUME", ops.get(3).getString("type")); assertEquals(19, ops.get(3).getInt("itemId"));
        assertEquals("PICKUP", ops.get(4).getString("type")); assertFalse(state.hasPendingOperations());
        assertFalse(state.drainOperations().contains("coins"));
    }
    @Test void shieldInstallAndLostShieldHaveDistinctOperations() {
        PlayerShopState state = new PlayerShopState(); state.setItemCount(20, 2); state.enableOperationRecording();
        state.consumeItem(20); state.addInstalledForceField(1); state.clearInstalledForceField();
        JsonValue ops = new JsonReader().parse(state.drainOperations());
        assertEquals(2, ops.size); assertEquals("INSTALL", ops.get(0).getString("type"));
        assertEquals(-3, ops.get(1).getInt("itemId"));
    }
    @Test void authoritativeLoadReplacesZeroBalancesAndRemovedItems() {
        PlayerShopState state = new PlayerShopState(); state.setCoins(50); state.setStars(20);
        state.setResearchLevel("shellResearch", 4);
        state.loadState(0, 0, "{}", "{\"0\":3}");
        assertEquals(0, state.getCoins()); assertEquals(0, state.getStars());
        assertEquals(0, state.getResearchLevel("shellResearch")); assertEquals(0, state.getItemCount(19));
    }
    @Test void pricingMetadataMatchesClientCatalog() throws Exception {
        JsonValue data = new JsonReader().parse(Files.readString(Path.of("../assets/economy/catalog.json")));
        assertEquals(ShopItem.values().length, data.get("items").size);
        for (JsonValue entry = data.get("items").child; entry != null; entry = entry.next) {
            ShopItem item = ShopItem.findById(entry.getInt("id")); assertNotNull(item);
            assertEquals(item.getBasePrice(), entry.getInt("price")); assertEquals(item.getPackSize(), entry.getInt("pack"));
            assertEquals(item.getBaseLimit(), entry.getInt("limit")); assertEquals(item.getRequiredTechLevel(), entry.getInt("level"));
            assertEquals(item.getRequiredTech(), entry.getString("tech", null));
        }
        for (JsonValue entry = data.get("research").child; entry != null; entry = entry.next) {
            ResearchTech tech = ResearchTech.findById(entry.getString("id")); assertNotNull(tech);
            assertArrayEquals(tech.getStarPrices(), entry.get("prices").asIntArray());
        }
    }
}
