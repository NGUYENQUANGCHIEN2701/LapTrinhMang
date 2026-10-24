package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.map.MapDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkshopBaseAccessTest {
    private GameSimulation start(int tileSize) {
        MapDefinition map = TestMaps.parse(
                "########",
                "#o....x#",
                "#......#",
                "########");
        map.setTileSize(tileSize);
        GameSimulation sim = new GameSimulation(18L);
        sim.startCampaignLevel(map, "campaign_3", "player");
        return sim;
    }

    @Test
    void onlyGreenSpawnTileAllowsAccessAndReturningRestoresIt() {
        for (int tileSize : new int[] {40, 64}) {
            GameSimulation sim = start(tileSize);
            PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
            assertTrue(sim.isLocalPlayerAtSpawnBase());

            player.getPosition().setX(2.5 * tileSize);
            assertFalse(sim.isLocalPlayerAtSpawnBase(), "Ô nền cạnh căn cứ không phải ô xanh");
            player.getPosition().setX(6.5 * tileSize);
            assertFalse(sim.isLocalPlayerAtSpawnBase(), "Căn cứ đỏ không được mở cửa hàng");
            player.getPosition().setX(1.5 * tileSize);
            player.getPosition().setY(2.5 * tileSize);
            assertFalse(sim.isLocalPlayerAtSpawnBase());
            player.getPosition().setY(1.5 * tileSize);
            assertTrue(sim.isLocalPlayerAtSpawnBase());
        }
    }

    @Test
    void accessUsesTileContainingTankCenterNotNearbyGlowOrTankOverlap() {
        GameSimulation sim = start(40);
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
        player.getPosition().setX(79.99);
        assertTrue(sim.isLocalPlayerAtSpawnBase());
        player.getPosition().setX(80);
        assertFalse(sim.isLocalPlayerAtSpawnBase());
        player.getPosition().setX(39.99);
        assertFalse(sim.isLocalPlayerAtSpawnBase());
        player.getPosition().setX(-1);
        assertFalse(sim.isLocalPlayerAtSpawnBase());
    }

    @Test
    void deadMissingAndTerminalPlayersCannotUseInBattleShop() {
        assertFalse(new GameSimulation(18L).isLocalPlayerAtSpawnBase());
        GameSimulation sim = start(40);
        sim.getWorld().setStatus(GameWorld.Status.DEFEAT);
        assertFalse(sim.isLocalPlayerAtSpawnBase());
        sim.getWorld().setStatus(GameWorld.Status.VICTORY);
        assertFalse(sim.isLocalPlayerAtSpawnBase());
        sim.getWorld().setStatus(GameWorld.Status.PLAYING);
        PlayerTank player = (PlayerTank) sim.getWorld().getTank("player");
        player.takeDamage(player.getMaxHP() + player.getShieldHp());
        assertFalse(sim.isLocalPlayerAtSpawnBase());
        sim.getWorld().removeTank("player");
        assertFalse(sim.isLocalPlayerAtSpawnBase());
    }

    @Test
    void fallbackSpawnWithoutGreenTileDoesNotBecomeWorkshop() {
        GameSimulation sim = new GameSimulation(18L);
        sim.startCampaignLevel(TestMaps.open(), "campaign_2", "player");
        assertFalse(sim.isLocalPlayerAtSpawnBase());
    }
}
