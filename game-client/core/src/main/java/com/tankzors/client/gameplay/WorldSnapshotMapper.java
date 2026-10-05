package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.entity.Wall;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;
import com.tankzors.client.model.BulletSnapshot;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.model.MapSnapshot;
import com.tankzors.client.model.TankSnapshot;
import com.tankzors.client.gameplay.map.TileType;

/**
 * Map GameWorld (local simulation) → GameStateSnapshot (định dạng render cũ).
 * Cho phép GameRenderer xử lý local gameplay mà không cần parse JSON server.
 */
public final class WorldSnapshotMapper {
    private WorldSnapshotMapper() {}

    public static GameStateSnapshot snapshot(GameWorld world, String localPlayerId, int score, double elapsedSeconds) {
        GameStateSnapshot snap = new GameStateSnapshot();
        snap.gameStatus = world.getStatus().name();
        snap.score = score;
        // elapsedSeconds không nằm trong wire format cũ; ta gửi qua score hiển thị riêng
        // (renderer hiện không vẽ elapsed — giữ NHƯ CŨ để không phá UI).

        // Map
        IMap map = world.getCurrentMap();
        if (map != null) {
            MapSnapshot ms = new MapSnapshot();
            ms.width = map.getWidth();
            ms.height = map.getHeight();
            ms.tileSize = map.getTileSize();
            int[][] grid = new int[map.getHeight()][map.getWidth()];
            int[][] wallHitPoints = new int[map.getHeight()][map.getWidth()];
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[row].length; col++) {
                    grid[row][col] = map.getTileAt(col, row).getValue();
                }
            }
            for (Wall wall : map.getWalls()) {
                int row = wall.getRow();
                int col = wall.getCol();
                if (row >= 0 && row < wallHitPoints.length
                        && col >= 0 && col < wallHitPoints[row].length) {
                    wallHitPoints[row][col] = wall.getHitPoints();
                }
            }
            ms.grid = grid;
            ms.wallHitPoints = wallHitPoints;
            snap.map = ms;
        }

        // Tanks
        for (ITank tank : world.getAllTanks()) {
            TankSnapshot ts = new TankSnapshot();
            ts.id = tank.getId();
            ts.x = (float) tank.getPosition().getX();
            ts.y = (float) tank.getPosition().getY();
            ts.rotation = (float) tank.getRotation();
            ts.hp = tank.getHP();
            ts.maxHp = tank.getMaxHP();
            if (tank instanceof com.tankzors.client.gameplay.entity.Tank) {
                com.tankzors.client.gameplay.entity.Tank body = (com.tankzors.client.gameplay.entity.Tank) tank;
                ts.shieldHp = body.getShieldHp();
                ts.maxShieldHp = body.getMaxShieldHp();
            }
            ts.alive = tank.isAlive();
            ts.isPlayer = tank instanceof PlayerTank;
            snap.tanks.add(ts);
        }

        // Bullets (chỉ active)
        for (IBullet bullet : world.getAllBullets()) {
            if (!bullet.isActive()) continue;
            if (!(bullet instanceof com.tankzors.client.gameplay.entity.Bullet)) continue;
            com.tankzors.client.gameplay.entity.Bullet b = (com.tankzors.client.gameplay.entity.Bullet) bullet;
            BulletSnapshot bs = new BulletSnapshot();
            bs.id = b.getId();
            bs.x = (float) b.getPosition().getX();
            bs.y = (float) b.getPosition().getY();
            bs.direction = (float) b.getDirection();
            bs.ownerId = b.getOwnerId();
            bs.weaponType = b.getWeaponType();
            bs.damage = b.getDamage();
            bs.startX = (float) b.getStartX();
            bs.startY = (float) b.getStartY();
            bs.maxRange = (float) b.getMaxRange();
            bs.active = b.isActive();
            snap.bullets.add(bs);
        }

        return snap;
    }
}
