package com.tankzors.interfaces;

import com.tankzors.entity.Position;

/**
 * Interface đại diện cho một viên đạn trong game.
 *
 * <p>Mọi loại đạn ({@code NormalBullet}, đạn nhanh, đạn to...) đều implement
 * interface này. BulletManager chỉ thao tác với {@code IBullet} nên có thể
 * thêm loại đạn mới mà không sửa code quản lý (Open/Closed Principle - OCP).</p>
 */
public interface IBullet {

    /**
     * Cập nhật vị trí viên đạn mỗi tick.
     *
     * @param deltaTime thời gian đã trôi qua kể từ tick trước (giây)
     */
    void update(double deltaTime);

    /**
     * @return vị trí hiện tại của viên đạn.
     */
    Position getPosition();

    /**
     * @return hướng bay hiện tại của viên đạn (độ).
     */
    double getDirection();

    /**
     * @return lượng sát thương viên đạn gây ra khi trúng mục tiêu.
     */
    int getDamage();

    /**
     * @return tốc độ bay (pixel/giây).
     */
    double getSpeed();

    /**
     * @return {@code true} nếu viên đạn còn hoạt động trên sân.
     */
    boolean isActive();

    /**
     * Vô hiệu hoá viên đạn (sẽ bị xoá ở tick kế tiếp).
     */
    void deactivate();

    /**
     * @return ID của xe tăng đã bắn viên đạn này.
     */
    String getOwnerId();
}