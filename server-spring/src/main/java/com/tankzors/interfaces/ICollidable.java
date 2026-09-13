package com.tankzors.interfaces;

import com.tankzors.entity.Rectangle;

/**
 * Interface đại diện cho mọi đối tượng có thể va chạm trong game
 * (xe tăng, tường, đạn, vật phẩm...).
 *
 * <p>CollisionManager chỉ làm việc với {@code ICollidable} và kiểm tra va chạm
 * AABB (Axis-Aligned Bounding Box) nên mọi đối tượng mới muốn tham gia hệ
 * thống va chạm chỉ cần implement interface này.</p>
 */
public interface ICollidable {

    /**
     * @return hình chữ nhật bao quanh đối tượng (dùng cho kiểm tra AABB).
     */
    Rectangle getBoundingBox();

    /**
     * Kiểm tra đối tượng này có va chạm với đối tượng khác không.
     *
     * @param other đối tượng cần kiểm tra va chạm
     * @return {@code true} nếu 2 bounding box giao nhau
     */
    boolean collidesWith(ICollidable other);
}