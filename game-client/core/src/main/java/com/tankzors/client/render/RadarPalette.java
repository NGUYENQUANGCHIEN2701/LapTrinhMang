package com.tankzors.client.render;

import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.model.MapSnapshot;

/** Exact ARGB constants and layout from Builds game.effects.Radar. No GL dependency. */
public final class RadarPalette {
    private RadarPalette() {}
    public static int cellSize(int screenW, int screenH, int mapW, int mapH) {
        return Math.max(3, (int) (Math.min((double) screenW / mapW, (double) screenH / mapH) * .9));
    }
    public static int ground(int id) {
        if ((id >= 1 && id <= 23) || (id >= 53 && id <= 55) || (id >= 101 && id <= 106)) return -10985166;
        if ((id >= 24 && id <= 33) || (id >= 91 && id <= 100)) return -9276312;
        if (id >= 34 && id <= 52) return -7639235;
        if (id >= 56 && id <= 90) return -5006473;
        return 0xff000000;
    }
    public static int obstacle(int id) {
        if ((id >= 1 && id <= 8) || (id >= 14 && id <= 18)) return -11388657;
        if (id >= 9 && id <= 13) return -12566464;
        if (id >= 19 && id <= 21) return -12753045;
        return 0;
    }
    public static int team(int team) {
        return team == 1 ? 0xff009900 : team == 2 ? 0xff990000 : team == 3 ? 0xff000099 : 0xff999900;
    }
    public static int dim(int argb) {
        return (argb & 0xff000000) | (((argb >>> 16) & 255) / 2 << 16)
                | (((argb >>> 8) & 255) / 2 << 8) | ((argb & 255) / 2);
    }
    public static int sourceGround(MapSnapshot map, int col, int row) {
        if (map.visualGrid == null) return 1;
        int raw = map.visualGrid[row][col];
        if (raw >= 0 && raw < 91) return raw;
        if (raw >= 95 && raw <= 98) return (raw - 95) % 2 == 0 ? 25 : 29;
        if ((raw >= 91 && raw <= 94) || raw == 99) {
            return col > 0 ? sourceGround(map, col - 1, row) : row > 0 ? sourceGround(map, col, row - 1) : 1;
        }
        return MapDefinition.groundIdForCode(raw);
    }
    /** Map current gameplay cells back to Builds layer-1 IDs; destroyed cells stay empty. */
    public static int sourceObject(MapSnapshot map, int col, int row) {
        int tile = map.grid[row][col];
        int raw = map.visualGrid == null ? 0 : map.visualGrid[row][col];
        int original = raw >= 130 && raw < 250 ? (raw - 130) % 40 + 1 : 0;
        switch (tile) {
            case 1: return original >= 1 && original <= 8 ? original : 1;
            case 5: return original >= 9 && original <= 13 ? original : 12;
            case 15: return 11;
            case 6: return original >= 14 && original <= 18 ? original : 16;
            case 2: return 19;
            case 3: return 34;
            case 4: return 35;
            case 7: return 29;
            case 8: return 26;
            case 9: return raw >= 115 && raw < 130 ? 37 + (raw - 115) % 5 : 37;
            case 10: return 28;
            case 11: return 30;
            case 12: return 31;
            case 13: return 32;
            case 14: return 27;
            case 16: return 25;
            default: return 0;
        }
    }
}
