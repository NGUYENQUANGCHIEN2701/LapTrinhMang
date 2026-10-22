package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.*;
import com.tankzors.client.gameplay.map.*;
import com.tankzors.client.shop.PlayerShopState;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AmmoObstacleParityTest {
    @Test void damagedBrickStatesLoadTheirOriginalDurability() {
        for (int id = 1; id <= 8; id++) {
            MapDefinition map = MapLoader.parse("{\"grid\":[[0,0,0],[0,1,0],[0,0,0]],"
                    + "\"visualGrid\":[[0,0,0],[0," + (129 + id) + ",0],[0,0,0]]}");
            assertNotNull(map);
            assertEquals(4 - (id - 1) / 2, map.wallAt(1, 1).getHitPoints());
        }
    }

    @Test void excessProjectileDamageContinuesPastDestroyedObstacles() {
        for (int tile : new int[]{1, 6}) {
            MapDefinition map = new MapDefinition("obstacle", 5, 3, 40,
                    new int[][]{{0,0,0,0,0},{0,0,tile,0,0},{0,0,0,0,0}});
            GameSimulation sim = new GameSimulation(12L);
            sim.getWorld().setCurrentMap(map);
            sim.getWorld().setStatus(GameWorld.Status.PLAYING);
            sim.getWorld().addTank(new PlayerTank("player", 20, 20));
            sim.getWorld().addTank(new AITank("enemy", 180, 20,
                    com.tankzors.client.gameplay.ai.DifficultyLevel.EASY));
            Bullet bullet = new Bullet("ap", "player", new Position(100, 60), 90, 1, 5, 8, 4, 5);
            sim.getWorld().addBullet(bullet);
            sim.step(GameplayConfig.STEP_SECONDS);
            assertEquals(TileType.GROUND, map.getTileAt(2, 1));
            assertTrue(bullet.isActive());
            assertEquals(tile == 1 ? 1 : 4, bullet.getDamage());
        }
    }

    @Test void guidedMissileBreaksConcreteButLeavesSteel() {
        for (int legacy : new int[]{9,10,12,13}) {
            int[][] grid = new int[20][20];
            grid[5][3] = 3;
            grid[5][5] = 5;
            MapDefinition map = new MapDefinition("concrete",20,20,40,grid);
            int[][] visual = new int[20][20];
            visual[5][5] = 129 + legacy;
            map.setVisualGrid(visual);
            map.setEnemies(List.of());
            PlayerShopState shop = new PlayerShopState();
            shop.setItemCount(8, 2);
            shop.selectWeapon(8);
            GameSimulation sim = new GameSimulation(22L);
            sim.setShopState(shop);
            sim.startCampaignLevel(map,"campaign_1","player");
            ((PlayerTank) sim.getWorld().getTank("player")).rotate(90);
            sim.getWorld().addTank(AITank.turret("sentinel",700,700,Direction.DOWN,1,10000));
            sim.enqueueShoot();
            for (int i = 0; i < 180; i++) sim.step(1.0 / 60);
            assertEquals(legacy <= 10 ? TileType.GROUND : TileType.WHITE_WALL, map.getTileAt(5,5));
        }
    }

    @Test void plasmaReloadsTenTicksEarlierThanShell() {
        PlayerTank tank = new PlayerTank("player",100,100);
        tank.setActiveWeaponId(1);
        assertNotNull(tank.shoot());
        tank.updateCooldown(.75 - 10.0 / 60 - .001);
        assertNull(tank.shoot());
        tank.updateCooldown(.002);
        assertNotNull(tank.shoot());
    }
}
