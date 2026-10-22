package com.tankzors.client.render;

import com.tankzors.client.model.MapSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadarPaletteTest {
    @Test void colorsAndLayoutMatchBuildsArgbConstants() {
        assertEquals(0xff009900, RadarPalette.team(1));
        assertEquals(0xff990000, RadarPalette.team(2));
        assertEquals(0xff000099, RadarPalette.team(3));
        assertEquals(0xff999900, RadarPalette.team(-3));
        assertEquals(-10985166, RadarPalette.ground(1));
        assertEquals(-9276312, RadarPalette.ground(24));
        assertEquals(-7639235, RadarPalette.ground(34));
        assertEquals(-5006473, RadarPalette.ground(56));
        assertEquals(0xff000000, RadarPalette.ground(0));
        assertEquals(-11388657, RadarPalette.obstacle(1));
        assertEquals(-12566464, RadarPalette.obstacle(12));
        assertEquals(-12753045, RadarPalette.obstacle(19));
        assertEquals(0xff4c4c00, RadarPalette.dim(0xff999900));
        assertEquals(24, RadarPalette.cellSize(800, 600, 16, 22));
        assertEquals(3, RadarPalette.cellSize(100, 100, 200, 200));
    }
    @Test void currentStateOverridesOriginalSourceTilesAfterDestructionOrCollection() {
        MapSnapshot map = new MapSnapshot();
        map.grid = new int[][]{{1, 0, 7, 0, 9, 5, 0}};
        map.visualGrid = new int[][]{{130, 130, 164, 164, 115, 141, 93}};
        assertEquals(1, RadarPalette.sourceObject(map, 0, 0));
        assertEquals(0, RadarPalette.sourceObject(map, 1, 0));
        assertEquals(29, RadarPalette.sourceObject(map, 2, 0));
        assertEquals(0, RadarPalette.sourceObject(map, 3, 0));
        assertEquals(37, RadarPalette.sourceObject(map, 4, 0));
        assertEquals(12, RadarPalette.sourceObject(map, 5, 0));
        assertEquals(0, RadarPalette.sourceObject(map, 6, 0));
        assertEquals(1, RadarPalette.sourceGround(map, 6, 0));
    }
}
