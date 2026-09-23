package com.tankzors.client.shop;

/**
 * Định nghĩa 10 nhánh cây công nghệ nghiên cứu (Research Tech Trees).
 * Mua và nâng cấp bằng Ngôi sao (Stars), trích xuất từ dữ liệu chuẩn Tankzors Lux.
 */
public enum ResearchTech {
    SHELL_RESEARCH("shellResearch", "Nghien cuu Dan Phao", 8,
            new int[]{5, 7, 8, 8, 9, 4, 4, 4},
            "Mo khoa Plasma (lv1), Nong doi (lv2), Double Plasma (lv3), Xuyen giap (lv4, lv5), tang toc ban (lv6-8)."),

    ARTILLERY_RESEARCH("artilleryResearch", "Nghien cuu Phao Binh", 5,
            new int[]{4, 4, 4, 4, 4},
            "Mo khoa phao cau vong ban qua tuong, tang tam ban va suc cong pha tam no."),

    MISSILE_RESEARCH("missileResearch", "Nghien cuu Ten Lua", 2,
            new int[]{5, 5},
            "Mo khoa Dau dan tam xa (lv1) va Ten lua dan duong tu hanh pha tuong be tong (lv2)."),

    MINE_RESEARCH("mineResearch", "Nghien cuu Min", 2,
            new int[]{5, 5},
            "Mo khoa Min chong tang TMD-5 (lv1) va Min chong tang TMD-9 cuc manh (lv2)."),

    DYNAMITE_RESEARCH("dynamiteResearch", "Nghien cuu Thuoc No", 3,
            new int[]{4, 4, 4},
            "Mo khoa thuoc no Dynamite x70 (lv1), x100 (lv2), x160 (lv3) pha huy chuong ngai vat."),

    ARMOR_RESEARCH("armorResearch", "Nghien cuu Giap Tru", 3,
            new int[]{5, 5, 5},
            "Mo khoa Giap tam (lv1), Giap vinh cuu (lv2), Giam 30% gia mua giap va phi sua xe (lv3)."),

    POWER_FIELD_RESEARCH("powerFieldResearch", "Nghien cuu Khien Luc", 4,
            new int[]{5, 5, 5, 5},
            "Mo khoa Khien nang luong tu hoi phuc (lv1), tang toc sac khien va mo rong dung tich len 7 (lv4)."),

    AIR_RAID_RESEARCH("airRaidResearch", "Nghien cuu Khong Kich", 3,
            new int[]{6, 6, 6},
            "Mo khoa Goi may bay nem bom yem tro (ket hop Radar), tang ban kinh no va so luong bom."),

    ALLY_TANK_RESEARCH("allyTankResearch", "Nghien cuu Dong Minh", 3,
            new int[]{6, 6, 6},
            "Mo khoa mua xe tang dong minh cap 1 (lv1), boc thep cap 2 (lv2) va sieu thiet giap cap 3 (lv3)."),

    EXTRA_INTEREST("extraInterest", "Thuong Lai Suat Coin", 2,
            new int[]{9, 9},
            "Tang them 5% (lv1) va 10% (lv2) tong so coin nhan duoc sau moi chien thang campaign.");

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

