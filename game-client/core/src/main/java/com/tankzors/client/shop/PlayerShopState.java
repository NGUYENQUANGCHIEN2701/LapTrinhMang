package com.tankzors.client.shop;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý trạng thái cửa hàng, túi đồ và cây công nghệ phía Client.
 * Chạy 100% client-authoritative theo kiến trúc dự án và chuẩn Tankzors Lux.
 */
public class PlayerShopState {

    /** Hidden persisted state: Force Field plates already mounted on the tank. */
    public static final int INSTALLED_FORCE_FIELD_ID = -3;

    private int coins = 0;
    private int stars = 0;
    private final Map<Integer, Integer> inventory = new HashMap<>();
    private final Map<String, Integer> research = new HashMap<>();
    private Map<Integer, Integer> shopOpeningInventory;

    /** Builds only refunds ammunition purchased during this shop visit. */
    public void beginShopVisit() { shopOpeningInventory = snapshotInventory(); }
    public void endShopVisit() { shopOpeningInventory = null; }

    private int activeWeaponId = 0; // Mặc định là Round (0)

    public PlayerShopState() {
        // Trang bị khởi đầu của một campaign mới.
        inventory.put(ShopItem.ROUND.getId(), 99);
        inventory.put(ShopItem.REPAIR.getId(), 3);
        inventory.put(ShopItem.SPEED_UP.getId(), 2);
    }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }
    public void addCoins(int amount) { if (amount > 0) this.coins += amount; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = Math.max(0, stars); }
    public void addStars(int amount) { if (amount > 0) this.stars += amount; }

    public int getActiveWeaponId() { return activeWeaponId; }
    public void setActiveWeaponId(int id) { this.activeWeaponId = id; }

    public void selectWeapon(int weaponId) {
        if (isWeaponSlot(weaponId) && getItemCount(weaponId) > 0) {
            this.activeWeaponId = weaponId;
        }
    }

    /** Fire-key slots: rounds 0-5, artillery/missiles 6-8, air strike 9, mines 10-11, dynamite 12-14. */
    public static boolean isWeaponSlot(int itemId) {
        return itemId >= 0 && itemId <= ShopItem.DYNAMITE3.getId();
    }

    public void cycleNextWeapon() {
        int[] weaponIds = {0, 1, 2, 3, 4, 5};
        int currentIndex = 0;
        for (int i = 0; i < weaponIds.length; i++) {
            if (weaponIds[i] == activeWeaponId) {
                currentIndex = i;
                break;
            }
        }
        for (int step = 1; step <= weaponIds.length; step++) {
            int nextId = weaponIds[(currentIndex + step) % weaponIds.length];
            if (getItemCount(nextId) > 0) {
                this.activeWeaponId = nextId;
                return;
            }
        }
    }

    /** Chọn loại đạn đầu tiên còn hàng; trả active hiện tại nếu tất cả đã hết. */
    public int selectFirstAvailableWeapon() {
        if (getItemCount(activeWeaponId) > 0) return activeWeaponId;
        for (int weaponId = 0; weaponId <= 5; weaponId++) {
            if (getItemCount(weaponId) > 0) {
                activeWeaponId = weaponId;
                return activeWeaponId;
            }
        }
        return activeWeaponId;
    }

    public int getItemCount(int itemId) {
        return inventory.getOrDefault(itemId, 0);
    }

    public void setItemCount(int itemId, int count) {
        inventory.put(itemId, Math.max(0, count));
    }

    public boolean consumeItem(int itemId) {
        int count = getItemCount(itemId);
        if (count <= 0) return false;
        inventory.put(itemId, count - 1);
        return true;
    }

    /** Bản sao túi đồ dùng làm checkpoint đầu màn. */
    public Map<Integer, Integer> snapshotInventory() {
        return new HashMap<>(inventory);
    }

    /** Khôi phục đúng túi đồ đầu màn khi chơi lại, không ảnh hưởng tiền/nghiên cứu. */
    public void restoreInventory(Map<Integer, Integer> snapshot) {
        if (snapshot == null) return;
        inventory.clear();
        inventory.putAll(snapshot);
        selectFirstAvailableWeapon();
    }

    public int getResearchLevel(String techId) {
        return research.getOrDefault(techId, 0);
    }

    public void setResearchLevel(String techId, int level) {
        research.put(techId, Math.max(0, level));
    }

    public boolean hasExChassis() {
        return getItemCount(ShopItem.EX_CHASSIS.getId()) > 0;
    }

    public boolean hasRadar() {
        return getItemCount(ShopItem.RADAR.getId()) > 0;
    }

    public int getInstalledForceField() {
        return getItemCount(INSTALLED_FORCE_FIELD_ID);
    }

    public void addInstalledForceField(int amount) {
        if (amount > 0) setItemCount(INSTALLED_FORCE_FIELD_ID, getInstalledForceField() + amount);
    }

    public void clearInstalledForceField() {
        setItemCount(INSTALLED_FORCE_FIELD_ID, 0);
    }

    public int getEffectiveLimit(ShopItem item) {
        if (item == null) return 99;
        int id = item.getId();
        if (hasExChassis()) {
            if (id == ShopItem.DOUBLE_ROUND.getId() || id == ShopItem.DOUBLE_PLASMA.getId()
                    || id == ShopItem.ROUND2.getId() || id == ShopItem.ROUND3.getId()) {
                return 99;
            }
        }
        if (id == ShopItem.FORCE_FIELD.getId()) {
            return getResearchLevel("powerFieldResearch") >= 4 ? 7 : 5;
        }
        if (id == ShopItem.ARMOR.getId() || id == ShopItem.ARMOR2.getId()) {
            return getResearchLevel("armorResearch") >= 3 ? 5 : 4;
        }
        return item.getBaseLimit();
    }

    public int getEffectivePrice(ShopItem item) {
        if (item == null) return 0;
        if (item == ShopItem.REPAIR) {
            int unitCost = getResearchLevel("armorResearch") >= 3 ? 5 : 8;
            int maxHp = 5 + getArmorHpBonus() + (hasExChassis() ? 2 : 0);
            return maxHp * unitCost;
        }
        int price = item.getBasePrice();
        if (getResearchLevel("armorResearch") >= 3) {
            if (item == ShopItem.ARMOR || item == ShopItem.ARMOR2 || item == ShopItem.FORCE_FIELD) {
                // Giảm 30% giá theo công thức Tankzors Lux gốc
                price = price - (price * 30 / 100);
            }
        }
        return price;
    }

    public String getItemDescription(ShopItem item) {
        int level = getResearchLevel("artilleryResearch");
        switch (item) {
            case ROUND: return "Dan thuong: 1 HP. Gia theo 10 vien. Tru do ben tuong gach; het luc thi dung.";
            case PLASMA: return "Plasma: 1 HP, bay nhanh; nap nhanh hon dan thuong 10 tick. Gia theo 10 vien.";
            case DOUBLE_ROUND: return "Dan doi: 2 HP; con luc thi bay tiep sau khi pha gach/bui hoac chan dan. Gia theo 10 vien.";
            case DOUBLE_PLASMA: return "Plasma doi: 2 HP, nap nhanh hon 10 tick; con luc thi bay tiep. Gia theo 10 vien.";
            case ROUND2: return "Xuyen giap: 3 HP; mat luc theo do ben gach/bui va dan bi chan, con luc thi bay tiep. Gia theo 10 vien.";
            case ROUND3: return "Xuyen giap cuong hoa: 5 HP; pha gach/bui roi bay tiep neu con luc. Gia theo 10 vien.";
            case ARTILLERY:
                return "Phao cau vong: luc " + com.tankzors.client.gameplay.SpecialAmmoRules.artilleryPower(level)
                        + ", tam " + com.tankzors.client.gameplay.SpecialAmmoRules.artilleryDistance(level)
                        + " o, vung no " + (com.tankzors.client.gameplay.SpecialAmmoRules.artilleryRadius(level) * 2 - 1) + " o.";
            case WARHEAD: return "Tang toc dan; cham xe/tuong no. Luc 9, vung 3 o; giam 1 moi o. Khong pha be tong/thep.";
            case ROCKET: return "WASD lai ten lua; ban lan nua de no. Luc 32, vung 5 o. Pha o be tong va cham, khong pha thep.";
            case MINE: return "Dat duoi xe; chi no khi dich de len, tru 5 HP.";
            case MINE2: return "Dat duoi xe; chi no khi dich de len, tru 9 HP.";
            case DYNAMITE: return "Ngoi 3 giay; luc 7, vung 5 o. Can than tu sat thuong.";
            case DYNAMITE2: return "Ngoi 3 giay; luc 10, vung 5 o. Pha da.";
            case DYNAMITE3: return "Ngoi 3 giay; luc 19, vung 7 o. Pha da.";
            case AIR_SUPPORT:
                int air = getResearchLevel("airRaidResearch");
                return "Can Radar; chon muc tieu. " + com.tankzors.client.gameplay.SpecialAmmoRules.airBombs(air)
                        + " bom, luc " + com.tankzors.client.gameplay.SpecialAmmoRules.airPower(air)
                        + ", vung " + (com.tankzors.client.gameplay.SpecialAmmoRules.airRadius(air) * 2 - 1) + " o.";
            case ARMOR:
                int armorMax = getResearchLevel("armorResearch") >= 3 ? 5 : 4;
                return "Giap tam thoi (+1 HP). Mat 1 tam khi xe bi tieu diet. Toi da " + armorMax + " HP.";
            case ARMOR2:
                int permMax = getResearchLevel("armorResearch") >= 3 ? 5 : 4;
                return "Giap vinh cuu (+1 HP). Nang cap do ben xe vinh vien. Toi da " + permMax + " HP.";
            case FORCE_FIELD:
                int shieldMax = getResearchLevel("powerFieldResearch") >= 4 ? 7 : 5;
                return "Khien nang luong: Tu dong tai tao sau 5s khong dinh dan. Toi da " + shieldMax + " HP.";
            case REPAIR:
                return "Tui cuu thuong: Dung trong tran (phim Q) de hoi day HP ngay lap tuc.";
            case SPEED_UP:
                int speedSec = hasExChassis() ? 60 : 45;
                return "Tang toc do di chuyen va giam hoi nong sung trong " + speedSec + " giay.";
            case FREEZE:
                int freezeSec = hasExChassis() ? 10 : 5;
                return "Dong bang toan bo xe tang, thap phao va dan cua dich trong " + freezeSec + " giay.";
            case EX_CHASSIS:
                return "Khung gam tang nang: +2 HP, mo rong suc chua dan dac biet len 99 vien, tang thoi gian Nitro (+15s) va Freeze (+5s).";
            case ADD_LIFE:
                return "Them 1 luot hoi sinh ngay tai tran khi xe tang bi tieu diet.";
            case RADAR:
                return "Mo ban do mini tong quan tran dau (phim Q); cho phep mua va goi Khong kich.";
            case ALLY_TANK: return "Xe dong minh hang nhe: 8 HP, ho tro tan cong va di theo nguoi choi.";
            case ALLY_TANK2: return "Xe dong minh hang nhe nang cap: 15 HP, ho tro tan cong va di theo nguoi choi.";
            case ALLY_TANK3: return "Xe dong minh hang nang: 25 HP, ho tro tan cong va di theo nguoi choi.";
            default: return item.getDescription();
        }
    }

    /** Instructions for the selected item in the paused Q inventory. */
    public String getInventoryUsage(ShopItem item) {
        switch (item) {
            case ROUND: case PLASMA: case DOUBLE_ROUND: case DOUBLE_PLASMA:
            case ROUND2: case ROUND3:
                return "Enter: trang bi. Space / J: ban theo huong xe.\n"
                        + "Damage con lai sau khi pha gach/bui se tiep tuc bay. Khong pha be tong/thep.";
            case ARTILLERY:
                return "Enter: trang bi. Space / J: ban qua vat can den dung tam.\n"
                        + "Vu no giam 1 luc moi o; gay sat thuong ca xe cua ban. Khong pha be tong/thep.";
            case WARHEAD:
                return "Enter: trang bi. Space / J: phong dau dan, no khi cham xe dich hoac tuong.\n"
                        + "Vu no giam 1 luc moi o va co the gay sat thuong xe cua ban.";
            case ROCKET:
                return "Enter: trang bi. Space / J: phong; WASD / mui ten: lai ten lua.\n"
                        + "Ban lan nua de kich no. Xe dung yen khi dieu khien ten lua. Can than vu no gan xe.";
            case MINE: case MINE2:
                return "Enter: trang bi. Space / J: dat tren o trong duoi xe.\n"
                        + "Chi kich hoat khi xe dich de len; an toan voi xe cua ban va dong minh.";
            case DYNAMITE: case DYNAMITE2: case DYNAMITE3:
                return "Enter: trang bi. Space / J: dat tren o trong duoi xe. Roi xa truoc 3 giay!\n"
                        + "Vu no giam 1 luc moi o, pha gach/bui/khoi da va gay sat thuong ca xe cua ban.";
            case AIR_SUPPORT:
                return "Enter: trang bi. Space / J: mo ngam; WASD / mui ten: chon o.\n"
                        + "Space / Enter: goi may bay. Q: huy ngam. Can than bom trung xe cua ban.";
            case REPAIR:
                return "Enter: dung ngay de hoi day HP cua xe. Khong tieu hao khi xe da day HP.";
            case SPEED_UP:
                return "Enter: dung ngay de tang toc di chuyen va nap dan. Thoi gian con lai hien tren HUD.";
            case FREEZE:
                return "Enter: dung ngay de dong bang xe, thap phao va dan cua dich trong mot khoang thoi gian.";
            case FORCE_FIELD:
                return "Enter: lap cac mieng khien trong tui den gioi han. Khien tu hoi phuc sau khi ngung bi tan cong.";
            case ALLY_TANK: case ALLY_TANK2: case ALLY_TANK3:
                return "Enter: goi xe dong minh tai can cu xuat phat. Xe tu tim dich va bam theo ban.\n"
                        + "Hay di ra khoi can cu truoc khi goi. Can cu bi chiem cho thi khong mat vat pham.";
            case RADAR:
                return "Enter: mo ban do tong quan. Radar duoc giu lai sau khi dung.";
            default: return "Enter: chon vat pham.";
        }
    }

    public String getUnlockHint(ShopItem item) {
        if (item == ShopItem.EX_CHASSIS && hasExChassis()) {
            return "[DA SO HUU]";
        }
        if (item == ShopItem.RADAR && hasRadar()) {
            return "[DA SO HUU]";
        }
        if (item.getRequiredTech() != null && getResearchLevel(item.getRequiredTech()) < item.getRequiredTechLevel()) {
            ResearchTech tech = ResearchTech.findById(item.getRequiredTech());
            return "Can " + (tech == null ? item.getRequiredTech() : tech.getDisplayName()) + " lv" + item.getRequiredTechLevel();
        }
        if (item == ShopItem.AIR_SUPPORT && !hasRadar()) return "Can mua Radar";
        return "";
    }

    public boolean isItemUnlocked(ShopItem item) {
        if (item == null) return false;
        if (item.getRequiredTech() != null) {
            int currentTechLevel = getResearchLevel(item.getRequiredTech());
            if (currentTechLevel < item.getRequiredTechLevel()) {
                return false;
            }
        }
        if (item == ShopItem.AIR_SUPPORT) {
            return getResearchLevel("airRaidResearch") >= 1 && hasRadar();
        }
        return true;
    }

    public boolean canBuy(ShopItem item) {
        if (!isItemUnlocked(item)) return false;
        int price = getEffectivePrice(item);
        if (coins < price) return false;
        int currentCount = getItemCount(item.getId());
        return currentCount < getEffectiveLimit(item);
    }

    public boolean buy(ShopItem item) {
        if (!canBuy(item)) return false;
        int price = getEffectivePrice(item);
        int currentCountBefore = getItemCount(item.getId());
        int received = Math.min(item.getPackSize(), getEffectiveLimit(item) - currentCountBefore);
        coins -= price - price * (item.getPackSize() - received) / item.getPackSize();
        int currentCount = getItemCount(item.getId());
        int limit = getEffectiveLimit(item);
        int newCount = Math.min(limit, currentCount + item.getPackSize());
        inventory.put(item.getId(), newCount);
        return true;
    }

    public boolean canSell(ShopItem item) {
        if (item == null) return false;
        if (item == ShopItem.RADAR) return false;
        if (item == ShopItem.EX_CHASSIS) return false; // Không thể bán khung gầm vĩnh viễn
        int floor = shopOpeningInventory == null ? getItemCount(item.getId())
                : shopOpeningInventory.getOrDefault(item.getId(), 0);
        return isItemUnlocked(item) && getItemCount(item.getId()) - item.getPackSize() >= floor;
    }

    public boolean sell(ShopItem item) {
        if (!canSell(item)) return false;
        int price = getEffectivePrice(item);
        coins += price;
        int currentCount = getItemCount(item.getId());
        inventory.put(item.getId(), currentCount - item.getPackSize());
        return true;
    }

    public boolean canUpgradeResearch(ResearchTech tech) {
        if (tech == null) return false;
        int currentLevel = getResearchLevel(tech.getId());
        if (currentLevel >= tech.getMaxLevel()) return false;
        int price = tech.getPriceForLevel(currentLevel);
        return stars >= price;
    }

    public boolean upgradeResearch(ResearchTech tech) {
        if (!canUpgradeResearch(tech)) return false;
        int currentLevel = getResearchLevel(tech.getId());
        int price = tech.getPriceForLevel(currentLevel);
        stars -= price;
        research.put(tech.getId(), currentLevel + 1);
        return true;
    }

    public int getRepairTankCost(int missingHp) {
        if (missingHp <= 0) return 0;
        int unitPrice = getResearchLevel("armorResearch") >= 3 ? 3 : 6;
        return unitPrice * missingHp;
    }

    public boolean repairTank(int missingHp) {
        int cost = getRepairTankCost(missingHp);
        if (cost <= 0) return true;
        if (coins >= cost) {
            coins -= cost;
            return true;
        }
        return false;
    }

    public int getArmorHpBonus() {
        // Builds adds one HP point per armor plate; combat itself uses a 5 HP base scale.
        return getItemCount(ShopItem.ARMOR.getId())
                + getItemCount(ShopItem.ARMOR2.getId());
    }

    public float getSpeedMultiplier() {
        // Builds changes the tank hull sprite and adds 2 HP; it does not alter
        // the base movement speed. Nitro is the only speed modifier.
        return 1.0f;
    }

    public int getExtraInterestPercent() {
        int lvl = getResearchLevel("extraInterest");
        if (lvl == 1) return 5;
        if (lvl >= 2) return 10;
        return 0;
    }

    /** Builds reduces the shell reload counter by two ticks for levels 6..8. */
    public double getShellReloadSeconds() {
        int level = getResearchLevel("shellResearch");
        return Math.max(0.10, 0.75 - Math.max(0, level - 5) * (2.0 / 60.0));
    }

    public Map<Integer, Integer> getInventory() {
        return new HashMap<>(inventory);
    }

    public Map<String, Integer> getResearch() {
        return new HashMap<>(research);
    }

    /**
     * Nạp dữ liệu đồng bộ từ server.
     */
    public void loadState(int totalCoins, int totalStars, String researchJson, String inventoryJson) {
        if (totalCoins > 0) this.coins = totalCoins;
        if (totalStars > 0) this.stars = totalStars;
        parseResearch(researchJson);
        parseInventory(inventoryJson);
    }

    private void parseResearch(String json) {
        if (json == null || json.trim().isEmpty() || !json.contains("{")) return;
        String clean = json.replace("{", "").replace("}", "").replace("\"", "").trim();
        if (clean.isEmpty()) return;
        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    research.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void parseInventory(String json) {
        if (json == null || json.trim().isEmpty() || !json.contains("{")) return;
        String clean = json.replace("{", "").replace("}", "").replace("\"", "").trim();
        if (clean.isEmpty()) return;
        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    inventory.put(Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public String serializeResearch() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : research.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public String serializeInventory() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, Integer> e : inventory.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
