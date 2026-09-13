package com.tankzors.protocol;

/**
 * Action mà client gửi trong packet {@code JOIN_GAME}/{@code MOVE_INPUT}/
 * {@code SHOOT_INPUT} (v1).
 *
 * <p>Server cũ chấp nhận alias: {@code MOVE}/{@code MOVE_INPUT},
 * {@code SHOOT}/{@code SHOOT_INPUT}, {@code LEAVE_GAME}, {@code STOP}.
 * Client React hiện tại gửi dạng type = JOIN_GAME/MOVE_INPUT/SHOOT_INPUT
 * (không dùng field action). Khi做不到 sang server Spring Boot,
 * Spring handler phải chấp nhận cùng set alias như {@code MessageHandler.parseInput}
 * để React client chạy được mà không cần rebuild pipeline.</p>
 */
public enum InputAction {
    JOIN_GAME,
    MOVE,
    MOVE_INPUT,
    STOP,
    SHOOT,
    SHOOT_INPUT,
    LEAVE_GAME
}
