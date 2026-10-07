package com.tankzors.client.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Vết bánh xích để lại trên nền khi tank di chuyển. Thuần hiển thị: chỉ đọc
 * toạ độ từ snapshot, không đụng tới simulation.
 *
 * <p>Mỗi khi tank đi được {@link #STAMP_SPACING} px kể từ dấu trước, một dấu
 * mới được ghi lại (vị trí + trục di chuyển). Dấu mờ dần trong
 * {@link #LIFETIME_SECONDS} giây; bộ đệm vòng giới hạn {@link #CAPACITY} dấu.</p>
 */
final class TrackMarks {

    static final float STAMP_SPACING = 4f;
    static final float LIFETIME_SECONDS = 7f;
    static final int CAPACITY = 2048;
    /** Nhảy xa hơn mức này giữa hai frame (respawn) thì không nối vết. */
    static final float MAX_STEP = 40f;

    private final float[] xs = new float[CAPACITY];
    private final float[] ys = new float[CAPACITY];
    private final boolean[] horizontal = new boolean[CAPACITY];
    private final float[] ages = new float[CAPACITY];
    private int head;
    private int size;

    /** Dấu cuối cùng của từng tank: {x, y}. */
    private final Map<String, float[]> lastStamp = new HashMap<>();
    private final Map<String, Boolean> seenThisFrame = new HashMap<>();

    void beginFrame(float deltaSeconds) {
        for (int i = 0; i < size; i++) {
            ages[index(i)] += deltaSeconds;
        }
        while (size > 0 && ages[index(0)] >= LIFETIME_SECONDS) {
            head = (head + 1) % CAPACITY;
            size--;
        }
        seenThisFrame.clear();
    }

    void track(String tankId, float x, float y) {
        if (tankId == null) return;
        seenThisFrame.put(tankId, Boolean.TRUE);
        float[] last = lastStamp.get(tankId);
        if (last == null) {
            lastStamp.put(tankId, new float[]{x, y});
            return;
        }
        float dx = x - last[0];
        float dy = y - last[1];
        float distSq = dx * dx + dy * dy;
        if (distSq > MAX_STEP * MAX_STEP) {
            last[0] = x;
            last[1] = y;
            return;
        }
        if (distSq < STAMP_SPACING * STAMP_SPACING) return;
        boolean alongX = Math.abs(dx) > Math.abs(dy);
        float dist = (float) Math.sqrt(distSq);
        int steps = (int) (dist / STAMP_SPACING);
        float stepX = dx / dist * STAMP_SPACING;
        float stepY = dy / dist * STAMP_SPACING;
        for (int i = 1; i <= steps; i++) {
            push(last[0] + stepX * i, last[1] + stepY * i, alongX);
        }
        last[0] += stepX * steps;
        last[1] += stepY * steps;
    }

    /** Bỏ trạng thái nối vết của tank không còn xuất hiện (chết / rời trận). */
    void endFrame() {
        Iterator<String> it = lastStamp.keySet().iterator();
        while (it.hasNext()) {
            if (!seenThisFrame.containsKey(it.next())) it.remove();
        }
    }

    void clear() {
        head = 0;
        size = 0;
        lastStamp.clear();
    }

    int size() { return size; }
    float x(int i) { return xs[index(i)]; }
    float y(int i) { return ys[index(i)]; }
    boolean isHorizontal(int i) { return horizontal[index(i)]; }

    /** Độ đậm 0..1, giảm tuyến tính theo tuổi của dấu. */
    float opacity(int i) {
        return Math.max(0f, 1f - ages[index(i)] / LIFETIME_SECONDS);
    }

    private void push(float x, float y, boolean alongX) {
        if (size == CAPACITY) {
            head = (head + 1) % CAPACITY;
            size--;
        }
        int slot = index(size);
        xs[slot] = x;
        ys[slot] = y;
        horizontal[slot] = alongX;
        ages[slot] = 0f;
        size++;
    }

    private int index(int i) {
        return (head + i) % CAPACITY;
    }
}
