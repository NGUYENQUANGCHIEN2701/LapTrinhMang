package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.entity.AITank;
import com.tankzors.client.gameplay.entity.PlayerTank;
import com.tankzors.client.gameplay.interfaces.IBullet;
import com.tankzors.client.gameplay.interfaces.IMap;
import com.tankzors.client.gameplay.interfaces.ITank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trạng thái trận đấu (tanks, bullets, map). Port từ server engine.GameState —
 * bỏ tất cả concurrency (ConcurrentHashMap/CopyOnWriteArrayList/volatile) vì
 * client chạy single-thread trên render thread.
 */
public class GameWorld {

    public enum Status { WAITING, PLAYING, VICTORY, DEFEAT }

    private final Map<String, ITank> tanks = new HashMap<>();
    private final List<IBullet> bullets = new ArrayList<>();
    private IMap currentMap;
    private Status status = Status.WAITING;
    private int score = 0;
    private long tickCount = 0;

    // ========== TANK ==========
    public void addTank(ITank tank) { if (tank != null) tanks.put(tank.getId(), tank); }

    public void removeTank(String tankId) {
        ITank removed = tanks.remove(tankId);
        if (removed instanceof AITank ai) ai.getController().reset();
        if (removed instanceof PlayerTank player) player.stopMoving();
    }

    public ITank getTank(String tankId) { return tanks.get(tankId); }

    public Collection<ITank> getAllTanks() { return Collections.unmodifiableCollection(tanks.values()); }

    public int getTankCount() { return tanks.size(); }

    // ========== BULLET ==========
    public void addBullet(IBullet bullet) { if (bullet != null) bullets.add(bullet); }

    public void removeInactiveBullets() {
        // ArrayList.removeIf tương đương hành vi COWL.removeIf, single-thread an toàn
        for (int i = bullets.size() - 1; i >= 0; i--) {
            if (!bullets.get(i).isActive()) bullets.remove(i);
        }
    }

    public List<IBullet> getAllBullets() { return Collections.unmodifiableList(bullets); }

    public int getBulletCount() { return bullets.size(); }

    // ========== MAP ==========
    public IMap getCurrentMap() { return currentMap; }
    public void setCurrentMap(IMap map) { this.currentMap = map; }

    // ========== STATUS & SCORE ==========
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isPlaying() { return status == Status.PLAYING; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void addScore(int points) { this.score += points; }
    public long getTickCount() { return tickCount; }
    public void incrementTick() { tickCount++; }

    /** Reset về WAITING (dùng restart). */
    public void reset() {
        for (ITank tank : tanks.values()) {
            if (tank instanceof AITank ai) ai.getController().reset();
            if (tank instanceof PlayerTank player) player.stopMoving();
        }
        tanks.clear();
        bullets.clear();
        score = 0;
        tickCount = 0;
        status = Status.WAITING;
    }

    @Override public String toString() {
        return "GameWorld{status=" + status + ", tanks=" + tanks.size()
                + ", bullets=" + bullets.size() + ", score=" + score + ", tick=" + tickCount + "}";
    }
}
