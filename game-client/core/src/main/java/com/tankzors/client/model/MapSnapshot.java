package com.tankzors.client.model;

/**
 * Snapshot map được gửi mỗi khi level đổi (trong STATE.map).
 *
 * <p>Wire format:
 * {@code { width, height, tileSize, grid:[row][col] }}.</p>
 *
 * <p>Tile values: 0=GROUND, 1=WALL, 2=WATER, 3=SPAWN_PLAYER, 4=SPAWN_ENEMY.</p>
 */
public final class MapSnapshot {
    public int width;
    public int height;
    public int tileSize = 40;
    public int[][] grid;
}
