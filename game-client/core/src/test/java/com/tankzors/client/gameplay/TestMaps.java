package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.map.MapDefinition;

/** Fixtures map cho parity test core (tương đương server AIMaps). */
public final class TestMaps {
    private TestMaps() {}

    /** Map 20x15 mở, chỉ viền tường. */
    public static MapDefinition open() {
        int[][] grid = new int[15][20];
        for (int y = 0; y < 15; y++) for (int x = 0; x < 20; x++) {
            if (x == 0 || y == 0 || x == 19 || y == 14) grid[y][x] = 1;
        }
        return new MapDefinition("test", 20, 15, 40, grid);
    }

    /**
     * Map dạng text ASCII cho test nhanh:
     * '#' = WALL, '~' = WATER, '.' = GROUND, 'o' = SPAWN_PLAYER, 'x' = SPAWN_ENEMY.
     */
    public static MapDefinition parse(String... rows) {
        int[][] grid = new int[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                grid[y][x] = switch (c) {
                    case '#' -> 1;
                    case '~' -> 2;
                    case 'o' -> 3;
                    case 'x' -> 4;
                    default -> 0;
                };
            }
        }
        return new MapDefinition("fixture", rows[0].length(), rows.length, 40, grid);
    }

    /**
     * Map campaign_1 tái dựng — chỉ dùng cho test khi không load được resource.
     * Empty grid 20x15 với viền + 2 khối tường ở giữa.
     */
    public static MapDefinition singleCorridor() {
        return parse(
                "####################",
                "#o................x#",
                "#..................#",
                "#..................#",
                "#......######......#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#......######......#",
                "#..................#",
                "#..................#",
                "#..................#",
                "#..................#",
                "####################"
        );
    }
}
