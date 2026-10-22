package com.tankzors.client.model;

import java.util.ArrayList;
import java.util.List;

/** Latest session state consumed by {@code GameRenderer}. */
public final class GameStateSnapshot {
    public String gameStatus = "WAITING";
    public String transitionStatus;
    public int score;
    public MapSnapshot map;
    public final List<BuildingSnapshot> buildings = new ArrayList<>();
    public final List<TankSnapshot> tanks = new ArrayList<>();
    public final List<BulletSnapshot> bullets = new ArrayList<>();
    public final List<EffectSnapshot> effects = new ArrayList<>();

    public int hpRemain;
    public int aiKills;
    public int elapsedSeconds;
    public int starsEarned;
    public int coinsEarned;

    // ===== Shop & Arsenal HUD fields =====
    public int playerCoins;
    public int playerStars;
    public int artilleryRange = 4;
    public boolean airAimActive;
    public int airAimCol, airAimRow;
    public int activeWeaponId;
    public int activeWeaponAmmo;
    public int repairCount;
    public int speedCount;
    public float speedBoostSecondsRemaining;
    public float freezeSecondsRemaining;
    public boolean radarActive;
    public boolean hasRadar;
    public boolean radarPaused;
    public int radarMenuSelection;
    public boolean radarSettingsOpen;
    public boolean radarShowObjects;
    public boolean inventoryMenuOpen;
    public int inventorySelectionIndex;
    public String inventorySelectedName = "";
    public String inventorySelectedDescription = "";
    public String inventorySelectedUsage = "";
    public int[] inventoryItemIds = new int[0];
    public int[] inventoryItemCounts = new int[0];

    // ===== Mission (kịch bản màn Builds) =====
    /** Dòng gợi ý từ trigger/thùng đồ; null khi không có. */
    public String hintText;
    /** Lời dặn đầu màn; hiển thị khi {@link #briefingOpen}. */
    public String briefing;
    public boolean briefingOpen;
    /** Lời tổng kết hiện cùng màn hình thắng. */
    public String debriefing;
    /** Xe địch còn phải hạ (sống + chờ sinh); -1 khi màn không đếm đợt. */
    public int enemiesRemaining = -1;
    public int enemiesTotal;

    public void clearEntities() {
        buildings.clear();
        tanks.clear();
        bullets.clear();
        effects.clear();
    }
}
