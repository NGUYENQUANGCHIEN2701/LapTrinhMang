package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.model.TankSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Nội suy render giữa hai fixed step: chỉ đổi toạ độ snapshot, không đổi world. */
class WorldSnapshotInterpolationTest {

    private static TankSnapshot tank(GameStateSnapshot snap, String id) {
        for (TankSnapshot t : snap.tanks) if (id.equals(t.id)) return t;
        throw new AssertionError("missing tank " + id);
    }

    @Test
    @DisplayName("Snapshot nội suy vị trí tank giữa đầu step và vị trí hiện tại")
    void interpolatesTankBetweenSteps() {
        GameSimulation sim = new GameSimulation(1L);
        GameWorld world = sim.getWorld();
        world.setCurrentMap(TestMaps.open());
        world.setStatus(GameWorld.Status.PLAYING);
        PlayerTank player = new PlayerTank("p", 200, 200);
        world.addTank(player);
        player.setMoving(Direction.UP, true);

        sim.step(GameplayConfig.STEP_SECONDS);
        double moved = 200 - player.getPosition().getY();
        assertTrue(moved > 0, "tank phải di chuyển trong step");

        assertEquals(200f, tank(WorldSnapshotMapper.snapshot(world, "p", 0, 0, 0f), "p").y, 1e-3);
        assertEquals(200f - moved / 2, tank(WorldSnapshotMapper.snapshot(world, "p", 0, 0, 0.5f), "p").y, 1e-3);
        assertEquals((float) player.getPosition().getY(),
                tank(WorldSnapshotMapper.snapshot(world, "p", 0, 0), "p").y, 1e-3);
        assertEquals(200 - moved, player.getPosition().getY(), 1e-9, "snapshot không được sửa world");
    }

    @Test
    @DisplayName("Dịch chuyển xa (respawn) hiển thị thẳng vị trí mới, không trượt qua map")
    void skipsInterpolationForTeleport() {
        assertEquals(500f, WorldSnapshotMapper.interpolate(100, 100, 500, 100, 0.5f, true));
        assertEquals(110f, WorldSnapshotMapper.interpolate(100, 100, 120, 100, 0.5f, true));
    }

    @Test
    @DisplayName("Alpha phản ánh phần dư accumulator của fixed-step driver")
    void interpolationAlphaTracksAccumulator() {
        GameSimulation sim = new GameSimulation(1L);
        sim.getWorld().setCurrentMap(TestMaps.open());
        sim.advance((float) (GameplayConfig.STEP_SECONDS * 1.5));
        assertEquals(0.5f, sim.getInterpolationAlpha(), 1e-3);
    }
}
