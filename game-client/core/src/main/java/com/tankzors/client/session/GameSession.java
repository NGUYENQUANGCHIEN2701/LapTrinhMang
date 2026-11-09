package com.tankzors.client.session;

import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.gameplay.result.MatchResult;

/**
 * Ranh giới một phiên gameplay (campaign local / host / guest).
 * Input từ ClientInputProcessor route vào đây thay vì thẳng socket.
 */
public interface GameSession {

    /** Input từ bàn phím. */
    void onMove(Direction direction, boolean moving);
    void onShoot();
    default boolean isRadarOpen() { return false; }
    default boolean isRadarPaused() { return false; }
    default void closeRadar() {}
    default void toggleRadarPause() {}
    default boolean isAiming() { return false; }
    void onAllReleased();
    default void onReverse(boolean reversing) {}

    /** Tiêu th rawDeltaTime (giây) từ render loop. */
    void update(float rawDeltaTime);

    /** Snapshot để GameRenderer vẽ. */
    GameStateSnapshot snapshot();

    /** ID tank người chơi trên máy này (cho HUD). */
    String localPlayerId();

    /** Trạng thái hiện tại (WAITING/PLAYING/VICTORY/DEFEAT). */
    String status();

    /** Chơi lại màn hiện tại (R). */
    void restart();

    /** Chuyển màn kế (N) nếu VICTORY. */
    void nextLevel();

    /**
     * Kích hoạt session (nạp map đầu tiên). Gọi sau khi Gdx đã ready
     * (trong {@code GameClient.create()}). Với session có sẵn map (host/guest)
     * có thể no-op.
     */
    default void start() {}

    /** Kết quả đã chốt, hoặc null nếu chưa kết thúc/chưa lưu. */
    MatchResult matchResult();

    void dispose();

    /** Có cần WebSocket không (campaign local = false; host/guest = true). */
    default boolean requiresSocket() { return false; }
    default boolean canLeave() { return true; }
    default void retrySave() {}

    default com.tankzors.client.shop.PlayerShopState getShopState() { return null; }
    default void cycleWeapon() {}
    default void selectWeapon(int weaponId) {}
    default boolean isInventoryMenuOpen() { return false; }
    default void openInventoryMenu() {}
    default void closeInventoryMenu() {}
    default void moveInventorySelection(int delta) {}
    default void selectInventoryRow(int relativeRow) {}
    default void confirmInventorySelection() {}
    /** Trong trận cần đứng trên ô hồi sinh xanh; chiến thắng được vào cửa hàng giữa màn. */
    default boolean canOpenShop() { return false; }
    /** Đã thắng và còn màn kế để Enter chuyển tiếp. */
    default boolean hasNextLevel() { return false; }
    /** Đóng màn hình briefing đầu màn để bắt đầu chơi. */
    default void dismissBriefing() {}
    default void setShopOpen(boolean open) {}
    default void onWorkshopItemBought(com.tankzors.client.shop.ShopItem item) {}
    default void onWorkshopItemSold(com.tankzors.client.shop.ShopItem item) {}
    default int workshopMissingHp() { return 0; }
    default boolean repairTankAtWorkshop() { return false; }
    default int workshopMissingAlliedHp() { return 0; }
    default boolean repairAlliesAtWorkshop() { return false; }
}
