package com.tankzors.client.gameplay.entity;

import com.tankzors.client.gameplay.TestMaps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTankMovementTest {

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
