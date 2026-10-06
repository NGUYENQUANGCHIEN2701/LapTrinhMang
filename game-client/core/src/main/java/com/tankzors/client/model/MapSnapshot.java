package com.tankzors.client.model;

/**
 * Snapshot map được gửi mỗi khi level đổi (trong STATE.map).
 *
 * <p>Wire format:
 * {@code { width, height, tileSize, grid:[row][col], wallHitPoints:[row][col] }}.</p>
 *
 * <p>Tile values: 0=GROUND, 1=BRICK, 2=WATER, 3/4=SPAWN,
 * 5=WHITE_WALL, 6=BUSH, 7=REPAIR, 8=SPEED, 9=FLAG.</p>
 */
public final class MapSnapshot {
    public int width;
    public int height;
    public int tileSize = 40;
    public int[][] grid;
    /** Mã tile hình ảnh gốc, tùy chọn; gameplay vẫn chỉ dùng {@link #grid}. */
    public int[][] visualGrid;
    /** HP hiện tại của vật cản; renderer dùng 4/3/2/1 cho các frame tường nâu. */
    public int[][] wallHitPoints;
}
