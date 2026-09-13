package com.tankzors.map;

/**
 * Loại ô trong lưới bản đồ.
 * Giá trị số tương ứng với dữ liệu trong file JSON map.
 */
public enum TileType {

    /** Ô đất - đi qua được. */
    GROUND(0),

    /** Ô tường - chặn di chuyển và chặn đạn. */
    WALL(1),

    /** Ô nước - chặn di chuyển nhưng không chặn đạn. */
    WATER(2),

    /** Điểm xuất phát của player. */
    SPAWN_PLAYER(3),

    /** Điểm xuất phát của AI. */
    SPAWN_ENEMY(4);

    private final int value;

    TileType(int value) {
        this.value = value;
    }

    /**
     * @return giá trị số đại diện trong file JSON map.
     */
    public int getValue() {
        return value;
    }

    /**
     * Chuyển từ giá trị số sang TileType.
     *
     * @param value giá trị số trong file JSON map
     * @return TileType tương ứng, mặc định GROUND nếu không khớp
     */
    public static TileType fromValue(int value) {
        for (TileType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return GROUND;
    }
}