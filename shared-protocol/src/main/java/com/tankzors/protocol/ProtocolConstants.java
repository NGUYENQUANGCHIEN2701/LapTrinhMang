package com.tankzors.protocol;

/**
 * Tuning constants chốt từ protocol-v1 mục 4.
 *
 * <p>Mục đích: server-spring và game-client cùng tham chiếu tới các giá trị
 * này để Phase 2/3 khỏi lệch. Giá trị KHÔNG đổi trong Phase 1.</p>
 */
public final class ProtocolConstants {
    private ProtocolConstants() {
    }

    /** Port HTTP/WebSocket mặc định của server. */
    public static final int DEFAULT_PORT = 8080;

    /** WebSocket endpoint path (giữ nguyên khi migrate sang Spring Boot). */
    public static final String WS_PATH = "/game";

    /** HTTP health endpoint path. */
    public static final String HEALTH_PATH = "/api/health";

    /** Tick rate target (Hz). */
    public static final int TICK_RATE = 30;

    /** Tick interval milliseconds (1000 / TICK_RATE). */
    public static final long TICK_INTERVAL_MS = 1000L / TICK_RATE;

    // ===== Player =====
    public static final double PLAYER_SPEED = 150.0;
    public static final int PLAYER_HP = 100;
    public static final int PLAYER_MAX_HP = 100;
    public static final double PLAYER_WIDTH = 40.0;
    public static final double PLAYER_HEIGHT = 40.0;
    public static final double PLAYER_SHOOT_COOLDOWN = 0.5;

    // ===== Bullet =====
    public static final double BULLET_SPEED = 300.0;
    public static final int BULLET_DAMAGE = 25;
    public static final double BULLET_WIDTH = 8.0;
    public static final double BULLET_HEIGHT = 8.0;

    // ===== Collision =====
    public static final double TANK_WALL_MARGIN = 4.0;
    public static final double CORNER_NUDGE_MAX = 6.0;
}
