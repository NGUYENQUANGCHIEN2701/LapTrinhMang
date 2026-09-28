package com.tankzors.client.shop;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý trạng thái cửa hàng, túi đồ và cây công nghệ phía Client.
 * Chạy 100% client-authoritative theo kiến trúc dự án và chuẩn Tankzors Lux.
 */
public class PlayerShopState {

    private int coins = 0;
    private int stars = 0;
    private final Map<Integer, Integer> inventory = new HashMap<>();
    private final Map<String, Integer> research = new HashMap<>();
    private int activeWeaponId = 0; // Mặc định là Round (0)

    public PlayerShopState() {
        // Trang bị khởi đầu của một campaign mới.
        inventory.put(ShopItem.ROUND.getId(), 100);
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
        if (weaponId >= 0 && weaponId <= 5 && getItemCount(weaponId) > 0) {
            this.activeWeaponId = weaponId;
        }
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
        int price = item.getBasePrice();
        if (getResearchLevel("armorResearch") >= 3) {
            if (item == ShopItem.ARMOR || item == ShopItem.ARMOR2 || item == ShopItem.FORCE_FIELD) {
                // Giảm 30% giá theo công thức Tankzors Lux gốc
                price = price - (price * 30 / 100);
            }
        }
        return price;
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
        if (item == ShopItem.EX_CHASSIS) {
            return !hasExChassis();
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
        coins -= price;
        int currentCount = getItemCount(item.getId());
        int limit = getEffectiveLimit(item);
        int newCount = Math.min(limit, currentCount + item.getPackSize());
        inventory.put(item.getId(), newCount);
        return true;
    }

    public boolean canSell(ShopItem item) {
        if (item == null) return false;
        if (item == ShopItem.EX_CHASSIS) return false; // Không thể bán khung gầm vĩnh viễn
        return getItemCount(item.getId()) >= item.getPackSize();
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
        return getItemCount(ShopItem.ARMOR.getId()) * 20
                + getItemCount(ShopItem.ARMOR2.getId()) * 25;
    }

    public float getSpeedMultiplier() {
        return hasExChassis() ? 1.15f : 1.0f;
    }

    public int getExtraInterestPercent() {
        int lvl = getResearchLevel("extraInterest");
        if (lvl == 1) return 5;
        if (lvl >= 2) return 10;
        return 0;
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
