package com.tankzors.client.gameplay.ai;

/**
 * Bảng chỉ số xe AI của Builds (game.Base.createBot): chỉ số mảng
 * {@code typeId * 5 + level - 1}, typeId 0..7, level (enemyDifficulty) 1..5.
 */
public final class BuildsTankStats {
    private BuildsTankStats() {}

    public static final String[] TYPES = {"light", "light", "light", "kamikaze", "heavy", "heavy", "siege", "heavy"};
    private static final int[] HPS = {3, 3, 5, 3, 5, 5, 7, 9, 8, 7, 6, 8, 9, 9, 7, 1, 3, 3, 5, 1, 8, 10, 12, 16, 60,
            8, 10, 12, 16, 2, 5, 10, 15, 30, 90, 20, 30, 40, 90, 150};
    private static final int[] SHIELD_HPS = {0, 0, 0, 4, 0, 0, 0, 0, 5, 0, 0, 0, 3, 7, 0, 0, 0, 0, 0, 0, 0, 3, 4, 8, 10,
            0, 3, 5, 8, 30, 0, 3, 5, 10, 30, 7, 15, 20, 40, 60};
    private static final int[] SPEED_BONUSES = {1, 2, 2, 2, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 3, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1};
    private static final int[] BULLET_TYPES = {0, 0, 0, 1, 1, 0, 0, 0, 2, 0, 1, 1, 1, 3, 6, 0, 0, 0, 0, 0, 2, 2, 2, 4, 2,
            3, 3, 3, 5, 3, 0, 0, 0, 0, 0, 2, 3, 4, 6, 5};

    /** Bot Builds: maxReloadDelay 60 frame (60 Hz). */
    public static final int RELOAD_FRAMES = 60;

    private static int index(int typeId, int level) {
        int t = Math.max(0, Math.min(7, typeId));
        int l = Math.max(1, Math.min(5, level));
        return t * 5 + l - 1;
    }

    public static int hitPoints(int typeId, int level) { return HPS[index(typeId, level)]; }
    public static int shieldHitPoints(int typeId, int level) { return SHIELD_HPS[index(typeId, level)]; }
    public static int bulletType(int typeId, int level) { return BULLET_TYPES[index(typeId, level)]; }

    /** Tốc độ Builds: 2 + bonus; kamikaze +2, loại 7 -0.5. Đơn vị: 100/speed frame mỗi ô. */
    public static double speed(int typeId, int level) {
        double speed = 2.0 + SPEED_BONUSES[index(typeId, level)];
        if (typeId == 3) speed += 2;
        if (typeId == 7) speed -= 0.5;
        return speed;
    }

    /** Số frame giữa hai bước quay 22.5°: heavy 5, còn lại 4, kamikaze bớt 1. */
    public static int rotationDelay(int typeId) {
        int delay = "heavy".equals(TYPES[Math.max(0, Math.min(7, typeId))]) ? 5 : 4;
        if (typeId == 3) delay--;
        return delay;
    }
}
