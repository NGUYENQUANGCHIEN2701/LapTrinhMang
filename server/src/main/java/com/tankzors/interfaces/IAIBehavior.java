package com.tankzors.interfaces;

import com.tankzors.engine.GameState;
import com.tankzors.entity.AITank;

/**
 * Interface đại diện cho một hành vi (behavior) của AI.
 *
 * <p>Thiết kế theo Liskov Substitution Principle (LSP): mọi behavior
 * ({@code PatrolBehavior}, {@code ChaseBehavior}, {@code ShootBehavior}...)
 * đều thay thế được cho nhau. AIController có thể đổi behavior của một AITank
 * bất kỳ lúc nào mà không cần biết behavior cụ thể đang chạy.</p>
 */
public interface IAIBehavior {

    /**
     * Cập nhật hành vi AI mỗi tick.
     *
     * @param self      xe tăng AI đang thực thi hành vi này
     * @param state     trạng thái hiện tại của trận đấu
     * @param deltaTime thời gian đã trôi qua kể từ tick trước (giây)
     */
    void update(AITank self, GameState state, double deltaTime);

    /**
     * @return tên hành vi (dùng cho log/debug).
     */
    String getBehaviorName();
}