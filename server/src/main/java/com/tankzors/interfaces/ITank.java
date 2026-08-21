package com.tankzors.interfaces;

import com.tankzors.entity.Direction;
import com.tankzors.entity.Position;

/**
 * Interface đại diện cho một xe tăng trong game.
 *
 * <p>Đây là hợp đồng (contract) cho mọi loại xe tăng: {@code PlayerTank} do
 * người chơi điều khiển, {@code AITank} do máy điều khiển. GameEngine chỉ làm
 * việc với {@code ITank} mà không cần biết class cụ thể (Dependency Inversion
 * Principle - DIP).</p>
 */
public interface ITank {

    /**
     * @return ID duy nhất của xe tăng.
     */
    String getId();

    /**
     * Di chuyển xe tăng theo một hướng trong khoảng thời gian deltaTime.
     *
     * @param direction hướng di chuyển
     * @param deltaTime thời gian đã trôi qua kể từ tick trước (giây)
     */
    void move(Direction direction, double deltaTime);

    /**
     * Xoay xe tăng một góc (độ).
     *
     * @param angle góc xoay mới (độ)
     */
    void rotate(double angle);

    /**
     * @return vị trí hiện tại của xe tăng.
     */
    Position getPosition();

    /**
     * @return góc xoay hiện tại (độ).
     */
    double getRotation();

    /**
     * @return máu (HP) hiện tại.
     */
    int getHP();

    /**
     * @return máu tối đa.
     */
    int getMaxHP();

    /**
     * Nhận sát thương từ một viên đạn hoặc nguồn khác.
     *
     * @param damage lượng sát thương nhận vào
     */
    void takeDamage(int damage);

    /**
     * @return {@code true} nếu xe tăng còn sống (HP &gt; 0).
     */
    boolean isAlive();

    /**
     * Bắn một viên đạn từ vị trí và hướng hiện tại của xe tăng.
     *
     * @return viên đạn vừa được bắn ra
     */
    IBullet shoot();
}