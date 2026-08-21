package com.tankzors.interfaces;

import com.tankzors.entity.Item;

/**
 * Interface Observer nhận thông báo về các sự kiện trong game.
 *
 * <p>Áp dụng Observer Pattern: GameEngine (subject) thông báo sự kiện cho mọi
 * {@code IGameObserver} đã đăng ký (achievement system, sound system, log...)
 * mà không cần biết chúng xử lý sự kiện như thế nào.</p>
 */
public interface IGameObserver {

    /**
     * Được gọi khi một viên đạn trúng một xe tăng.
     *
     * @param target xe tăng bị trúng đạn
     * @param bullet viên đạn gây sát thương
     */
    void onTankHit(ITank target, IBullet bullet);

    /**
     * Được gọi khi một xe tăng bị phá huỷ (HP về 0).
     *
     * @param tank xe tăng bị phá huỷ
     */
    void onTankDestroyed(ITank tank);

    /**
     * Được gọi khi trận đấu kết thúc.
     *
     * @param winnerId ID của xe tăng chiến thắng (null nếu hoà)
     */
    void onMatchEnd(String winnerId);

    /**
     * Được gọi khi một xe tăng nhặt được vật phẩm.
     *
     * @param tank xe tăng nhặt vật phẩm
     * @param item vật phẩm được nhặt
     */
    void onItemCollected(ITank tank, Item item);
}