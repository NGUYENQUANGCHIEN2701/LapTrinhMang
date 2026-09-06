package com.tankzors.interfaces;

import com.tankzors.entity.Position;
import com.tankzors.entity.Wall;
import com.tankzors.map.TileType;

import java.util.List;

/**
 * Interface đại diện cho bản đồ trong game.
 *
 * <p>Cung cấp thông tin về lưới ô, tường và điểm xuất phát. GameEngine và
 * CollisionManager chỉ phụ thuộc vào {@code IMap} nên có thể thay đổi cách
 * lưu trữ bản đồ (JSON, DB...) mà không ảnh hưởng phần lõi game
 * (Dependency Inversion Principle - DIP).</p>
 */
public interface IMap {

    /** Size of one grid cell in pixels. */
    int getTileSize();

    /**
     * @return chiều rộng bản đồ (số ô theo trục x).
     */
    int getWidth();

    /**
     * @return chiều cao bản đồ (số ô theo trục y).
     */
    int getHeight();

    /**
     * Lấy loại ô tại vị trí (x, y) trong lưới.
     *
     * @param x chỉ số cột (0-based)
     * @param y chỉ số hàng (0-based)
     * @return loại ô tại vị trí đó
     */
    TileType getTileAt(int x, int y);

    /**
     * @return danh sách tất cả bức tường trên bản đồ.
     */
    List<Wall> getWalls();

    /**
     * @return danh sách các điểm xuất phát (spawn points) của player và AI.
     */
    List<Position> getSpawnPoints();

    /**
     * Kiểm tra ô (x, y) có đi qua được không.
     *
     * @param x chỉ số cột (0-based)
     * @param y chỉ số hàng (0-based)
     * @return {@code true} nếu ô đó không phải tường/nước (đi được)
     */
    boolean isWalkable(int x, int y);
}