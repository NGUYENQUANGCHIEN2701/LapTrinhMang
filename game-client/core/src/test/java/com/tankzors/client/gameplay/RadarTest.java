package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.*;
import com.tankzors.client.gameplay.map.*;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.shop.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadarTest {
    @Test void blinkFollowsSixtyHzClockAndFiftyOneTickPeriod() {
        RadarState radar = new RadarState();
        radar.open();
        assertFalse(radar.showsObjects());
        radar.update(1.0 / 60);
        assertTrue(radar.showsObjects());
        radar.update(50.0 / 60);
        assertTrue(radar.showsObjects());
        radar.update(1.0 / 60);
        assertFalse(radar.showsObjects());
        radar.togglePause();
        radar.update(10);
        assertFalse(radar.showsObjects());
        radar.togglePause();
        radar.update(51.0 / 60);
        assertTrue(radar.showsObjects());
        radar.close();
        radar.update(10);
        assertTrue(radar.showsObjects());
        radar.reset();
        assertFalse(radar.isOpen());
        assertFalse(radar.showsObjects());
    }

    @Test void ownedRadarDoesNotOpenUntilUsedAndNeverGetsConsumedOrSold() {
        PlayerShopState shop = new PlayerShopState();
        shop.setCoins(640);
        assertTrue(shop.buy(ShopItem.RADAR));
        assertEquals(320, shop.getCoins());
        assertFalse(shop.buy(ShopItem.RADAR));
        assertFalse(shop.canSell(ShopItem.RADAR));
        assertFalse(shop.sell(ShopItem.RADAR));
        GameSimulation sim = start(shop);
        assertFalse(sim.isRadarActive());
        sim.applyWorkshopPurchase(ShopItem.RADAR);
        assertFalse(sim.isRadarActive());
        for (int i = 0; i < 3; i++) {
            assertTrue(sim.useInventoryItem(23));
            assertTrue(sim.isRadarActive());
            sim.closeRadar();
        }
        assertEquals(1, shop.getItemCount(23));
        shop.setItemCount(23, 0);
        assertFalse(sim.useInventoryItem(23));
    }

    @Test void radarFreezesTheWholeWorldAndClosingNeverAccumulatesBacklog() {
        PlayerShopState shop = new PlayerShopState();
        shop.setItemCount(23, 1);
        GameSimulation sim = start(shop);
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
        sim.useInventoryItem(17);
        player.addShieldCapacity(1);
        sim.getWorld().addBullet(new Bullet("b", "player", new Position(200, 100), 90, 200, 1, 4, 8, 0));
        sim.step(1.0 / 30);
        assertTrue(sim.useInventoryItem(23));
        double time = sim.getElapsedSeconds(), nitro = sim.getSpeedBoostRemaining();
        long tick = sim.getWorld().getTickCount();
        double bulletX = sim.getWorld().getAllBullets().get(0).getPosition().getX();
        sim.enqueueMove(Direction.RIGHT, true);
        sim.enqueueShoot();
        sim.advance(120);
        sim.step(120);
        assertEquals(time, sim.getElapsedSeconds());
        assertEquals(nitro, sim.getSpeedBoostRemaining());
        assertEquals(tick, sim.getWorld().getTickCount());
        assertEquals(0, player.getShieldHp());
        assertEquals(bulletX, sim.getWorld().getAllBullets().get(0).getPosition().getX());
        GameStateSnapshot frame = new GameStateSnapshot();
        sim.appendEffectsTo(frame);
        assertTrue(frame.radarActive);
        assertTrue(frame.hasRadar);
        double x = player.getPosition().getX();
        sim.closeRadar();
        sim.step(1.0 / 30);
        assertEquals(time + 1.0 / 30, sim.getElapsedSeconds(), 1e-6);
        assertEquals(x, player.getPosition().getX());
        assertEquals(1, sim.getWorld().getBulletCount());
    }

    private GameSimulation start(PlayerShopState shop) {
        GameSimulation sim = new GameSimulation(93);
        sim.setShopState(shop);
        sim.startCampaignLevel(TestMaps.open(), "campaign_1", "player");
        sim.getWorld().addTank(AITank.turret("sentinel", 700, 500, Direction.DOWN, 1, 1000));
        return sim;
    }
}
