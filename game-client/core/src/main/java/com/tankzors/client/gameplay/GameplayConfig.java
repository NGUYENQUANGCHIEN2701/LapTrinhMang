package com.tankzors.client.gameplay;

/**
 * Hằng số cấu hình gameplay client. Parity với ServerConfig cũ (30Hz).
 */
public final class GameplayConfig {
    private GameplayConfig() {}

    /** Tần suất mô phỏng (Hz) — bằng server tick-rate cũ. */
    public static final int TICK_RATE = 30;
    /** Thời gian một step mô phỏng (giây). */
    public static final double STEP_SECONDS = 1.0 / TICK_RATE;
    /** Thời gian một step (ms). */
    public static final long STEP_MILLIS = (long) Math.round(1000.0 / TICK_RATE);
    /** Delta tối đa nhận vào mỗi frame (giây) — chống " chạy bù" vô hạn khi lag. */
    public static final float MAX_FRAME_DELTA = 0.25f;
    /** Số step tối đa chạy mỗi frame. */
    public static final int MAX_STEPS_PER_FRAME = 5;
}
