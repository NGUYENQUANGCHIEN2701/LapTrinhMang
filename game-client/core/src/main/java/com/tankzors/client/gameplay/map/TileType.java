package com.tankzors.client.gameplay.map;

/**
 * Loại ô lưới map. Giá trị số tương ứng JSON map.
 * 0=GROUND,1=BRICK,2=WATER,3=SPAWN_PLAYER,4=SPAWN_ENEMY,
 * 5=WHITE_WALL,6=BUSH,7=REPAIR_PICKUP,8=SPEED_PICKUP.
 */
public enum TileType {
    GROUND(0),
    WALL(1),
    WATER(2),
    SPAWN_PLAYER(3),
    SPAWN_ENEMY(4),
    WHITE_WALL(5),
    BUSH(6),
    REPAIR_PICKUP(7),
    SPEED_PICKUP(8);

    private final int value;

    TileType(int value) { this.value = value; }

    public int getValue() { return value; }

    public static TileType fromValue(int value) {
        for (TileType type : values()) {
            if (type.value == value) return type;
        }
        return GROUND;
    }
}
