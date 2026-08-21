package com.tankzors.interfaces;

import com.tankzors.entity.Position;

/**
 * Interface đại diện cho một loại vũ khí.
 *
 * <p>Thiết kế theo Open/Closed Principle (OCP): khi muốn thêm vũ khí mới
 * (súng nhanh, súng mạnh, súng bắn xa...) chỉ cần tạo class mới implement
 * {@code IWeapon}, không cần sửa bất kỳ code nào đang dùng vũ khí cũ.</p>
 */
public interface IWeapon {

    /**
     * @return tên hiển thị của vũ khí.
     */
    String getName();

    /**
     * @return lượng sát thương mỗi viên đạn.
     */
    int getDamage();

    /**
     * @return thời gian hồi giữa 2 lần bắn (giây).
     */
    double getFireRate();

    /**
     * @return tốc độ đạn khi bắn ra (pixel/giây).
     */
    double getBulletSpeed();

    /**
     * Tạo một viên đạn mới từ vị trí và hướng cho trước.
     *
     * @param pos       vị trí xuất phát của viên đạn
     * @param direction hướng bay (độ)
     * @return viên đạn vừa tạo
     */
    IBullet createBullet(Position pos, double direction);
}