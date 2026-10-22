package com.tankzors.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadarLayoutTest {
    @Test void fitsWideTallAndLargeMapsAtDesktopAndMobileSizes() {
        for (int[] screen : new int[][]{{800, 600}, {480, 320}, {320, 480}, {1920, 1080}}) {
            for (int[] map : new int[][]{{21, 17}, {200, 12}, {12, 200}, {200, 200}}) {
                RadarLayout layout = new RadarLayout(screen[0], screen[1], map[0], map[1]);
                assertTrue(layout.cell > 0);
                assertTrue(layout.x > screen[0] / 2f);
                assertTrue(layout.x + layout.width < screen[0]);
                assertTrue(layout.y + layout.height < screen[1]);
                assertTrue(layout.width <= 288.01f);
                assertTrue(layout.height <= 240.01f);
                assertEquals(layout.width - 16, layout.cell * map[0], .001f);
                assertEquals(layout.height - 58, layout.cell * map[1], .001f);
            }
        }
    }
}
