package com.tankzors.client.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetRepositoryTest {

    @Test
    void brownWallUsesAProgressivelyMoreBrokenFrameAfterEveryHit() {
        int[] frames = {
                AssetRepository.brownWallFrameIndex(4),
                AssetRepository.brownWallFrameIndex(3),
                AssetRepository.brownWallFrameIndex(2),
                AssetRepository.brownWallFrameIndex(1)
        };

        assertArrayEquals(new int[]{0, 2, 4, 5}, frames);
    }

    @Test
    void spawnUsesTheOriginalBuildsBaseAndTeamRingFrames() {
        assertEquals(0, AssetRepository.spawnBaseFrameIndex(true));
        assertEquals(1, AssetRepository.spawnBaseFrameIndex(false));
        assertEquals(1, AssetRepository.teamRingFrameIndex(true));
        assertEquals(2, AssetRepository.teamRingFrameIndex(false));
    }
}
