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
    void onAllReleased();

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
    boolean requiresSocket();

    default com.tankzors.client.shop.PlayerShopState getShopState() { return null; }
    default void cycleWeapon() {}
    default void selectWeapon(int weaponId) {}
}
