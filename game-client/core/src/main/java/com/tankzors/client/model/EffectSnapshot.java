package com.tankzors.client.model;

/** Hiệu ứng ngắn hạn do simulation phát ra, renderer chỉ đọc. */
public final class EffectSnapshot {
    public float x;
    public float y;
    public float progress;
    public boolean bush;
    /** true = tia va chạm nhỏ; false = hiệu ứng phá hủy đầy đủ. */
    public boolean impact;
}
