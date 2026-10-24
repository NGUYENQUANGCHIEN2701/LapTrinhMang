package com.tankzors.client.shop;

/**
 * Định nghĩa danh mục vật phẩm trong Cửa Hàng Tankzors.
 * Dữ liệu và icon ID chuẩn theo Tankzors Lux (từ thư mục Builds).
 */
public enum ShopItem {
    // --- ĐẠN DƯỢC CƠ BẢN (Mua theo lốc 10 viên) ---
    ROUND(0, "Đạn thường", Category.BASIC_AMMO, 5, 10, 99, null, 0, "Đạn tiêu chuẩn cơ bản của xe tăng."),
    PLASMA(1, "Plasma", Category.BASIC_AMMO, 15, 10, 99, "shellResearch", 1, "Máy tạo tia plasma năng lượng cao."),
    DOUBLE_ROUND(2, "Đạn đôi", Category.BASIC_AMMO, 30, 10, 40, "shellResearch", 2, "Bắn cùng lúc 2 viên đạn nòng đôi."),
    DOUBLE_PLASMA(3, "Plasma đôi", Category.BASIC_AMMO, 55, 10, 40, "shellResearch", 3, "Bắn cùng lúc 2 tia plasma cực mạnh."),
    ROUND2(4, "Đạn xuyên giáp", Category.BASIC_AMMO, 70, 10, 10, "shellResearch", 4,
            "Đạn xuyên giáp 3 HP, vẫn bay sau khi phá đạn."),
    ROUND3(5, "Xuyên giáp cường hóa", Category.BASIC_AMMO, 120, 10, 10, "shellResearch", 5,
            "Đạn xuyên giáp cường hóa, sát thương 5 HP."),

    // --- VŨ KHÍ BỔ TRỢ & ĐẶC BIỆT (Mua từng đơn vị) ---
    ARTILLERY(6, "Pháo cầu vồng", Category.SPECIAL_AMMO, 100, 1, 99, "artilleryResearch", 1, "Pháo cầu vồng, bay qua tường và nổ vùng."),
    WARHEAD(7, "Đầu đạn tầm xa", Category.SPECIAL_AMMO, 60, 1, 99, "missileResearch", 1, "Đầu đạn tầm xa, nổ vùng 3 ô."),
    ROCKET(8, "Tên lửa dẫn đường", Category.SPECIAL_AMMO, 320, 1, 99, "missileResearch", 2, "Tên lửa dẫn đường, phá tường thường."),
    MINE(10, "Mìn TMD-5", Category.SPECIAL_AMMO, 10, 1, 99, "mineResearch", 1, "Mìn chống tăng cơ bản."),
    MINE2(11, "Mìn TMD-9", Category.SPECIAL_AMMO, 25, 1, 99, "mineResearch", 2, "Mìn chống tăng sát thương cao."),
    DYNAMITE(12, "Thuốc nổ x70", Category.SPECIAL_AMMO, 35, 1, 99, "dynamiteResearch", 1, "Thuốc nổ công phá chướng ngại vật."),
    DYNAMITE2(13, "Thuốc nổ x100", Category.SPECIAL_AMMO, 60, 1, 99, "dynamiteResearch", 2, "Thuốc nổ công phá mạnh, phá tường bê tông."),
    DYNAMITE3(14, "Thuốc nổ x160", Category.SPECIAL_AMMO, 110, 1, 99, "dynamiteResearch", 3, "Thuốc nổ uy lực cực đại, vùng nổ rộng."),
    AIR_SUPPORT(9, "Không kích", Category.SPECIAL_AMMO, 170, 1, 99, "airRaidResearch", 1, "Gọi máy bay ném bom yểm trợ."),

    // --- PHÒNG THỦ & HỖ TRỢ ---
    ARMOR(21, "Giáp tạm thời", Category.DEFENSE, 110, 1, 4, "armorResearch", 1, "Giáp tạm thời (+1 HP, mất khi xe bị phá hủy)."),
    ARMOR2(22, "Giáp vĩnh cửu", Category.DEFENSE, 250, 1, 4, "armorResearch", 2, "Giáp vĩnh cửu nâng cấp độ bền xe (+1 HP)."),
    FORCE_FIELD(20, "Khiên năng lượng", Category.DEFENSE, 100, 1, 5, "powerFieldResearch", 1, "Khiên năng lượng tự động tái tạo."),
    REPAIR(19, "Bộ sửa xe", Category.SUPPORT, 40, 1, 99, null, 0, "Túi cứu thương hồi phục HP trong trận."),
    RADAR(23, "Radar", Category.SUPPORT, 320, 1, 1, null, 0, "Mở bản đồ tổng quan; cho phép mua không kích."),
    SPEED_UP(17, "Tăng tốc Nitro", Category.SUPPORT, 35, 1, 99, null, 0, "Tăng tốc độ di chuyển và giảm hồi nòng súng."),
    FREEZE(15, "Thiết bị đóng băng", Category.SUPPORT, 120, 1, 99, null, 0, "Đóng băng toàn bộ xe tăng và tháp pháo địch."),
    EX_CHASSIS(-1, "Khung gầm mở rộng", Category.SUPPORT, 910, 1, 1, null, 0, "Khung gầm mở rộng sức chứa đạn lên 99 viên."),
    ADD_LIFE(-2, "Mạng bổ sung", Category.SUPPORT, 170, 1, 9, null, 0, "Thêm 1 lượt hồi sinh khi xe tăng bị phá hủy."),

    // --- XE ĐỒNG MINH ---
    ALLY_TANK(25, "Xe đồng minh cấp 1", Category.ALLIES, 90, 1, 3, "allyTankResearch", 1, "Gọi xe tăng đồng minh cấp 1 yểm trợ."),
    ALLY_TANK2(26, "Xe đồng minh cấp 2", Category.ALLIES, 210, 1, 3, "allyTankResearch", 2, "Gọi xe tăng bọc thép đồng minh cấp 2."),
    ALLY_TANK3(27, "Xe đồng minh cấp 3", Category.ALLIES, 420, 1, 3, "allyTankResearch", 3, "Gọi siêu xe tăng đồng minh cấp 3.");

    public enum Category {
        BASIC_AMMO("Đạn dược"),
        SPECIAL_AMMO("Vũ khí đặc biệt"),
        DEFENSE("Phòng thủ"),
        SUPPORT("Hỗ trợ"),
        ALLIES("Đồng minh");

        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private final int id;
    private final String displayName;
    private final Category category;
    private final int basePrice;
    private final int packSize;
    private final int baseLimit;
    private final String requiredTech;
    private final int requiredTechLevel;
    private final String description;

    ShopItem(int id, String displayName, Category category, int basePrice, int packSize, int baseLimit,
             String requiredTech, int requiredTechLevel, String description) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.basePrice = basePrice;
        this.packSize = packSize;
        this.baseLimit = baseLimit;
        this.requiredTech = requiredTech;
        this.requiredTechLevel = requiredTechLevel;
        this.description = description;
    }

    public int getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Category getCategory() { return category; }
    public int getBasePrice() { return basePrice; }
    public int getPackSize() { return packSize; }
    public int getBaseLimit() { return baseLimit; }
    public String getRequiredTech() { return requiredTech; }
    public int getRequiredTechLevel() { return requiredTechLevel; }
    public String getDescription() { return description; }

    public static ShopItem findById(int id) {
        for (ShopItem item : values()) {
            if (item.id == id) return item;
        }
        return null;
    }
}
