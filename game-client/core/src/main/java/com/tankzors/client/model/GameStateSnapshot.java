package com.tankzors.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Toàn bộ STATE packet đã parse, dùng cho {@code GameRenderer}.
 *
 * <p>Phase 3 không yêu cầu interpolation; mỗi tick server (= 30Hz) ghi đè.
 * Engine render 60Hz sẽ render cùng state 2 lần.</p>
 */
public final class GameStateSnapshot {
    public String gameStatus = "WAITING";
    /**
     * Trạng thái chuyển màn của session (không phải world status). Khi khác null
     * ({@code "LOADING"} / {@code "LOAD_ERROR"}), renderer vẽ overlay chuyển màn
     * thay vì overlay VICTORY/DEFEAT, và gameStatus giữ nguyên kết quả cũ.
     */
    public String transitionStatus;
    public int score;
    public MapSnapshot map;
    public final List<TankSnapshot> tanks = new ArrayList<>();
    public final List<BulletSnapshot> bullets = new ArrayList<>();

    public void clearEntities() {
        tanks.clear();
        bullets.clear();
    }
}
