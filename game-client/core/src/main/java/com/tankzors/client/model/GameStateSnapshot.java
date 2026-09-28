package com.tankzors.client.model;

import java.util.ArrayList;
import java.util.List;

/** Latest session state consumed by {@code GameRenderer}. */
public final class GameStateSnapshot {
    public String gameStatus = "WAITING";
    public String transitionStatus;
    public int score;
    public MapSnapshot map;
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
    public int activeWeaponId;
    public int activeWeaponAmmo;
    public int repairCount;
    public int speedCount;
    public float speedBoostSecondsRemaining;
    public boolean inventoryMenuOpen;
    public int inventorySelectionIndex;
    public int[] inventoryItemIds = new int[0];
    public int[] inventoryItemCounts = new int[0];

    public void clearEntities() {
        tanks.clear();
        bullets.clear();
        effects.clear();
    }
}
