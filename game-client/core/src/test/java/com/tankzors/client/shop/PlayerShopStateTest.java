package com.tankzors.client.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerShopStateTest {

    private PlayerShopState state;

    @BeforeEach
    void setUp() {
        state = new PlayerShopState();
    }

    @Test
    void testInitialState() {
        assertEquals(0, state.getCoins());
        assertEquals(0, state.getStars());
        assertEquals(100, state.getItemCount(ShopItem.ROUND.getId()));
        assertEquals(3, state.getItemCount(ShopItem.REPAIR.getId()));
        assertEquals(2, state.getItemCount(ShopItem.SPEED_UP.getId()));
        assertEquals(0, state.getActiveWeaponId());
    }

    @Test
    void inventoryCheckpointCanBeRestored() {
        var checkpoint = state.snapshotInventory();
        assertTrue(state.consumeItem(ShopItem.REPAIR.getId()));
        assertTrue(state.consumeItem(ShopItem.SPEED_UP.getId()));
        state.restoreInventory(checkpoint);
        assertEquals(3, state.getItemCount(ShopItem.REPAIR.getId()));
        assertEquals(2, state.getItemCount(ShopItem.SPEED_UP.getId()));
    }

    @Test
    void testBuyAndSellBasicAmmo() {
        state.setCoins(200);
        state.setItemCount(ShopItem.ROUND.getId(), 90);
        assertTrue(state.canBuy(ShopItem.ROUND));
        assertTrue(state.buy(ShopItem.ROUND));

        // 10 pack, price 5
        assertEquals(195, state.getCoins());
        assertEquals(100, state.getItemCount(ShopItem.ROUND.getId()));

        // Sell
        assertTrue(state.canSell(ShopItem.ROUND));
        assertTrue(state.sell(ShopItem.ROUND));
        assertEquals(200, state.getCoins());
        assertEquals(90, state.getItemCount(ShopItem.ROUND.getId()));
    }

    @Test
    void testResearchPrerequisites() {
        state.setCoins(5000);
        // Plasma requires shellResearch >= 1
        assertFalse(state.isItemUnlocked(ShopItem.PLASMA));
        assertFalse(state.canBuy(ShopItem.PLASMA));

        // Upgrade shell tech
        state.setStars(100);
        assertTrue(state.upgradeResearch(ResearchTech.SHELL_RESEARCH));
        assertEquals(1, state.getResearchLevel("shellResearch"));

        // Now unlocked
        assertTrue(state.isItemUnlocked(ShopItem.PLASMA));
        assertTrue(state.canBuy(ShopItem.PLASMA));
        assertTrue(state.buy(ShopItem.PLASMA));
        assertEquals(10, state.getItemCount(ShopItem.PLASMA.getId()));
    }

    @Test
    void testArmorDiscountAtResearchLevel3() {
        state.setCoins(1000);
        // Base price of ARMOR is 110
        assertEquals(110, state.getEffectivePrice(ShopItem.ARMOR));

        // Upgrade armorResearch to 3
        state.setResearchLevel("armorResearch", 3);
        // 30% discount -> 110 - (110 * 30 / 100) = 77
        assertEquals(77, state.getEffectivePrice(ShopItem.ARMOR));

        // Repair cost per HP: 6 -> 3
        assertEquals(30, state.getRepairTankCost(10));
    }

    @Test
    void testExChassisLimitExpansion() {
        // Without chassis: double round limit is 40
        assertEquals(40, state.getEffectiveLimit(ShopItem.DOUBLE_ROUND));

        // Buy chassis
        state.setItemCount(ShopItem.EX_CHASSIS.getId(), 1);
        assertTrue(state.hasExChassis());
        assertEquals(99, state.getEffectiveLimit(ShopItem.DOUBLE_ROUND));
        assertEquals(1.0f, state.getSpeedMultiplier(), 0.001f,
                "Builds cho khung gầm +2 HP, không tăng tốc độ nền");
    }

    @Test
    void installedForceFieldSurvivesInventorySerialization() {
        state.addInstalledForceField(3);
        PlayerShopState restored = new PlayerShopState();
        restored.loadState(0, 0, "{}", state.serializeInventory());
        assertEquals(3, restored.getInstalledForceField());
    }

    @Test
    void testExtraInterest() {
        assertEquals(0, state.getExtraInterestPercent());
        state.setResearchLevel("extraInterest", 1);
        assertEquals(5, state.getExtraInterestPercent());
        state.setResearchLevel("extraInterest", 2);
        assertEquals(10, state.getExtraInterestPercent());
    }

    @Test
    void shellResearchLevelsSixToEightReduceReloadLikeBuilds() {
        assertEquals(0.50, state.getShellReloadSeconds(), 0.0001);
        state.setResearchLevel("shellResearch", 6);
        assertEquals(0.50 - 2.0 / 60.0, state.getShellReloadSeconds(), 0.0001);
        state.setResearchLevel("shellResearch", 8);
        assertEquals(0.50 - 6.0 / 60.0, state.getShellReloadSeconds(), 0.0001);
    }

    @Test
    void testSerializationAndDeserialization() {
        state.setCoins(1500);
        state.setStars(25);
        state.setItemCount(ShopItem.PLASMA.getId(), 8);
        state.setItemCount(ShopItem.ARMOR.getId(), 2);
        state.setResearchLevel("speedResearch", 2);
        state.setResearchLevel("armorResearch", 1);

        String invJson = state.serializeInventory();
        String resJson = state.serializeResearch();

        PlayerShopState restored = new PlayerShopState();
        restored.loadState(1500, 25, resJson, invJson);

        assertEquals(1500, restored.getCoins());
        assertEquals(25, restored.getStars());
        assertEquals(8, restored.getItemCount(ShopItem.PLASMA.getId()));
        assertEquals(2, restored.getItemCount(ShopItem.ARMOR.getId()));
        assertEquals(2, restored.getResearchLevel("speedResearch"));
        assertEquals(1, restored.getResearchLevel("armorResearch"));
    }

    @Test
    void testWeaponCycling() {
        state.setActiveWeaponId(0);
        // No ammo for weapons 1..5 yet
        state.cycleNextWeapon();
        assertEquals(0, state.getActiveWeaponId());

        // Add ammo for plasma (1) and double round (2)
        state.setItemCount(1, 10);
        state.setItemCount(2, 5);

        state.cycleNextWeapon();
        assertEquals(1, state.getActiveWeaponId());

        state.cycleNextWeapon();
        assertEquals(2, state.getActiveWeaponId());

        state.cycleNextWeapon();
        assertEquals(0, state.getActiveWeaponId());

        // Direct select
        state.selectWeapon(2);
        assertEquals(2, state.getActiveWeaponId());

        // Cannot select weapon without ammo
        state.selectWeapon(3);
        assertEquals(2, state.getActiveWeaponId());
    }
}
