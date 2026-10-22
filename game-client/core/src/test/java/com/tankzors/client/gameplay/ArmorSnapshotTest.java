package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.model.TankSnapshot;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ShopItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmorSnapshotTest {
    @Test
    void armorExtendsHullAndRendersAfterBaseHpWithoutCountingChassis() {
        GameSimulation sim = new GameSimulation(42L);
        PlayerShopState shop = new PlayerShopState();
        shop.setItemCount(ShopItem.ARMOR.getId(), 2);
        shop.setItemCount(ShopItem.ARMOR2.getId(), 1);
        shop.setItemCount(ShopItem.EX_CHASSIS.getId(), 1);
        shop.setItemCount(ShopItem.ADD_LIFE.getId(), 1);
        sim.setShopState(shop);
        sim.startCampaignLevel(TestMaps.open(), "campaign_1", "player");
        PlayerTank tank = (PlayerTank) sim.getWorld().getTank("player");
        TankSnapshot full = snapshot(sim);
        assertEquals(10, full.hp);
        assertEquals(3, full.armorHp);
        assertFalse(full.isArmorSegment(6)); // Five base HP plus two chassis HP.
        assertTrue(full.isArmorSegment(7));
        assertTrue(full.isArmorSegment(9));
        assertFalse(full.isArmorSegment(10));

        tank.takeDamage(2);
        TankSnapshot damaged = snapshot(sim);
        assertEquals(8, damaged.hp); // Only one filled armor segment remains.
        assertEquals(10, damaged.maxHp);
        assertEquals(3, damaged.armorHp);
        tank.restoreHull();
        assertEquals(10, snapshot(sim).hp);

        sim.applyWorkshopPurchase(ShopItem.ARMOR);
        assertEquals(4, snapshot(sim).armorHp);
        assertEquals(11, snapshot(sim).maxHp);
        sim.applyWorkshopSale(ShopItem.ARMOR);
        assertEquals(3, snapshot(sim).armorHp);
        assertEquals(10, snapshot(sim).maxHp);

        tank.takeDamage(tank.getMaxHP());
        sim.step(GameplayConfig.STEP_SECONDS);
        TankSnapshot respawned = snapshot(sim);
        assertEquals(9, respawned.maxHp);
        assertEquals(9, respawned.hp);
        assertEquals(2, respawned.armorHp); // Lost one temporary plate; permanent stays.
    }

    @Test
    void unarmoredTankHasNoArmorSegments() {
        TankSnapshot tank = new TankSnapshot();
        tank.hp = tank.maxHp = 5;
        for (int i = 0; i < 5; i++) assertFalse(tank.isArmorSegment(i));
    }

    private static TankSnapshot snapshot(GameSimulation sim) {
        return WorldSnapshotMapper.snapshot(sim.getWorld(), "player", 0, 0).tanks.stream()
                .filter(t -> "player".equals(t.id)).findFirst().orElseThrow();
    }
}
