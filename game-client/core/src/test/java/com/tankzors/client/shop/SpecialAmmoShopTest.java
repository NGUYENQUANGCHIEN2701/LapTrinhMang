package com.tankzors.client.shop;

import com.tankzors.client.gameplay.SpecialAmmoRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpecialAmmoShopTest {
    @Test void partialPacksRefundExcessAndOldAmmoCannotBeSold() {
        PlayerShopState state = new PlayerShopState();
        state.setResearchLevel("shellResearch", 1);
        state.setItemCount(1, 95);
        state.setCoins(100);
        state.beginShopVisit();
        assertFalse(state.canSell(ShopItem.PLASMA));
        assertTrue(state.buy(ShopItem.PLASMA));
        assertEquals(99, state.getItemCount(1));
        assertEquals(94, state.getCoins());
        assertFalse(state.sell(ShopItem.PLASMA));
        state.endShopVisit();
        state.beginShopVisit();
        assertFalse(state.canSell(ShopItem.PLASMA));
    }

    @Test void pricesLimitsAndUnlocksMatchBuilds() {
        int[] ids = {6,7,8,10,11,12,13,14,9};
        int[] prices = {100,60,320,10,25,35,60,110,170};
        PlayerShopState state = new PlayerShopState();
        for (int i = 0; i < ids.length; i++) {
            ShopItem item = ShopItem.findById(ids[i]);
            assertEquals(prices[i], state.getEffectivePrice(item));
            assertEquals(99, state.getEffectiveLimit(item));
            assertFalse(state.isItemUnlocked(item));
            assertFalse(state.getUnlockHint(item).isEmpty());
            state.setResearchLevel(item.getRequiredTech(), item.getRequiredTechLevel());
            if (item == ShopItem.AIR_SUPPORT) {
                assertFalse(state.isItemUnlocked(item));
                assertEquals("Can mua Radar", state.getUnlockHint(item));
                state.setItemCount(23, 1);
            }
            assertTrue(state.isItemUnlocked(item));
            state.setItemCount(ids[i], 1);
            state.selectWeapon(ids[i]);
            assertEquals(ids[i], state.getActiveWeaponId());
            assertEquals("", state.getUnlockHint(item));
        }
    }
    @Test void descriptionsReflectResearchAndAllOneShotItemsDisableAutofire() {
        PlayerShopState state = new PlayerShopState();
        state.setResearchLevel("artilleryResearch", 5);
        assertTrue(state.getItemDescription(ShopItem.ARTILLERY).contains("luc 17, tam 5 o, vung no 7 o"));
        state.setResearchLevel("airRaidResearch", 3);
        assertTrue(state.getItemDescription(ShopItem.AIR_SUPPORT).contains("7 bom, luc 21, vung 7 o"));
        assertTrue(SpecialAmmoRules.repeatsWhileHeld(6));
        assertTrue(SpecialAmmoRules.repeatsWhileHeld(7));
        for (int id = 8; id <= 14; id++) assertFalse(SpecialAmmoRules.repeatsWhileHeld(id));
    }
}
