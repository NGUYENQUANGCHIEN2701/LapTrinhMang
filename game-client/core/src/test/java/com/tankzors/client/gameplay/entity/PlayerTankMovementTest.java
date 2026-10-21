package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.map.MapDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTankMovementTest {

    @Test
    void buffersTurnUntilTileCenterThenMovesWithoutDelay() {
        PlayerTank player = new PlayerTank("player", 100, 100);
        MapDefinition map = TestMaps.open();
        player.setMoving(Direction.UP, true);
        player.updateMovement(0.1, map);
        player.setMoving(Direction.RIGHT, true);
        player.updateMovement(0.1, map);

        assertEquals(100, player.getPosition().getX(), 1e-9);
        assertEquals(88, player.getPosition().getY(), 1e-9);
        assertEquals(0, player.getRotation(), 1e-9);

        // Còn 28px tới tâm ô; 0.5s đi được 30px, dư 2px cho bước sang phải.
        player.updateMovement(0.5, map);
        assertEquals(102, player.getPosition().getX(), 1e-9);
        assertEquals(60, player.getPosition().getY(), 1e-9);
        assertEquals(90, player.getRotation(), 1e-9);
    }

    @Test
    void releaseCompletesCurrentTileWithoutStartingAnother() {
        PlayerTank player = new PlayerTank("player", 100, 100);
        MapDefinition map = TestMaps.open();
        player.setMoving(Direction.UP, true);
        player.updateMovement(0.1, map);
        player.stopMoving();
        player.updateMovement(1.0, map);

        assertEquals(60, player.getPosition().getY(), 1e-9);
        player.updateMovement(1.0, map);
        assertEquals(60, player.getPosition().getY(), 1e-9);
    }

    @Test
    void releasingReverseCompletesOneTileWithoutTurning() {
        PlayerTank player = new PlayerTank("player", 100, 100);
        MapDefinition map = TestMaps.open();
        player.setReversing(true);
        player.updateMovement(0.1, map);
        player.setReversing(false);
        player.updateMovement(1.0, map);

        assertEquals(140, player.getPosition().getY(), 1e-9);
        assertEquals(0, player.getRotation(), 1e-9);
    }

    @Test
    void blockedTileKeepsTankAtCurrentTileCenter() {
        PlayerTank player = new PlayerTank("player", 100, 100, 600);
        MapDefinition map = TestMaps.parse(
                "########", "#..#...#", "#..#...#", "#..#...#", "########");
        player.setMoving(Direction.RIGHT, true);
        player.updateMovement(0.5, map);

        assertEquals(100, player.getPosition().getX(), 1e-9);
        assertEquals(100, player.getPosition().getY(), 1e-9);
    }

    @Test
    void tileOccupiedByAnotherTankBlocksMovement() {
        PlayerTank player = new PlayerTank("player", 100, 100, 600);
        MapDefinition map = TestMaps.open();
        player.setMoving(Direction.RIGHT, true);
        player.updateMovement(0.5, map, (mover, col, row) -> col == 3 && row == 2);

        assertEquals(100, player.getPosition().getX(), 1e-9, "Không được đi xuyên xe khác");
        assertEquals(100, player.getPosition().getY(), 1e-9);
    }

    @Test
    void movingPlayerReservesBothCurrentAndTargetTiles() {
        PlayerTank player = new PlayerTank("player", 100, 100);
        MapDefinition map = TestMaps.open();
        assertTrue(player.occupies(2, 2, 40));
        assertFalse(player.occupies(3, 2, 40));

        player.setMoving(Direction.RIGHT, true);
        player.updateMovement(0.05, map);
        assertTrue(player.occupies(2, 2, 40), "Ô đang rời vẫn bị chiếm");
        assertTrue(player.occupies(3, 2, 40), "Ô đích đã được giữ chỗ ngay khi bắt đầu đi");
        assertFalse(player.occupies(4, 2, 40));
    }

    @Test
    void quickTapBetweenTicksStillMovesExactlyOneTile() {
        PlayerTank player = new PlayerTank("player", 100, 100);
        MapDefinition map = TestMaps.open();
        player.setMoving(Direction.RIGHT, true);
        player.setMoving(Direction.RIGHT, false);
        player.stopMoving();
        player.updateMovement(0.1, map);
        assertEquals(106, player.getPosition().getX(), 1e-9, "Movement must be gradual, not teleportation");

        player.updateMovement(2.0, map);
        assertEquals(140, player.getPosition().getX(), 1e-9);
    }

    @Test
    void tileStepUsesActualMapTileSize() {
        PlayerTank player = new PlayerTank("player", 80, 80);
        MapDefinition map = new MapDefinition("32px", 10, 10, 32, new int[10][10]);
        player.setMoving(Direction.RIGHT, true);
        player.setMoving(Direction.RIGHT, false);
        player.updateMovement(2.0, map);

        assertEquals(112, player.getPosition().getX(), 1e-9);
        assertEquals(80, player.getPosition().getY(), 1e-9);
    }

    @Test
    void reverseMovesOppositeFacingWithoutTurningTank() {
        PlayerTank player = new PlayerTank("player", 100, 100);
        player.rotate(90);

        player.setReversing(true);
        player.updateMovement(0.1, TestMaps.open());

        assertTrue(player.getPosition().getX() < 100, "Facing right means reverse must move left");
        assertEquals(100, player.getPosition().getY(), 1e-9);
        assertEquals(90, player.getRotation(), 1e-9, "Reverse must not rotate the player sprite");
    }
}
