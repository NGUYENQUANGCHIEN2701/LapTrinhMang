package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.*;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.shop.PlayerShopState;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SupportTankInventoryTest {
    @Test void workshopRepairsOnlyLivingAlliesAndUsesBuildsDiscount() {
        GameSimulation sim = new GameSimulation(12L);
        PlayerShopState shop = new PlayerShopState();
        shop.setCoins(100);
        sim.setShopState(shop);
        AITank ally = AITank.support("ally",100,100,25);
        AITank enemy = AITank.builds("enemy",300,300,0,1);
        ally.takeDamage(2);
        enemy.takeDamage(1);
        sim.getWorld().addTank(ally);
        sim.getWorld().addTank(enemy);
        assertEquals(2,sim.getMissingAlliedHp());
        assertTrue(sim.repairAlliesAtWorkshop());
        assertEquals(90,shop.getCoins());
        assertEquals(8,ally.getHP());
        assertEquals(2,enemy.getHP());
        assertFalse(sim.repairAlliesAtWorkshop());
        shop.setResearchLevel("armorResearch",3);
        ally.takeDamage(2);
        assertTrue(sim.repairAlliesAtWorkshop());
        assertEquals(86,shop.getCoins());
        shop.setCoins(0);
        ally.takeDamage(1);
        assertFalse(sim.repairAlliesAtWorkshop());
        assertEquals(7,ally.getHP());
    }

    @Test void eachSupportTierSpawnsAtBaseAndConsumesExactlyOneItem() {
        for (int item = 25; item <= 27; item++) {
            GameSimulation sim = new GameSimulation(8L);
            PlayerShopState shop = new PlayerShopState();
            shop.setItemCount(item, 2);
            sim.setShopState(shop);
            int[][] grid = new int[20][20];
            grid[2][2] = 3;
            MapDefinition map = new MapDefinition("support",20,20,40,grid);
            map.setEnemies(List.of());
            sim.startCampaignLevel(map,"campaign_1","player");
            PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
            player.getPosition().setX(340);
            player.getPosition().setY(340);
            assertTrue(sim.useInventoryItem(item));
            AITank ally = (AITank) sim.getWorld().getAllTanks().stream()
                    .filter(t -> t instanceof AITank && ((AITank) t).isAlly()).findFirst().orElseThrow();
            assertTrue(ally.isFollowPlayer());
            assertEquals(item == 27 ? 25 : item == 26 ? 15 : 8, ally.getHP());
            assertEquals(100, ally.getPosition().getX());
            assertEquals(100, ally.getPosition().getY());
            assertEquals(4, ally.getBuildsSpeed());
            assertEquals(item == 27 ? 4 : 0, ally.getBuildsType());
            assertEquals(1, shop.getItemCount(item));
            assertFalse(sim.useInventoryItem(item), "An occupied base must not spawn a second tank");
            assertEquals(1, shop.getItemCount(item));
            assertTrue(sim.getHintText().contains("Căn cứ"));
        }
    }
}
