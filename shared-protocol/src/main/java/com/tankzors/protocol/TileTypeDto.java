package com.tankzors.protocol;

/**
 * Loại ô bản đồ trên wire (v1).
 *
 * <p>Giá trị số truyền trong {@code STATE.map.grid[row][col]}.
 * Nguồn: {@code com.tankzors.map.TileType} của server cũ.</p>
 */
public enum TileTypeDto {
    GROUND(0),
    WALL(1),
    WATER(2),
    SPAWN_PLAYER(3),
    SPAWN_ENEMY(4);

    private final int value;

    TileTypeDto(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static TileTypeDto fromValue(int value) {
        for (TileTypeDto t : values()) {
            if (t.value == value) return t;
        }
        return GROUND;
    }
}
