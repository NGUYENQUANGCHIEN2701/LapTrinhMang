package com.tankzors.client.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
}
