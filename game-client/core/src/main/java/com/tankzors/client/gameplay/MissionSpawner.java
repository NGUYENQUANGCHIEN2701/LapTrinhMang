package com.tankzors.client.gameplay;

import com.tankzors.client.gameplay.map.MissionConfig;

/**
 * Base sinh xe kiểu Builds (game.Base + Game.updateBases với enemySpawnOrdered):
 * chỉ base đầu danh sách còn lượt mới sinh; giới hạn số xe cùng lúc trên sân;
 * chỉ đếm lùi thời gian chờ khi sân còn chỗ; ô base phải trống mới sinh.
 */
final class MissionSpawner {

    /** Ô base đang bị chiếm hay không (do simulation trả lời). */
    interface CellCheck {
        boolean isFree(int col, int row);
    }

    private final MissionConfig.Waves waves;
    private final int[] remaining;
    private double cooldown;

    MissionSpawner(MissionConfig.Waves waves, double initialDelaySeconds) {
        this.waves = waves;
        this.remaining = new int[waves.bases.size()];
        for (int i = 0; i < remaining.length; i++) remaining[i] = Math.max(0, waves.bases.get(i).count);
        this.cooldown = Math.max(0, initialDelaySeconds);
    }

    MissionConfig.Waves waves() { return waves; }

    /** Số xe còn chờ sinh trên mọi base. */
    int pending() {
        int sum = 0;
        for (int count : remaining) sum += count;
        return sum;
    }

    /**
     * Tiến một bước; trả base vừa được phép sinh (đã trừ lượt) hoặc null.
     * @param onField số xe phe này đang sống trên sân (không tính ụ súng)
     */
    MissionConfig.SpawnBase update(double dt, int onField, CellCheck cells) {
        int index = currentBase();
        if (index < 0 || onField >= waves.maxOnField) return null;
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - dt);
            return null;
        }
        MissionConfig.SpawnBase base = waves.bases.get(index);
        if (!cells.isFree(base.x, base.y)) return null;
        remaining[index]--;
        cooldown = waves.intervalSeconds;
        return base;
    }

    private int currentBase() {
        for (int i = 0; i < remaining.length; i++) {
            if (remaining[i] > 0) return i;
        }
        return -1;
    }
}
