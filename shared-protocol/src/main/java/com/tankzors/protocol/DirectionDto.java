package com.tankzors.protocol;

/**
 * Hướng di chuyển tank trên wire (v1).
 *
 * <p>Giá trị string in hoa, không dash/underscore. Phù hợp với
 * {@code client/src/shared/types/game.ts} direction type.</p>
 */
public enum DirectionDto {
    UP,
    DOWN,
    LEFT,
    RIGHT
}
