package com.tankzors.ai;

import com.tankzors.engine.GameState;
import com.tankzors.map.GameMap;

final class AIMaps {
    static GameMap open() {
        int[][] grid = new int[15][20];
        for (int y = 0; y < 15; y++) for (int x = 0; x < 20; x++) {
            if (x == 0 || y == 0 || x == 19 || y == 14) grid[y][x] = 1;
        }
        return new GameMap("test", 20, 15, 40, grid);
    }

    static GameMap parse(String... rows) {
        int[][] grid = new int[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            grid[y][x] = rows[y].charAt(x) == '#' ? 1 : rows[y].charAt(x) == '~' ? 2 : 0;
        }
        return new GameMap("fixture", rows[0].length(), rows.length, 40, grid);
    }

    static GameState state(GameMap map) {
        var state = new GameState();
        state.setCurrentMap(map);
        state.setStatus(GameState.Status.PLAYING);
        return state;
    }
}
