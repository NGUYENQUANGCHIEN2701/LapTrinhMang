package com.tankzors.client.gameplay.map;

/**
 * Loại ô lưới map. Giá trị số tương ứng JSON map.
 * 0=GROUND,1=BRICK,2=WATER,3=SPAWN_PLAYER,4=SPAWN_ENEMY,
 * 5=WHITE_WALL,6=BUSH,7=REPAIR_PICKUP,8=SPEED_PICKUP,9=FLAG,
 * 10..14 = vật phẩm Builds (đóng băng, sao, xu, thùng đồ, mạng),
 * 15=STONE_WALL (khối đá tile 11: đạn không phá được, chỉ vụ nổ phá),
 * 16=DYNAMITE (thuốc nổ đang cháy ngòi, xe đi qua được).
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
    SPEED_PICKUP(8),
    FLAG(9),
    FREEZE_PICKUP(10),
    STAR_PICKUP(11),
    COIN_PICKUP(12),
    BOX_PICKUP(13),
    LIFE_PICKUP(14),
    STONE_WALL(15),
    DYNAMITE(16);

    private final int value;

    TileType(int value) { this.value = value; }

    public int getValue() { return value; }

    /** Vật phẩm nằm trên nền, nhặt khi xe người chơi đi vào ô. */
    public boolean isPickup() {
        switch (this) {
            case REPAIR_PICKUP: case SPEED_PICKUP: case FREEZE_PICKUP: case STAR_PICKUP:
            case COIN_PICKUP: case BOX_PICKUP: case LIFE_PICKUP:
                return true;
            default:
                return false;
        }
    }

    public static TileType fromValue(int value) {
        for (TileType type : values()) {
            if (type.value == value) return type;
        }
        return GROUND;
    }
}
