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

    public int hpRemain;
    public int aiKills;
    public int elapsedSeconds;
    public int starsEarned;
    public int coinsEarned;

    public void clearEntities() {
        tanks.clear();
        bullets.clear();
    }
}
