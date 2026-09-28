package com.tankzors.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisibilityMaskTest {
    @Test
    void visibleAreaAlwaysCoversAtLeastTwoThirdsWithoutRememberingHiddenCells() {
        int width = 20;
        int height = 15;
        int required = (width * height * 2 + 2) / 3;
        for (int[] center : new int[][]{{0, 0}, {10, 7}, {19, 14}}) {
            VisibilityMask mask = VisibilityMask.coveringTwoThirds(width, height, center[0], center[1]);
            int visible = 0;
            for (int row = 0; row < height; row++)
                for (int col = 0; col < width; col++)
                    if (mask.isVisible(col, row)) visible++;
            assertTrue(visible >= required);
            assertTrue(visible < width * height, "Ngoài phạm vi vẫn phải có vùng tối hoàn toàn");
        }
    }
}
