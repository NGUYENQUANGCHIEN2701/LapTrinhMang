package com.tankzors.client.gameplay;

/**
 * Listener nhận các sự kiện âm thanh / hiệu ứng từ simulation trận đấu.
 */
public interface GameEventListener {
    /** Xe tăng bắn đạn. */
    void onShoot(boolean isPlayer);

    /** Weapon-aware extension; existing listeners retain default-shell behavior. */
    default void onShoot(boolean isPlayer, int weaponType) { onShoot(isPlayer); }

    /** Đạn va chạm vào tường. */
    void onHitWall();

    /** Đạn trúng thân xe tăng. */
    void onHitTank(boolean isPlayer);

    /** Xe tăng bị phá hủy (hết HP). */
    void onTankDestroyed(boolean isPlayer);

    /** Trận đấu kết thúc (VICTORY hoặc DEFEAT). */
    void onMatchEnd(GameWorld.Status status);

    default void onPickup() {}
    default void onUseRepair() {}
    default void onUseSpeed() {}
    default void onUseFreeze() {}
}
