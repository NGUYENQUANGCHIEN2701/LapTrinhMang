package com.tankzors.client.shop;

/**
 * Định nghĩa danh mục vật phẩm trong Cửa Hàng Tankzors.
 * Dữ liệu và icon ID chuẩn theo Tankzors Lux (từ thư mục Builds).
 */
public enum ShopItem {
    // --- ĐẠN DƯỢC CƠ BẢN (Mua theo lốc 10 viên) ---
    ROUND(0, "Shell", Category.BASIC_AMMO, 5, 10, 99, null, 0, "Dan tieu chuan co ban cua xe tang."),
    PLASMA(1, "Plasma", Category.BASIC_AMMO, 15, 10, 99, "shellResearch", 1, "May tao tia plasma nang luong cao."),
    DOUBLE_ROUND(2, "Double shell", Category.BASIC_AMMO, 30, 10, 40, "shellResearch", 2, "Ban cung luc 2 vien dan nong doi."),
    DOUBLE_PLASMA(3, "Double plasma", Category.BASIC_AMMO, 55, 10, 40, "shellResearch", 3, "Ban cung luc 2 tia plasma cuc manh."),
    ROUND2(4, "Armor piercing shell", Category.BASIC_AMMO, 70, 10, 10, "shellResearch", 4,
            "Dan xuyen giap 3 HP, van bay sau khi pha dan."),
    ROUND3(5, "Reinforced piercing", Category.BASIC_AMMO, 120, 10, 10, "shellResearch", 5,
            "Dan xuyen giap cuong hoa, sat thuong 5 HP."),

    // --- VŨ KHÍ BỔ TRỢ & ĐẶC BIỆT (Mua từng đơn vị) ---
    ARTILLERY(6, "Artillery", Category.SPECIAL_AMMO, 100, 1, 99, "artilleryResearch", 1, "Phao cau vong, bay qua tuong va no vung."),
    WARHEAD(7, "Warhead", Category.SPECIAL_AMMO, 60, 1, 99, "missileResearch", 1, "Dau dan tam xa, no vung 3 o."),
    ROCKET(8, "Guided missile", Category.SPECIAL_AMMO, 320, 1, 99, "missileResearch", 2, "Ten lua dan duong, pha tuong thuong."),
    MINE(10, "TMD-5 mines", Category.SPECIAL_AMMO, 10, 1, 99, "mineResearch", 1, "Min chong tang co ban."),
    MINE2(11, "TMD-9 mines", Category.SPECIAL_AMMO, 25, 1, 99, "mineResearch", 2, "Min chong tang sat thuong cao."),
    DYNAMITE(12, "x70 dynamite", Category.SPECIAL_AMMO, 35, 1, 99, "dynamiteResearch", 1, "Thuoc no cong pha chuong ngai vat."),
    DYNAMITE2(13, "x100 dynamite", Category.SPECIAL_AMMO, 60, 1, 99, "dynamiteResearch", 2, "Thuoc no cong pha manh pha tuong be tong."),
    DYNAMITE3(14, "x160 dynamite", Category.SPECIAL_AMMO, 110, 1, 99, "dynamiteResearch", 3, "Thuoc no uy luc cuc dai vung no rong."),
    AIR_SUPPORT(9, "Air support", Category.SPECIAL_AMMO, 170, 1, 99, "airRaidResearch", 1, "Goi may bay nem bom yem tro."),

    // --- PHÒNG THỦ & HỖ TRỢ ---
    ARMOR(21, "Temp Armor", Category.DEFENSE, 110, 1, 4, "armorResearch", 1, "Giap tam thoi (+1 HP, mat khi xe bi pha huy)."),
    ARMOR2(22, "Perm Armor", Category.DEFENSE, 250, 1, 4, "armorResearch", 2, "Giap vinh cuu nang cap do ben xe (+1 HP)."),
    FORCE_FIELD(20, "Force Field", Category.DEFENSE, 100, 1, 5, "powerFieldResearch", 1, "Khien nang luong tu dong tai tao."),
    REPAIR(19, "Repair Kit", Category.SUPPORT, 40, 1, 99, null, 0, "Tui cuu thuong hoi phuc HP trong tran."),
    RADAR(23, "Radar", Category.SUPPORT, 320, 1, 1, null, 0, "Mo ban do tong quan; cho phep mua khong kich."),
    SPEED_UP(17, "Nitro Speed", Category.SUPPORT, 35, 1, 99, null, 0, "Tang toc do di chuyen va giam hoi nong sung."),
    FREEZE(15, "Freeze EMP", Category.SUPPORT, 120, 1, 99, null, 0, "Dong bang toan bo xe tang va thap phao dich."),
    EX_CHASSIS(-1, "Enh. Chassis", Category.SUPPORT, 910, 1, 1, null, 0, "Khung gam mo rong suc chua dan len 99 vien."),
    ADD_LIFE(-2, "Extra Life", Category.SUPPORT, 170, 1, 9, null, 0, "Them 1 luot hoi sinh khi xe tang bi pha huy."),

    // --- XE ĐỒNG MINH ---
    ALLY_TANK(25, "Ally Tank 1", Category.ALLIES, 90, 1, 3, "allyTankResearch", 1, "Goi xe tang dong minh cap 1 yem tro."),
    ALLY_TANK2(26, "Ally Tank 2", Category.ALLIES, 210, 1, 3, "allyTankResearch", 2, "Goi xe tang boc thep dong minh cap 2."),
    ALLY_TANK3(27, "Ally Tank 3", Category.ALLIES, 420, 1, 3, "allyTankResearch", 3, "Goi sieu xe tang dong minh cap 3.");

    public enum Category {
        BASIC_AMMO("Dan Duoc"),
        SPECIAL_AMMO("Vu Khi Dac Biet"),
        DEFENSE("Phong Thu"),
        SUPPORT("Ho Tro"),
        ALLIES("Dong Minh");

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
