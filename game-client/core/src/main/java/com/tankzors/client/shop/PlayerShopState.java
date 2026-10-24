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
            case ROUND: return "Đạn thường: 1 HP. Giá theo 10 viên. Trừ độ bền tường gạch; hết lực thì dừng.";
            case PLASMA: return "Plasma: 1 HP, bay nhanh; nạp nhanh hơn đạn thường 10 nhịp. Giá theo 10 viên.";
            case DOUBLE_ROUND: return "Đạn đôi: 2 HP; còn lực thì bay tiếp sau khi phá gạch/bụi hoặc chặn đạn. Giá theo 10 viên.";
            case DOUBLE_PLASMA: return "Plasma đôi: 2 HP, nạp nhanh hơn 10 nhịp; còn lực thì bay tiếp. Giá theo 10 viên.";
            case ROUND2: return "Xuyên giáp: 3 HP; mất lực theo độ bền gạch/bụi và đạn bị chặn, còn lực thì bay tiếp. Giá theo 10 viên.";
            case ROUND3: return "Xuyên giáp cường hóa: 5 HP; phá gạch/bụi rồi bay tiếp nếu còn lực. Giá theo 10 viên.";
            case ARTILLERY:
                return "Pháo cầu vồng: lực " + com.tankzors.client.gameplay.SpecialAmmoRules.artilleryPower(level)
                        + ", tầm " + com.tankzors.client.gameplay.SpecialAmmoRules.artilleryDistance(level)
                        + " ô, vùng nổ " + (com.tankzors.client.gameplay.SpecialAmmoRules.artilleryRadius(level) * 2 - 1) + " ô.";
            case WARHEAD: return "Tăng tốc đạn; chạm xe/tường thì nổ. Lực 9, vùng 3 ô; giảm 1 mỗi ô. Không phá bê tông/thép.";
            case ROCKET: return "WASD lái tên lửa; bắn lần nữa để nổ. Lực 32, vùng 5 ô. Phá ô bê tông va chạm, không phá thép.";
            case MINE: return "Đặt dưới xe; chỉ nổ khi địch đè lên, trừ 5 HP.";
            case MINE2: return "Đặt dưới xe; chỉ nổ khi địch đè lên, trừ 9 HP.";
            case DYNAMITE: return "Ngòi 3 giây; lực 7, vùng 5 ô. Cẩn thận tự sát thương.";
            case DYNAMITE2: return "Ngòi 3 giây; lực 10, vùng 5 ô. Phá đá.";
            case DYNAMITE3: return "Ngòi 3 giây; lực 19, vùng 7 ô. Phá đá.";
            case AIR_SUPPORT:
                int air = getResearchLevel("airRaidResearch");
                return "Cần Radar; chọn mục tiêu. " + com.tankzors.client.gameplay.SpecialAmmoRules.airBombs(air)
                        + " bom, lực " + com.tankzors.client.gameplay.SpecialAmmoRules.airPower(air)
                        + ", vùng " + (com.tankzors.client.gameplay.SpecialAmmoRules.airRadius(air) * 2 - 1) + " ô.";
            case ARMOR:
                int armorMax = getResearchLevel("armorResearch") >= 3 ? 5 : 4;
                return "Giáp tạm thời (+1 HP). Mất 1 tấm khi xe bị tiêu diệt. Tối đa " + armorMax + " HP.";
            case ARMOR2:
                int permMax = getResearchLevel("armorResearch") >= 3 ? 5 : 4;
                return "Giáp vĩnh cửu (+1 HP). Nâng cấp độ bền xe vĩnh viễn. Tối đa " + permMax + " HP.";
            case FORCE_FIELD:
                int shieldMax = getResearchLevel("powerFieldResearch") >= 4 ? 7 : 5;
                return "Khiên năng lượng: tự động tái tạo sau 5 giây không dính đạn. Tối đa " + shieldMax + " HP.";
            case REPAIR:
                return "Túi cứu thương: dùng trong trận (phím Q) để hồi đầy HP ngay lập tức.";
            case SPEED_UP:
                int speedSec = hasExChassis() ? 60 : 45;
                return "Tăng tốc độ di chuyển và giảm hồi nòng súng trong " + speedSec + " giây.";
            case FREEZE:
                int freezeSec = hasExChassis() ? 10 : 5;
                return "Đóng băng toàn bộ xe tăng, tháp pháo và đạn của địch trong " + freezeSec + " giây.";
            case EX_CHASSIS:
                return "Khung gầm tăng nặng: +2 HP, mở rộng sức chứa đạn đặc biệt lên 99 viên, tăng thời gian Nitro (+15 giây) và đóng băng (+5 giây).";
            case ADD_LIFE:
                return "Thêm 1 lượt hồi sinh ngay tại trận khi xe tăng bị tiêu diệt.";
            case RADAR:
                return "Mở bản đồ nhỏ tổng quan trận đấu (phím Q); cho phép mua và gọi không kích.";
            case ALLY_TANK: return "Xe đồng minh hạng nhẹ: 8 HP, hỗ trợ tấn công và đi theo người chơi.";
            case ALLY_TANK2: return "Xe đồng minh hạng nhẹ nâng cấp: 15 HP, hỗ trợ tấn công và đi theo người chơi.";
            case ALLY_TANK3: return "Xe đồng minh hạng nặng: 25 HP, hỗ trợ tấn công và đi theo người chơi.";
            default: return item.getDescription();
        }
    }

    /** Instructions for the selected item in the paused Q inventory. */
    public String getInventoryUsage(ShopItem item) {
        switch (item) {
            case ROUND: case PLASMA: case DOUBLE_ROUND: case DOUBLE_PLASMA:
            case ROUND2: case ROUND3:
                return "Enter: trang bị. Space / J: bắn theo hướng xe.\n"
                        + "Đạn còn sát thương sau khi phá gạch/bụi sẽ tiếp tục bay. Không phá bê tông/thép.";
            case ARTILLERY:
                return "Enter: trang bị. Space / J: bắn qua vật cản đến đúng tầm.\n"
                        + "Vụ nổ giảm 1 lực mỗi ô; gây sát thương cả xe của bạn. Không phá bê tông/thép.";
            case WARHEAD:
                return "Enter: trang bị. Space / J: phóng đầu đạn, nổ khi chạm xe địch hoặc tường.\n"
                        + "Vụ nổ giảm 1 lực mỗi ô và có thể gây sát thương xe của bạn.";
            case ROCKET:
                return "Enter: trang bị. Space / J: phóng; WASD / mũi tên: lái tên lửa.\n"
                        + "Bắn lần nữa để kích nổ. Xe đứng yên khi điều khiển tên lửa. Cẩn thận vụ nổ gần xe.";
            case MINE: case MINE2:
                return "Enter: trang bị. Space / J: đặt trên ô trống dưới xe.\n"
                        + "Chỉ kích hoạt khi xe địch đè lên; an toàn với xe của bạn và đồng minh.";
            case DYNAMITE: case DYNAMITE2: case DYNAMITE3:
                return "Enter: trang bị. Space / J: đặt trên ô trống dưới xe. Rời xa trước 3 giây!\n"
                        + "Vụ nổ giảm 1 lực mỗi ô, phá gạch/bụi/khối đá và gây sát thương cả xe của bạn.";
            case AIR_SUPPORT:
                return "Enter: trang bị. Space / J: mở ngắm; WASD / mũi tên: chọn ô.\n"
                        + "Space / Enter: gọi máy bay. Q: hủy ngắm. Cẩn thận bom trúng xe của bạn.";
            case REPAIR:
                return "Enter: dùng ngay để hồi đầy HP của xe. Không tiêu hao khi xe đã đầy HP.";
            case SPEED_UP:
                return "Enter: dùng ngay để tăng tốc di chuyển và nạp đạn. Thời gian còn lại hiện trên màn hình.";
            case FREEZE:
                return "Enter: dùng ngay để đóng băng xe, tháp pháo và đạn của địch trong một khoảng thời gian.";
            case FORCE_FIELD:
                return "Enter: lắp các miếng khiên trong túi đến giới hạn. Khiên tự hồi phục sau khi ngừng bị tấn công.";
            case ALLY_TANK: case ALLY_TANK2: case ALLY_TANK3:
                return "Enter: gọi xe đồng minh tại căn cứ xuất phát. Xe tự tìm địch và bám theo bạn.\n"
                        + "Hãy đi ra khỏi căn cứ trước khi gọi. Căn cứ bị chiếm chỗ thì không mất vật phẩm.";
            case RADAR:
                return "Enter: mở bản đồ tổng quan. Radar được giữ lại sau khi dùng.";
            default: return "Enter: chọn vật phẩm.";
        }
    }

    public String getUnlockHint(ShopItem item) {
        if (item == ShopItem.EX_CHASSIS && hasExChassis()) {
            return "[ĐÃ SỞ HỮU]";
        }
        if (item == ShopItem.RADAR && hasRadar()) {
            return "[ĐÃ SỞ HỮU]";
        }
        if (item.getRequiredTech() != null && getResearchLevel(item.getRequiredTech()) < item.getRequiredTechLevel()) {
            ResearchTech tech = ResearchTech.findById(item.getRequiredTech());
            return "Cần " + (tech == null ? item.getRequiredTech() : tech.getDisplayName()) + " cấp " + item.getRequiredTechLevel();
        }
        if (item == ShopItem.AIR_SUPPORT && !hasRadar()) return "Cần mua Radar";
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
