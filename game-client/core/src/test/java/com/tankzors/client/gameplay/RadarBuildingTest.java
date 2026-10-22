package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.*;
import com.tankzors.client.gameplay.map.*;
import com.tankzors.client.model.GameStateSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadarBuildingTest {
    @Test void realBuildingDataBlocksCellsTracksDamageAndDisappearsFromRadarWhenDestroyed() {
        MissionConfig mission = new MissionConfig();
        mission.getBuildings().add(new MissionConfig.BuildingConfig(8, 5, "radar", 2, 4));
        mission.getBuildings().add(new MissionConfig.BuildingConfig(4, 8, "reactor", -3, 3));
        MapDefinition map = TestMaps.open();
        map.setMission(mission);
        GameSimulation sim = new GameSimulation(90);
        sim.startCampaignLevel(map, "campaign_1", "player");
        sim.getWorld().addTank(AITank.turret("sentinel", 700, 500, Direction.DOWN, 1, 100));
        assertFalse(map.isWalkable(8, 5));
        assertFalse(map.isWalkable(9, 6));
        assertTrue(map.isWalkable(10, 6));
        GameStateSnapshot frame = WorldSnapshotMapper.snapshot(sim.getWorld(), "player", 0, 0);
        assertEquals(2, frame.buildings.size());
        assertEquals("radar", frame.buildings.get(0).type);
        assertEquals(4, frame.buildings.get(0).hp);
        sim.getWorld().addBullet(new Bullet("test", "player", new Position(340, 220), 90, 0, 4, 8, 4, 0));
        sim.step(1.0 / 30);
        assertTrue(map.isWalkable(8, 5));
        frame = WorldSnapshotMapper.snapshot(sim.getWorld(), "player", 0, 0);
        assertEquals(1, frame.buildings.size());
        assertEquals("reactor", frame.buildings.get(0).type);
        assertEquals(-3, frame.buildings.get(0).team);
    }
    @Test void parserPreservesBuildingKindTeamAndHitPoints() {
        MapDefinition map = MapLoader.parse("{\"width\":4,\"height\":4,\"grid\":[[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]],\"mission\":{\"buildings\":[{\"type\":\"reactor\",\"x\":1,\"y\":1,\"team\":3,\"hitPoints\":8}]}}");
        assertNotNull(map);
        assertEquals(1, map.getMission().getBuildings().size());
        assertEquals(8, map.getMission().getBuildings().get(0).hitPoints);
        assertEquals(3, map.getMission().getBuildings().get(0).team);
    }
}
