package com.tankzors.client.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void campaignFlagUsesOriginalFrameWithAnIndependentVerticalFlip() {
        assertEquals(3, AssetRepository.flagFrameIndex());
        assertTrue(AssetRepository.flagRequiresVerticalFlip(),
                "Region riêng của cờ phải lật dọc cho camera y-down");
    }

    @Test
    void legacyTileIdsAddressOneBasedAtlasFrames() {
        assertEquals(0, AssetRepository.frameIndexForTileId(1, 106));
        assertEquals(52, AssetRepository.frameIndexForTileId(53, 106));
        assertEquals(105, AssetRepository.frameIndexForTileId(999, 106));
    }
}
