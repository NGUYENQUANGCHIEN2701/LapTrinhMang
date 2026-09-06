package com.tankzors.engine;

import com.tankzors.entity.Position;
import com.tankzors.entity.Rectangle;
import com.tankzors.entity.Tank;
import com.tankzors.entity.Wall;
import com.tankzors.interfaces.IBullet;
import com.tankzors.interfaces.ICollidable;
import com.tankzors.interfaces.IMap;
import com.tankzors.interfaces.ITank;

/**
 * Quản lý và xử lý va chạm trong game (Collision Detection & Resolution).
 *
 * <p>Sử dụng phương pháp kiểm tra va chạm hình hộp 2D AABB
 * (Axis-Aligned Bounding Box) cho các thực thể xe tăng, tường và đạn.</p>
 */
public class CollisionManager {

    /**
     * Lấy Bounding Box của xe tăng nếu đặt tại toạ độ tâm {@code pos}.
     */
    public static Rectangle getTankBoundingBoxAt(ITank tank, Position pos) {
        if (pos == null) return null;
        double width = (tank instanceof Tank) ? ((Tank) tank).getWidth() : 40.0;
        double height = (tank instanceof Tank) ? ((Tank) tank).getHeight() : 40.0;
        return new Rectangle(
                pos.getX() - width / 2.0,
                pos.getY() - height / 2.0,
                width,
                height
        );
    }

    /**
     * Kiểm tra xem xe tăng khi đặt tại vị trí {@code targetPos} có bị va chạm với tường
     * hoặc đi ra ngoài biên của bản đồ hay không.
     *
     * @param tank      xe tăng cần kiểm tra
     * @param map       bản đồ game hiện tại
     * @param targetPos vị trí dự kiến của xe tăng (toạ độ tâm)
     * @return {@code true} nếu có va chạm (không hợp lệ)
     */
    public static boolean checkTankWallCollision(ITank tank, IMap map, Position targetPos) {
        if (tank == null || map == null || targetPos == null) {
            return false;
        }

        Rectangle targetBox = getTankBoundingBoxAt(tank, targetPos);
        if (targetBox == null) return false;

        // 1. Kiểm tra tràn ra ngoài biên bản đồ (Canvas boundaries)
        int tileSize = map.getTileSize();
        double mapWidthPx = map.getWidth() * tileSize;
        double mapHeightPx = map.getHeight() * tileSize;

        if (targetBox.getX() < 0 || targetBox.getY() < 0
                || (targetBox.getX() + targetBox.getWidth()) > mapWidthPx
                || (targetBox.getY() + targetBox.getHeight()) > mapHeightPx) {
            return true;
        }

        // Include water and the whole tank footprint, not only its center cell.
        int firstCol = (int) Math.floor(targetBox.getX() / tileSize);
        int lastCol = (int) Math.ceil((targetBox.getX() + targetBox.getWidth()) / tileSize) - 1;
        int firstRow = (int) Math.floor(targetBox.getY() / tileSize);
        int lastRow = (int) Math.ceil((targetBox.getY() + targetBox.getHeight()) / tileSize) - 1;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (!map.isWalkable(col, row)) return true;
            }
        }

        // 2. Kiểm tra giao cắt với tất cả các bức tường trên bản đồ
        for (Wall wall : map.getWalls()) {
            if (targetBox.intersects(wall.getBoundingBox())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Giải quyết va chạm cho xe tăng:
     * <ul>
     *   <li>Nếu vị trí {@code targetPos} không va chạm -> cho phép đi đến đó.</li>
     *   <li>Nếu có va chạm nhưng có thể trượt theo trục X -> cho phép trượt trục X.</li>
     *   <li>Nếu có va chạm nhưng có thể trượt theo trục Y -> cho phép trượt trục Y.</li>
     *   <li>Nếu cả 2 trục đều bị chặn -> giữ nguyên vị trí cũ.</li>
     * </ul>
     *
     * @param tank      xe tăng đang di chuyển
     * @param targetPos vị trí mong muốn di chuyển đến
     * @param map       bản đồ game
     * @return vị trí hợp lệ cuối cùng sau khi giải quyết va chạm
     */
    public static Position resolveCollision(ITank tank, Position targetPos, IMap map) {
        if (tank == null || targetPos == null) {
            return targetPos;
        }
        if (map == null) {
            return targetPos; // Nếu chưa load map thì cho di chuyển tự do
        }

        // 1. Nếu vị trí mới hoàn toàn thông thoáng
        if (!checkTankWallCollision(tank, map, targetPos)) {
            return targetPos;
        }

        Position currentPos = tank.getPosition();

        // 2. Thử trượt dọc theo trục X (giữ nguyên Y cũ)
        Position tryX = new Position(targetPos.getX(), currentPos.getY());
        if (!checkTankWallCollision(tank, map, tryX)) {
            return tryX;
        }

        // 3. Thử trượt dọc theo trục Y (giữ nguyên X cũ)
        Position tryY = new Position(currentPos.getX(), targetPos.getY());
        if (!checkTankWallCollision(tank, map, tryY)) {
            return tryY;
        }

        // 4. Bị chặn cả 2 hướng -> đứng yên tại vị trí hiện tại
        return currentPos;
    }

    /**
     * Kiểm tra xem viên đạn có va chạm với tường hoặc bay ra ngoài map hay không.
     *
     * @param bullet viên đạn cần kiểm tra
     * @param map    bản đồ game
     * @return {@code true} nếu đạn chạm tường hoặc ra ngoài biên
     */
    public static boolean checkBulletWallCollision(IBullet bullet, IMap map) {
        if (bullet == null || map == null || !bullet.isActive()) {
            return false;
        }

        Position bulletPos = bullet.getPosition();
        if (bulletPos == null) return false;

        int tileSize = map.getTileSize();
        double mapWidthPx = map.getWidth() * tileSize;
        double mapHeightPx = map.getHeight() * tileSize;

        double bx = bulletPos.getX();
        double by = bulletPos.getY();

        // Tràn ra ngoài biên bản đồ
        if (bx < 0 || by < 0 || bx >= mapWidthPx || by >= mapHeightPx) {
            return true;
        }

        // Kiểm tra theo Bounding Box nếu bullet implements ICollidable
        if (bullet instanceof ICollidable) {
            Rectangle bulletBox = ((ICollidable) bullet).getBoundingBox();
            for (Wall wall : map.getWalls()) {
                if (bulletBox.intersects(wall.getBoundingBox())) {
                    return true;
                }
            }
        } else {
            // Kiểm tra theo toạ độ ô (Tile coordinate)
            int tileX = (int) (bx / tileSize);
            int tileY = (int) (by / tileSize);
            if (!map.isWalkable(tileX, tileY)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Kiểm tra xem viên đạn có bắn trúng một xe tăng hay không.
     *
     * @param bullet viên đạn
     * @param tank   xe tăng
     * @return {@code true} nếu đạn chạm xe tăng (khác chủ sở hữu)
     */
    public static boolean checkBulletTankCollision(IBullet bullet, ITank tank) {
        if (bullet == null || tank == null) return false;
        if (!bullet.isActive() || !tank.isAlive()) return false;
        if (bullet.getOwnerId() != null && bullet.getOwnerId().equals(tank.getId())) {
            return false; // Không tự bắn chính mình
        }

        if (bullet instanceof ICollidable && tank instanceof ICollidable) {
            return ((ICollidable) bullet).collidesWith((ICollidable) tank);
        }

        return false;
    }
}
