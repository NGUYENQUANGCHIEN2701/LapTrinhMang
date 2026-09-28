package com.tankzors.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tầm nhìn hình tròn, không lưu vùng đã khám phá. Bán kính được nới động để
 * luôn nhìn thấy ít nhất 2/3 tổng số ô kể cả khi xe đứng sát mép map.
 */
public final class VisibilityMask {
    private final int centerCol;
    private final int centerRow;
    private final int radiusSquared;

    private VisibilityMask(int centerCol, int centerRow, int radiusSquared) {
        this.centerCol = centerCol;
        this.centerRow = centerRow;
        this.radiusSquared = radiusSquared;
    }

    public static VisibilityMask coveringTwoThirds(int width, int height, int centerCol, int centerRow) {
        List<Integer> distances = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int dx = col - centerCol;
                int dy = row - centerRow;
                distances.add(dx * dx + dy * dy);
            }
        }
        if (distances.isEmpty()) return new VisibilityMask(centerCol, centerRow, 0);
        Collections.sort(distances);
        int required = (width * height * 2 + 2) / 3;
        return new VisibilityMask(centerCol, centerRow,
                distances.get(Math.max(0, Math.min(required - 1, distances.size() - 1))));
    }

    public boolean isVisible(int col, int row) {
        int dx = col - centerCol;
        int dy = row - centerRow;
        return dx * dx + dy * dy <= radiusSquared;
    }

    public boolean isPixelVisible(float x, float y, int tileSize) {
        if (tileSize <= 0) return false;
        return isVisible((int) Math.floor(x / tileSize), (int) Math.floor(y / tileSize));
    }
}
