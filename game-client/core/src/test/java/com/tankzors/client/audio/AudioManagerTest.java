package com.tankzors.client.audio;

import com.tankzors.client.gameplay.GameEventListener;
import com.tankzors.client.gameplay.GameSimulation;
import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.TestMaps;
import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.Bullet;
import com.tankzors.client.gameplay.entity.Direction;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AudioManagerTest {

    private AudioManager audio;

    @BeforeEach
    public void setup() {
        audio = new AudioManager();
    }

    @Test
    public void testDefaultVolumes() {
        assertEquals(0.8f, audio.getSoundVolume(), 1e-4);
        assertEquals(0.7f, audio.getMusicVolume(), 1e-4);
        assertFalse(audio.isMuted());
    }

    @Test
    public void testCustomConstructorVolumes() {
        AudioManager custom = new AudioManager(0.5f, 0.3f);
        assertEquals(0.5f, custom.getSoundVolume(), 1e-4);
        assertEquals(0.3f, custom.getMusicVolume(), 1e-4);
        assertFalse(custom.isMuted());
    }

    @Test
    public void testVolumeClamping() {
        audio.setSoundVolume(-0.5f);
        assertEquals(0.0f, audio.getSoundVolume(), 1e-4);

        audio.setSoundVolume(1.8f);
        assertEquals(1.0f, audio.getSoundVolume(), 1e-4);

        audio.setMusicVolume(-1.0f);
        assertEquals(0.0f, audio.getMusicVolume(), 1e-4);

        audio.setMusicVolume(2.5f);
        assertEquals(1.0f, audio.getMusicVolume(), 1e-4);
    }

    @Test
    public void testMuteToggle() {
        assertFalse(audio.isMuted());
        audio.toggleMute();
        assertTrue(audio.isMuted());
        audio.toggleMute();
        assertFalse(audio.isMuted());

        audio.setMuted(true);
        assertTrue(audio.isMuted());
        audio.setMuted(false);
        assertFalse(audio.isMuted());
    }

    @Test
    public void testHeadlessPlaybackDoesNotThrow() {
        // Trong môi trường test JUnit không có Gdx.audio -> không được crash
        assertDoesNotThrow(() -> {
            audio.loadAll();
            audio.playShoot(true);
            audio.playShoot(false);
            audio.playHitWall();
            audio.playHitTank(true);
            audio.playHitTank(false);
            audio.playTankDestroyed(true);
            audio.playTankDestroyed(false);
            audio.playUiClick();
            audio.playBattleMusic("campaign_1");
            audio.playBattleMusic("campaign_2");
            audio.playBattleMusic("campaign_3");
            audio.playVictoryMusic();
            audio.playDefeatMusic();
            audio.stopMusic();
            audio.dispose();
        });
    }

    @Test
    public void testSimulationEventsTriggered() {
        GameSimulation sim = new GameSimulation(12345L);
        List<String> events = new ArrayList<>();

        sim.setEventListener(new GameEventListener() {
            @Override
            public void onShoot(boolean isPlayer) {
                events.add("SHOOT_" + (isPlayer ? "PLAYER" : "AI"));
            }

            @Override
            public void onHitWall() {
                events.add("HIT_WALL");
            }

            @Override
            public void onHitTank(boolean isPlayer) {
                events.add("HIT_TANK_" + (isPlayer ? "PLAYER" : "AI"));
            }

            @Override
            public void onTankDestroyed(boolean isPlayer) {
                events.add("DESTROYED_" + (isPlayer ? "PLAYER" : "AI"));
            }

            @Override
            public void onMatchEnd(GameWorld.Status status) {
                events.add("MATCH_END_" + status.name());
            }
        });

        // 1. Tạo map đơn giản với tường, player và enemy
        MapDefinition map = TestMaps.parse(
                "##########",
                "#o...#..x#",
                "##########"
        );

        sim.startCampaignLevel(map, "campaign_1", "p1");
        assertTrue(sim.getWorld().isPlaying(), "Trận đấu phải đang ở trạng thái PLAYING");

        // 2. Player bắn đạn -> onShoot(true)
        sim.enqueueShoot();
        sim.step(1.0 / 30.0);
        assertTrue(events.contains("SHOOT_PLAYER"), "Phải kích hoạt onShoot(true) cho player");

        // 3. Đạn va chạm tường tại col 5, row 1 (x: 200..240, y: 40..80) -> onHitWall
        Bullet wallBullet = new Bullet("b-wall", "p1", new Position(220, 60), 90.0, 200, 25, 6, 6);
        sim.getWorld().addBullet(wallBullet);
        sim.step(1.0 / 30.0);
        assertTrue(events.contains("HIT_WALL"), "Phải kích hoạt onHitWall khi đạn chạm tường");

        // 4. Test đạn chạm AI tank (spawn tại col 8, row 1: x=340, y=60) -> onHitTank
        Bullet tankBullet = new Bullet("b-tank", "p1", new Position(340, 60), 0.0, 200, 1, 6, 6);
        sim.getWorld().addBullet(tankBullet);
        sim.step(1.0 / 30.0);
        assertTrue(events.contains("HIT_TANK_AI"), "Phải kích hoạt onHitTank khi đạn gây damage");

        // Bắn phát kết liễu -> onTankDestroyed và onMatchEnd(VICTORY)
        Bullet killBullet = new Bullet("b-kill", "p1", new Position(340, 60), 0.0, 200, 100, 6, 6);
        sim.getWorld().addBullet(killBullet);
        sim.step(1.0 / 30.0);
        assertTrue(events.contains("DESTROYED_AI"), "Phải kích hoạt onTankDestroyed khi tank chết");
        assertTrue(events.contains("MATCH_END_VICTORY"), "Phải kích hoạt onMatchEnd(VICTORY) khi hết địch");
    }
}
