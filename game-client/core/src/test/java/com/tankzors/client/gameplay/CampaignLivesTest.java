package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ShopItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampaignLivesTest {
    private MapDefinition map(boolean captureFlag) {
        MapDefinition map = TestMaps.open();
        map.getGrid()[1][1] = 3;
        map.getGrid()[12][17] = 4;
        if (captureFlag) map.getGrid()[5][5] = 9;
        return map;
    }

    private void die(GameSimulation sim) {
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
        player.takeDamage(player.getMaxHP() + player.getShieldHp());
        sim.step(GameplayConfig.STEP_SECONDS);
    }

    @Test void allThreeCampaignMapsHaveFourLivesAndPreserveInventoryOnRespawn() {
        for (int level = 1; level <= 3; level++) {
            GameSimulation sim = new GameSimulation(45L);
            PlayerShopState shop = new PlayerShopState();
            shop.setItemCount(ShopItem.ROUND.getId(), 73);
            shop.setItemCount(ShopItem.REPAIR.getId(), 2);
            sim.setShopState(shop);
            int[] terminal = {0};
            sim.setMatchEndHandler((id, status, hp, kills, time) -> terminal[0]++);
            sim.startCampaignLevel(map(level == 1), "campaign_" + level, "player");
            assertEquals(4, sim.getPlayerLivesRemaining());
            for (int remaining = 3; remaining > 0; remaining--) {
                PlayerTank previous = (PlayerTank) sim.getWorld().getTank("player");
                previous.getPosition().setX(180);
                die(sim);
                PlayerTank current = (PlayerTank) sim.getWorld().getTank("player");
                assertNotSame(previous, current);
                assertEquals(GameWorld.Status.PLAYING, sim.getWorld().getStatus());
                assertEquals(remaining, sim.getPlayerLivesRemaining());
                assertEquals(current.getMaxHP(), current.getHP());
                assertEquals(60, current.getPosition().getX(), .001);
                assertEquals(60, current.getPosition().getY(), .001);
                assertEquals(73, shop.getItemCount(ShopItem.ROUND.getId()));
                assertEquals(2, shop.getItemCount(ShopItem.REPAIR.getId()));
                assertEquals(0, terminal[0]);
            }
            die(sim);
            assertEquals(0, sim.getPlayerLivesRemaining());
            assertEquals(GameWorld.Status.DEFEAT, sim.getWorld().getStatus());
            assertEquals(1, terminal[0]);
            sim.step(GameplayConfig.STEP_SECONDS);
            assertEquals(1, terminal[0]);
        }
    }

    @Test void retryAndNextMapRestoreFourBaseLives() {
        GameSimulation sim = new GameSimulation(3L);
        sim.startCampaignLevel(map(false), "campaign_2", "player");
        die(sim);
        assertEquals(3, sim.getPlayerLivesRemaining());
        sim.restart();
        assertEquals(4, sim.getPlayerLivesRemaining());
        die(sim);
        sim.startCampaignLevel(map(false), "campaign_3", "player");
        assertEquals(4, sim.getPlayerLivesRemaining());
    }

    @Test void purchasedExtraLifeIsUsedOnlyAfterBaseLivesRunOut() {
        GameSimulation sim = new GameSimulation(3L);
        PlayerShopState shop = new PlayerShopState();
        shop.setItemCount(ShopItem.ADD_LIFE.getId(), 1);
        sim.setShopState(shop);
        sim.startCampaignLevel(map(false), "campaign_2", "player");
        assertEquals(5, sim.getPlayerLivesRemaining());
        for (int i = 0; i < 3; i++) die(sim);
        assertEquals(1, shop.getItemCount(ShopItem.ADD_LIFE.getId()));
        die(sim);
        assertEquals(0, shop.getItemCount(ShopItem.ADD_LIFE.getId()));
        assertEquals(1, sim.getPlayerLivesRemaining());
        assertEquals(GameWorld.Status.PLAYING, sim.getWorld().getStatus());
        die(sim);
        assertEquals(GameWorld.Status.DEFEAT, sim.getWorld().getStatus());
    }
}
