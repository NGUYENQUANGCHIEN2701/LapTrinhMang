package com.tankzors.engine;

import com.tankzors.interfaces.IBullet;
import com.tankzors.entity.AITank;
import com.tankzors.entity.PlayerTank;
import com.tankzors.interfaces.ITank;
import com.tankzors.interfaces.IMap;
import com.tankzors.network.protocol.StateMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Trạng thái hiện tại của trận đấu (tanks, bullets, map...).
 *
 * <p>Thread-safe: sử dụng ConcurrentHashMap cho tanks và CopyOnWriteArrayList
 * cho bullets vì GameLoop thread ghi, broadcast thread đọc.</p>
 */
public class GameState {

    /**
     * Trạng thái trận đấu.
     */
    public enum Status {
        WAITING,    // Đang chờ người chơi
        PLAYING,    // Đang chơi
        VICTORY,    // Người chơi thắng
        DEFEAT      // Người chơi thua
    }

    // Danh sách xe tăng: tankId -> ITank
    private final Map<String, ITank> tanks = new ConcurrentHashMap<>();

    // Danh sách viên đạn đang hoạt động
    private final List<IBullet> bullets = new CopyOnWriteArrayList<>();

    // Bản đồ hiện tại (nullable - chưa load map)
    private IMap currentMap;

    // Trạng thái game
    private volatile Status status = Status.WAITING;

    // Điểm số người chơi
    private volatile int score = 0;

    // Số tick đã trôi qua
    private long tickCount = 0;

    // ========== TANK MANAGEMENT ==========

    /**
     * Thêm xe tăng vào trận đấu.
     */
    public void addTank(ITank tank) {
        if (tank != null) {
            tanks.put(tank.getId(), tank);
        }
    }

    /**
     * Xóa xe tăng khỏi trận đấu.
     */
    public void removeTank(String tankId) {
        ITank removed = tanks.remove(tankId);
        if (removed instanceof AITank ai) ai.getController().reset();
        if (removed instanceof PlayerTank player) player.stopMoving();
    }

    /**
     * Lấy xe tăng theo ID.
     */
    public ITank getTank(String tankId) {
        return tanks.get(tankId);
    }

    /**
     * Lấy toàn bộ danh sách xe tăng (read-only view).
     */
    public Collection<ITank> getAllTanks() {
        return Collections.unmodifiableCollection(tanks.values());
    }

    /**
     * Số lượng xe tăng đang hoạt động.
     */
    public int getTankCount() {
        return tanks.size();
    }

    // ========== BULLET MANAGEMENT ==========

    /**
     * Thêm viên đạn vào trận đấu.
     */
    public void addBullet(IBullet bullet) {
        if (bullet != null) {
            bullets.add(bullet);
        }
    }

    /**
     * Xóa tất cả viên đạn không còn hoạt động (isActive = false).
     */
    public void removeInactiveBullets() {
        bullets.removeIf(bullet -> !bullet.isActive());
    }

    /**
     * Lấy toàn bộ danh sách viên đạn (read-only view).
     */
    public List<IBullet> getAllBullets() {
        return Collections.unmodifiableList(bullets);
    }

    /**
     * Số lượng viên đạn đang bay.
     */
    public int getBulletCount() {
        return bullets.size();
    }

    // ========== MAP ==========

    public IMap getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(IMap map) {
        this.currentMap = map;
    }

    // ========== STATUS & SCORE ==========

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isPlaying() {
        return status == Status.PLAYING;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public long getTickCount() {
        return tickCount;
    }

    public void incrementTick() {
        tickCount++;
    }

    // ========== SERIALIZATION ==========

    /**
     * Chuyển đổi GameState thành StateMessage để gửi về client.
     *
     * <p>Mỗi tank/bullet được serialize thành Map chứa thông tin cần thiết
     * để client render (vị trí, hướng, HP, trạng thái...).</p>
     */
    public StateMessage toStateMessage() {
        StateMessage msg = new StateMessage();

        // Serialize tanks
        List<Object> tankList = new ArrayList<>();
        for (ITank tank : tanks.values()) {
            Map<String, Object> tankData = new HashMap<>();
            tankData.put("id", tank.getId());
            tankData.put("x", tank.getPosition().getX());
            tankData.put("y", tank.getPosition().getY());
            tankData.put("rotation", tank.getRotation());
            tankData.put("hp", tank.getHP());
            tankData.put("maxHp", tank.getMaxHP());
            tankData.put("alive", tank.isAlive());
            tankData.put("isPlayer", tank instanceof PlayerTank);
            tankList.add(tankData);
        }
        msg.setTanks(tankList);

        // Serialize bullets
        List<Object> bulletList = new ArrayList<>();
        for (IBullet bullet : bullets) {
            if (!bullet.isActive()) continue;
            Map<String, Object> bulletData = new HashMap<>();
            if (bullet instanceof com.tankzors.entity.Bullet entity) bulletData.put("id", entity.getId());
            bulletData.put("active", bullet.isActive());
            bulletData.put("x", bullet.getPosition().getX());
            bulletData.put("y", bullet.getPosition().getY());
            bulletData.put("direction", bullet.getDirection());
            bulletData.put("ownerId", bullet.getOwnerId());
            bulletList.add(bulletData);
        }
        msg.setBullets(bulletList);
        if (currentMap != null) {
            int[][] grid = new int[currentMap.getHeight()][currentMap.getWidth()];
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[row].length; col++) grid[row][col] = currentMap.getTileAt(col, row).getValue();
            }
            msg.setMap(Map.of("width", currentMap.getWidth(), "height", currentMap.getHeight(),
                    "tileSize", currentMap.getTileSize(), "grid", grid));
        }

        // Score & Status
        msg.setScore(score);
        msg.setGameStatus(status.name());

        return msg;
    }

    /**
     * Reset trạng thái game (dùng khi bắt đầu ván mới).
     */
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

    @Override
    public String toString() {
        return "GameState{status=" + status
                + ", tanks=" + tanks.size()
                + ", bullets=" + bullets.size()
                + ", score=" + score
                + ", tick=" + tickCount + "}";
    }
}
