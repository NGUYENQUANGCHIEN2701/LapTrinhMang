package com.tankzors.client.gameplay.map;

import com.tankzors.client.gameplay.entity.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kịch bản một màn campaign chuyển từ Builds (khối {@code mission} trong map JSON):
 * đợt địch theo base, xe đồng minh, ụ súng, thùng đồ, trigger và văn bản nhiệm vụ.
 * Chỉ là dữ liệu; {@code GameSimulation} diễn giải khi bắt đầu màn.
 */
public final class MissionConfig {

    public enum Objective {
        /** Builds winCondition 1: hạ hết xe tăng địch (ụ súng không bắt buộc). */
        DESTROY_TANKS,
        /** Builds winCondition 60: hạ hết xe tăng và ụ súng địch. */
        DESTROY_ALL
    }

    /** Một base sinh xe: ô (x, y) và số xe base này còn được sinh. */
    public static final class SpawnBase {
        public final int x;
        public final int y;
        public final int count;

        public SpawnBase(int x, int y, int count) {
            this.x = x;
            this.y = y;
            this.count = count;
        }
    }

    /**
     * Các base sinh xe lần lượt theo thứ tự danh sách, tối đa {@code maxOnField}
     * xe cùng lúc, mỗi lần sinh cách nhau {@code intervalSeconds}.
     */
    public static final class Waves {
        public final int maxOnField;
        public final double intervalSeconds;
        public final String difficulty;
        public final boolean followPlayer;
        public final List<SpawnBase> bases;
        /** Số xe mỗi loại Builds 0..7 (teamCounts); rỗng = toàn loại 0. */
        public final int[] types;
        /** Cấp độ khó Builds (enemyDifficulty 1..5). */
        public final int level;
        /** Builds enemyAIType & 8: chọn loại ngẫu nhiên thay vì lần lượt. */
        public final boolean randomTypes;

        public Waves(int maxOnField, double intervalSeconds, String difficulty,
                     boolean followPlayer, List<SpawnBase> bases) {
            this(maxOnField, intervalSeconds, difficulty, followPlayer, bases, new int[0], 1, false);
        }

        public Waves(int maxOnField, double intervalSeconds, String difficulty, boolean followPlayer,
                     List<SpawnBase> bases, int[] types, int level, boolean randomTypes) {
            this.maxOnField = Math.max(1, maxOnField);
            this.intervalSeconds = Math.max(0, intervalSeconds);
            this.difficulty = difficulty != null ? difficulty : "EASY";
            this.followPlayer = followPlayer;
            this.bases = bases != null ? Collections.unmodifiableList(new ArrayList<>(bases))
                    : Collections.<SpawnBase>emptyList();
            this.types = types != null ? java.util.Arrays.copyOf(types, types.length) : new int[0];
            this.level = Math.max(1, Math.min(5, level));
            this.randomTypes = randomTypes;
        }

        public int total() {
            int sum = 0;
            for (SpawnBase base : bases) sum += Math.max(0, base.count);
            return sum;
        }
    }

    /** Ụ súng cố định của phe địch, quay về {@code direction}, tầm {@code range} ô. */
    public static final class TurretConfig {
        public final int x;
        public final int y;
        public final Direction direction;
        public final int range;
        public final int hitPoints;

        public TurretConfig(int x, int y, Direction direction, int range, int hitPoints) {
            this.x = x;
            this.y = y;
            this.direction = direction != null ? direction : Direction.DOWN;
            this.range = Math.max(1, range);
            this.hitPoints = Math.max(1, hitPoints);
        }
    }

    /** Thùng đồ: nhặt được {@code count} vật phẩm {@code itemId} (-1 thùng rỗng). */
    public static final class BoxConfig {
        public final int x;
        public final int y;
        public final int itemId;
        public final int count;

        public BoxConfig(int x, int y, int itemId, int count) {
            this.x = x;
            this.y = y;
            this.itemId = itemId;
            this.count = count;
        }
    }

    public enum TriggerAction {
        /** Hiện dòng gợi ý {@code text} trong {@code seconds} giây. */
        TEXT,
        /** Kích hoạt trigger {@code target} như khi xe đi vào ô của nó. */
        PRESS,
        /** Bật trigger {@code target} (trigger tắt sẽ không phản hồi). */
        ENABLE,
        NONE
    }

    /** Ô trigger ẩn; chỉ kích hoạt một lần như Builds (không có cờ reusable). */
    public static final class TriggerConfig {
        public final int x;
        public final int y;
        public final boolean active;
        public final TriggerAction action;
        public final String text;
        public final double seconds;
        public final int target;

        public TriggerConfig(int x, int y, boolean active, TriggerAction action,
                             String text, double seconds, int target) {
            this.x = x;
            this.y = y;
            this.active = active;
            this.action = action != null ? action : TriggerAction.NONE;
            this.text = text;
            this.seconds = seconds;
            this.target = target;
        }
    }

    public static final class BuildingConfig {
        public final int x, y, team, hitPoints;
        public final String type;
        public BuildingConfig(int x, int y, String type, int team, int hitPoints) {
            this.x = x; this.y = y; this.type = type; this.team = team; this.hitPoints = Math.max(1, hitPoints);
        }
    }
    private final List<BuildingConfig> buildings = new ArrayList<>();
    public List<BuildingConfig> getBuildings() { return buildings; }

    private Objective objective = Objective.DESTROY_TANKS;
    private String briefing;
    private String debriefing;
    private Waves enemyWaves;
    private Waves allies;
    private final List<TurretConfig> turrets = new ArrayList<>();
    private final List<BoxConfig> boxes = new ArrayList<>();
    private final List<TriggerConfig> triggers = new ArrayList<>();

    public Objective getObjective() { return objective; }
    public void setObjective(Objective objective) {
        this.objective = objective != null ? objective : Objective.DESTROY_TANKS;
    }
    public String getBriefing() { return briefing; }
    public void setBriefing(String briefing) { this.briefing = briefing; }
    public String getDebriefing() { return debriefing; }
    public void setDebriefing(String debriefing) { this.debriefing = debriefing; }
    public Waves getEnemyWaves() { return enemyWaves; }
    public void setEnemyWaves(Waves enemyWaves) { this.enemyWaves = enemyWaves; }
    public Waves getAllies() { return allies; }
    public void setAllies(Waves allies) { this.allies = allies; }
    public List<TurretConfig> getTurrets() { return turrets; }
    public List<BoxConfig> getBoxes() { return boxes; }
    public List<TriggerConfig> getTriggers() { return triggers; }

    public BoxConfig boxAt(int x, int y) {
        for (BoxConfig box : boxes) {
            if (box.x == x && box.y == y) return box;
        }
        return null;
    }
}
