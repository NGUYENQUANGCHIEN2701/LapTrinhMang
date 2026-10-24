package com.tankzors.client.shop;

/**
 * Định nghĩa 10 nhánh cây công nghệ nghiên cứu (Research Tech Trees).
 * Mua và nâng cấp bằng Ngôi sao (Stars), trích xuất từ dữ liệu chuẩn Tankzors Lux.
 */
public enum ResearchTech {
    SHELL_RESEARCH("shellResearch", "Nghiên cứu đạn pháo", 8,
            new int[]{5, 7, 8, 8, 9, 4, 4, 4},
            "Mở khóa Plasma (cấp 1), nòng đôi (cấp 2), Plasma đôi (cấp 3), xuyên giáp (cấp 4, 5), tăng tốc bắn (cấp 6–8)."),

    ARTILLERY_RESEARCH("artilleryResearch", "Nghiên cứu pháo binh", 5,
            new int[]{4, 4, 4, 4, 4},
            "Mở khóa pháo cầu vồng bắn qua tường, tăng tầm bắn và sức công phá của vụ nổ."),

    MISSILE_RESEARCH("missileResearch", "Nghiên cứu tên lửa", 2,
            new int[]{5, 5},
            "Mở khóa đầu đạn tầm xa (cấp 1) và tên lửa dẫn đường tự hành phá tường bê tông (cấp 2)."),

    MINE_RESEARCH("mineResearch", "Nghiên cứu mìn", 2,
            new int[]{5, 5},
            "Mở khóa mìn chống tăng TMD-5 (cấp 1) và mìn chống tăng TMD-9 cực mạnh (cấp 2)."),

    DYNAMITE_RESEARCH("dynamiteResearch", "Nghiên cứu thuốc nổ", 3,
            new int[]{4, 4, 4},
            "Mở khóa thuốc nổ Dynamite x70 (cấp 1), x100 (cấp 2), x160 (cấp 3) phá hủy chướng ngại vật."),

    ARMOR_RESEARCH("armorResearch", "Nghiên cứu giáp trụ", 3,
            new int[]{5, 5, 5},
            "Mở khóa giáp tạm (cấp 1), giáp vĩnh cửu (cấp 2), giảm 30% giá mua giáp và phí sửa xe (cấp 3)."),

    POWER_FIELD_RESEARCH("powerFieldResearch", "Nghiên cứu khiên lực", 4,
            new int[]{5, 5, 5, 5},
            "Mở khóa khiên năng lượng tự hồi phục (cấp 1), tăng tốc sạc khiên và mở rộng dung tích lên 7 (cấp 4)."),

    AIR_RAID_RESEARCH("airRaidResearch", "Nghiên cứu không kích", 3,
            new int[]{6, 6, 6},
            "Mở khóa gọi máy bay ném bom yểm trợ (kết hợp Radar), tăng bán kính nổ và số lượng bom."),

    ALLY_TANK_RESEARCH("allyTankResearch", "Nghiên cứu đồng minh", 3,
            new int[]{6, 6, 6},
            "Mở khóa mua xe tăng đồng minh cấp 1, bọc thép cấp 2 và siêu thiết giáp cấp 3."),

    EXTRA_INTEREST("extraInterest", "Thưởng Xu bổ sung", 2,
            new int[]{9, 9},
            "Tăng thêm 5% (cấp 1) và 10% (cấp 2) tổng số Xu nhận được sau mỗi chiến thắng chiến dịch.");

    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final int[] starPrices;
    private final String description;

    ResearchTech(String id, String displayName, int maxLevel, int[] starPrices, String description) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.starPrices = starPrices;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getMaxLevel() { return maxLevel; }
    public int[] getStarPrices() { return starPrices; }
    public String getDescription() { return description; }

    public int getPriceForLevel(int currentLevel) {
        if (currentLevel >= 0 && currentLevel < starPrices.length) {
            return starPrices[currentLevel];
        }
        return 0;
    }

    public static ResearchTech findById(String id) {
        if (id == null) return null;
        for (ResearchTech tech : values()) {
            if (tech.id.equals(id)) return tech;
        }
        return null;
    }
}

