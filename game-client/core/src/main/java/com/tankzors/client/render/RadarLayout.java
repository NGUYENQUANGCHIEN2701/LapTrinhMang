package com.tankzors.client.render;

/** Fit the entire map inside a top-right panel, preserving square cells. */
public final class RadarLayout {
    public final float x, y, width, height, left, top, cell;

    public RadarLayout(int screenWidth, int screenHeight, int mapWidth, int mapHeight) {
        float margin = Math.min(12, Math.min(screenWidth, screenHeight) * .025f);
        float availableWidth = Math.min(288, screenWidth * .36f);
        float availableHeight = Math.min(240, screenHeight * .40f);
        cell = Math.min((availableWidth - 16) / Math.max(1, mapWidth),
                (availableHeight - 58) / Math.max(1, mapHeight));
        width = cell * mapWidth + 16;
        height = cell * mapHeight + 58;
        x = screenWidth - margin - width;
        y = margin;
        left = x + 8;
        top = y + 32;
    }
}
