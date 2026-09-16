package com.tankzors.client.gameplay.collision;

import com.tankzors.client.gameplay.entity.Position;
import com.tankzors.client.gameplay.entity.Rectangle;
import com.tankzors.client.gameplay.entity.Tank;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.ICollidable;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;

/**
 * Va chạm AABB_axis-aligned). Port nguyên vẹn từ server engine.CollisionManager:
 * - TANK_WALL_MARGIN = 4px (hitbox thực 32x32 trong ô 40px).
 * - resolveCollision: slide X/Y + corner nudge ≤6px.
 * Giữ byte-for-byte parity; chỉ đổi package + bỏ instanceof sang class cụ thể.
 */
public final class Collision {
    private Collision() {}

    public static final double TANK_WALL_MARGIN = 4.0;

    public static Rectangle getTankBoundingBoxAt(ITank tank, Position pos) {
        if (pos == null) return null;
        double width = (tank instanceof Tank) ? ((Tank) tank).getWidth() : 40.0;
        double height = (tank instanceof Tank) ? ((Tank) tank).getHeight() : 40.0;
        return new Rectangle(pos.getX() - width / 2.0, pos.getY() - height / 2.0, width, height);
    }

    public static Rectangle getTankWallCollisionBoxAt(ITank tank, Position pos) {
        if (pos == null) return null;
        double width = (tank instanceof Tank) ? ((Tank) tank).getWidth() : 40.0;
        double height = (tank instanceof Tank) ? ((Tank) tank).getHeight() : 40.0;
        return new Rectangle(
                pos.getX() - width / 2.0 + TANK_WALL_MARGIN,
                pos.getY() - height / 2.0 + TANK_WALL_MARGIN,
                Math.max(1.0, width - TANK_WALL_MARGIN * 2.0),
                Math.max(1.0, height - TANK_WALL_MARGIN * 2.0)
        );
    }

    public static boolean checkTankWallCollision(ITank tank, IMap map, Position targetPos) {
        if (tank == null || map == null || targetPos == null) return false;

        Rectangle targetBox = getTankWallCollisionBoxAt(tank, targetPos);
        if (targetBox == null) return false;

        int tileSize = map.getTileSize();
        double mapWidthPx = map.getWidth() * tileSize;
        double mapHeightPx = map.getHeight() * tileSize;

        if (targetBox.getX() < 0 || targetBox.getY() < 0
                || (targetBox.getX() + targetBox.getWidth()) > mapWidthPx
                || (targetBox.getY() + targetBox.getHeight()) > mapHeightPx) {
            return true;
        }

        int firstCol = (int) Math.floor(targetBox.getX() / tileSize);
        int lastCol = (int) Math.ceil((targetBox.getX() + targetBox.getWidth()) / tileSize) - 1;
        int firstRow = (int) Math.floor(targetBox.getY() / tileSize);
        int lastRow = (int) Math.ceil((targetBox.getY() + targetBox.getHeight()) / tileSize) - 1;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (!map.isWalkable(col, row)) return true;
            }
        }

        for (Wall wall : map.getWalls()) {
            if (targetBox.intersects(wall.getBoundingBox())) return true;
        }
        return false;
    }

    public static Position resolveCollision(ITank tank, Position targetPos, IMap map) {
        if (tank == null || targetPos == null) return targetPos;
        if (map == null) return targetPos;

        if (!checkTankWallCollision(tank, map, targetPos)) return targetPos;

        Position currentPos = tank.getPosition();

        // Slide X
        Position tryX = new Position(targetPos.getX(), currentPos.getY());
        if (!checkTankWallCollision(tank, map, tryX)) return tryX;

        // Slide Y
        Position tryY = new Position(currentPos.getX(), targetPos.getY());
        if (!checkTankWallCollision(tank, map, tryY)) return tryY;

        // Corner nudging
        int tileSize = map.getTileSize();
        double dy = targetPos.getY() - currentPos.getY();
        if (Math.abs(dy) > 1e-6) {
            int col = (int) Math.floor(currentPos.getX() / tileSize);
            double centerX = (col + 0.5) * tileSize;
            double diffX = centerX - currentPos.getX();
            if (Math.abs(diffX) > 1e-4 && Math.abs(diffX) <= 6.0) {
                double nudgeX = currentPos.getX() + Math.signum(diffX) * Math.min(Math.abs(diffX), 2.0);
                Position tryNudge = new Position(nudgeX, targetPos.getY());
                if (!checkTankWallCollision(tank, map, tryNudge)) return tryNudge;
            }
        }

        double dx = targetPos.getX() - currentPos.getX();
        if (Math.abs(dx) > 1e-6) {
            int row = (int) Math.floor(currentPos.getY() / tileSize);
            double centerY = (row + 0.5) * tileSize;
            double diffY = centerY - currentPos.getY();
            if (Math.abs(diffY) > 1e-4 && Math.abs(diffY) <= 6.0) {
                double nudgeY = currentPos.getY() + Math.signum(diffY) * Math.min(Math.abs(diffY), 2.0);
                Position tryNudge = new Position(targetPos.getX(), nudgeY);
                if (!checkTankWallCollision(tank, map, tryNudge)) return tryNudge;
            }
        }

        return currentPos;
    }

    public static boolean checkBulletWallCollision(IBullet bullet, IMap map) {
        if (bullet == null || map == null || !bullet.isActive()) return false;
        Position bulletPos = bullet.getPosition();
        if (bulletPos == null) return false;

        int tileSize = map.getTileSize();
        double mapWidthPx = map.getWidth() * tileSize;
        double mapHeightPx = map.getHeight() * tileSize;
        double bx = bulletPos.getX();
        double by = bulletPos.getY();

        if (bx < 0 || by < 0 || bx >= mapWidthPx || by >= mapHeightPx) return true;

        if (bullet instanceof ICollidable) {
            Rectangle bulletBox = ((ICollidable) bullet).getBoundingBox();
            for (Wall wall : map.getWalls()) {
                if (bulletBox.intersects(wall.getBoundingBox())) return true;
            }
        } else {
            int tileX = (int) (bx / tileSize);
            int tileY = (int) (by / tileSize);
            if (!map.isWalkable(tileX, tileY)) return true;
        }
        return false;
    }

    public static boolean checkBulletTankCollision(IBullet bullet, ITank tank) {
        if (bullet == null || tank == null) return false;
        if (!bullet.isActive() || !tank.isAlive()) return false;
        if (bullet.getOwnerId() != null && bullet.getOwnerId().equals(tank.getId())) return false;
        if (bullet instanceof ICollidable && tank instanceof ICollidable) {
            return ((ICollidable) bullet).collidesWith((ICollidable) tank);
        }
        return false;
    }
}
