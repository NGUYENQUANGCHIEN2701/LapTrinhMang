package com.tankzors.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldCameraBoundsTest {

    @Test
    void centersMapWhenItIsNarrowerThanViewport() {
        assertEquals(320f, WorldCameraBounds.centerForAxis(40f, 640f, 800f));
        assertEquals(320f, WorldCameraBounds.centerForAxis(600f, 640f, 800f));
    }

    @Test
    void clampsCameraAtTopAndBottomMapEdges() {
        assertEquals(300f, WorldCameraBounds.centerForAxis(140f, 880f, 600f));
        assertEquals(580f, WorldCameraBounds.centerForAxis(780f, 880f, 600f));
    }

    @Test
    void followsTargetThroughScrollableMapArea() {
        assertEquals(440f, WorldCameraBounds.centerForAxis(440f, 880f, 600f));
    }
}
