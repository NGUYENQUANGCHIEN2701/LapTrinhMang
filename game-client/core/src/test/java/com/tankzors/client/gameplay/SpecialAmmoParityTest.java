package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.*;
import com.tankzors.client.gameplay.map.*;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.shop.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpecialAmmoParityTest {
    private static final double DT = 1.0 / 30;
    private final PlayerShopState shop = new PlayerShopState();
    private final GameSimulation sim = new GameSimulation(430L);
    private MapDefinition map;
    private PlayerTank player;

    private void start(int item) {
        int[][] grid = new int[30][30];
        grid[5][3] = TileType.SPAWN_PLAYER.getValue();
        map = new MapDefinition("special", 30, 30, 40, grid);
        map.setEnemies(List.of());
        shop.setItemCount(item, 4);
        shop.selectWeapon(item);
        sim.setShopState(shop);
        sim.startCampaignLevel(map, "campaign_1", "player");
        player = (PlayerTank) sim.getWorld().getTank("player");
        player.rotate(90);
        target("sentinel", 27, 27, 10000);
    }
    private AITank target(String id, int col, int row, int hp) {
        AITank tank = AITank.turret(id, (col + .5) * 40, (row + .5) * 40, Direction.DOWN, 1, hp);
        sim.getWorld().addTank(tank);
        return tank;
    }
    private void run(double seconds) { for (int i = 0; i < Math.ceil(seconds / DT); i++) sim.step(DT); }
    private void fire() { sim.enqueueShoot(); sim.step(DT); }
    private GameStateSnapshot snapshot() {
        GameStateSnapshot s = WorldSnapshotMapper.snapshot(sim.getWorld(), "player", 0, 0);
        sim.appendEffectsTo(s);
        return s;
    }

    @Test void artilleryUsesAllResearchTiersAndLongerReload() {
        int[] powers = {9, 11, 11, 14, 17};
        for (int level = 1; level <= 5; level++) {
            SpecialAmmoParityTest f = new SpecialAmmoParityTest();
            f.shop.setResearchLevel("artilleryResearch", level);
            f.start(6);
            f.fire();
            Bullet shell = (Bullet) f.sim.getWorld().getAllBullets().get(0);
            assertEquals(powers[level - 1], shell.getDamage());
            assertEquals(level >= 3 ? 200 : 160, shell.getMaxRange());
            assertEquals(level >= 4 ? 120 : 84, shell.getSpeed(), .001);
            assertEquals(level >= 3 ? 5 : 4, f.snapshot().artilleryRange);
            f.run(.5);
            f.fire();
            assertEquals(3, f.shop.getItemCount(6));
            f.run(.65);
            f.fire();
            assertEquals(2, f.shop.getItemCount(6));
        }
    }

    @Test void artilleryExplosionUsesDiamondAndRadiusMinusOne() {
        shop.setResearchLevel("artilleryResearch", 1);
        start(6);
        AITank center = target("center", 7, 5, 100);
        AITank diagonal = target("diagonal", 8, 6, 100);
        AITank outside = target("outside", 10, 5, 100);
        map.setTileAt(7, 4, TileType.STONE_WALL);
        map.setGrid(map.getGrid());
        fire();
        run(2.6);
        assertEquals(82, center.getHP());
        assertEquals(86, diagonal.getHP());
        assertEquals(100, outside.getHP());
        assertEquals(TileType.GROUND, map.getTileAt(7, 4));
    }

    @Test void minesArePlacedOnceIgnoreAlliesAndDealFiveOrNineHp() {
        for (int id : new int[]{10, 11}) {
            SpecialAmmoParityTest f = new SpecialAmmoParityTest();
            f.start(id);
            f.fire();
            f.fire(); // The occupied cell must not consume another mine.
            assertEquals(3, f.shop.getItemCount(id));
            assertEquals(5, f.player.getHP());
            assertEquals(1, f.snapshot().effects.stream().filter(e -> e.weaponType == id).count());
            AITank ally = f.target("ally", 3, 5, 30);
            ally.setTeam(AITank.Team.ALLY);
            f.sim.step(DT);
            assertEquals(30, ally.getHP());
            AITank enemy = f.target("enemy", 3, 5, 30);
            f.sim.step(DT);
            assertEquals(id == 10 ? 25 : 21, enemy.getHP());
            assertTrue(f.snapshot().effects.stream().noneMatch(e -> e.weaponType == id));
        }
    }

    @Test void warheadAcceleratesAndExplodesWhenHittingTank() {
        start(7);
        AITank enemy = target("enemy", 5, 5, 100);
        fire();
        Bullet shot = (Bullet) sim.getWorld().getAllBullets().get(0);
        double initialSpeed = shot.getSpeed();
        run(.5);
        assertTrue(shot.getSpeed() > initialSpeed);
        run(1.2);
        assertFalse(shot.isActive());
        assertTrue(enemy.getHP() < 100);
        assertEquals(5, player.getHP());
    }

    @Test void lastGuidedMissileCanBeDetonatedWithoutFiringFallbackShell() {
        start(8);
        shop.setItemCount(8, 1);
        fire();
        assertEquals(0, shop.getItemCount(8));
        int shells = shop.getItemCount(0);
        fire();
        assertTrue(sim.getWorld().getAllBullets().stream().noneMatch(b -> b.isActive()));
        assertEquals(shells, shop.getItemCount(0));
    }

    @Test void guidedMissileControlsRocketInsteadOfMovingPlayer() {
        start(8);
        fire();
        double x = player.getPosition().getX(), y = player.getPosition().getY();
        sim.enqueueMove(Direction.DOWN, true);
        run(.3);
        assertEquals(x, player.getPosition().getX());
        assertEquals(y, player.getPosition().getY());
        Bullet shot = (Bullet) sim.getWorld().getAllBullets().get(0);
        assertEquals(180, shot.getDirection());
    }

    @Test void dynamiteTiersUseThreeSecondFuseAndCorrectPower() {
        for (int id : new int[]{12, 13, 14}) {
            SpecialAmmoParityTest f = new SpecialAmmoParityTest();
            f.start(id);
            AITank enemy = f.target("enemy", 3, 5, 100);
            f.fire();
            f.player.getPosition().setX(500);
            f.run(2.8);
            assertEquals(100, enemy.getHP());
            f.run(.5);
            assertEquals(100 - 2 * (id == 12 ? 7 : id == 13 ? 10 : 19), enemy.getHP());
        }
    }

    @Test void ownedAirStrikeAimsWithoutSpendingAndCanCancel() {
        start(9);
        // Builds checks research/radar when buying; captured/owned ammunition still works.
        fire();
        assertTrue(sim.isAirAimActive());
        assertEquals(4, shop.getItemCount(9));
        sim.enqueueMove(Direction.RIGHT, true);
        run(.4);
        assertTrue(snapshot().airAimCol > 3);
        assertEquals(140, player.getPosition().getX());
        sim.cancelAirAim();
        assertEquals(4, shop.getItemCount(9));
    }

    @Test void airStrikeConsumesOnConfirmationAndDropsFiveOrSevenBombs() {
        for (int level = 1; level <= 3; level++) {
            SpecialAmmoParityTest f = new SpecialAmmoParityTest();
            f.shop.setResearchLevel("airRaidResearch", level);
            f.shop.setItemCount(23, 1);
            f.start(9);
            // The player must leave the strike zone before confirming fire.
            f.player.getPosition().setX(600);
            f.player.getPosition().setY(600);
            f.fire();
            f.player.getPosition().setX(200);
            f.player.getPosition().setY(200);
            int[] bombs = {0};
            f.sim.setEventListener(new GameEventListener() {
                public void onShoot(boolean player) {}
                public void onHitWall() {}
                public void onHitTank(boolean player) {}
                public void onTankDestroyed(boolean player) {}
                public void onMatchEnd(GameWorld.Status status) {}
                public void onSpecialExplosion(int type, int radius) { bombs[0]++; }
            });
            f.fire();
            assertFalse(f.sim.isAirAimActive());
            assertEquals(3, f.shop.getItemCount(9));
            assertTrue(f.snapshot().effects.stream().anyMatch(e -> e.weaponType == 9));
            f.run(12);
            assertEquals(level == 3 ? 7 : 5, bombs[0]);
        }
    }

    @Test void explosionsUseShieldFirstThenHalfMobileOverflow() {
        PlayerTank tank = new PlayerTank("p", 0, 0, 60, 20, 20);
        tank.takeExplosionDamage(7, false);
        assertEquals(13, tank.getHP());
        AITank turret = AITank.turret("t", 0, 0, Direction.DOWN, 1, 20);
        turret.takeExplosionDamage(7, true);
        assertEquals(6, turret.getHP());
    }
}
