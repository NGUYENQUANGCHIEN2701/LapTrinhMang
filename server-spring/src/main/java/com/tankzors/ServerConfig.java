package com.tankzors;

public final class ServerConfig {
    private ServerConfig() {
    }

    public static final int PORT = 8080;

    /** Số tick mỗi giây (30Hz = 33ms mỗi tick) */
    public static final int TICK_RATE = 30;

    /** Thời gian mỗi tick tính bằng milliseconds */
    public static final long TICK_INTERVAL_MS = 1000L / TICK_RATE;

    public static final int MAP_SIZE = 2048;

    /** Kích thước tile (pixel) */
    public static final int TILE_SIZE = 32;
}
