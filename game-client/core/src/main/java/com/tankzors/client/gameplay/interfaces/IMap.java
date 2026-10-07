package com.tankzors.client.gameplay.interfaces;

import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.map.TileType;

import java.util.List;

/** Hợp đồng bản đồ. Parity server IMap. */
public interface IMap {
    int getTileSize();
    int getWidth();
    int getHeight();
    TileType getTileAt(int col, int row);
    List<Wall> getWalls();
    List<Position> getSpawnPoints();
    boolean isWalkable(int col, int row);

    /**
     * Hệ số tốc độ theo nền tại toạ độ pixel (Builds: cát chậm, đường nhanh).
     * Mặc định 1 cho map không có dữ liệu nền gốc.
     */
    default double speedFactorAt(double pixelX, double pixelY) { return 1.0; }
}
