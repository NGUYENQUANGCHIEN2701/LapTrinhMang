package com.tankzors.protocol;

/**
 * Packet {@code MOVE_INPUT} (client -> server) — v1 mục 3.3.
 *
 * <p>{@code moving=false} -> ngừng direction đó; nếu direction=null cùng
 * {@code STOP} -> ngừng toàn bộ (giống {@code PlayerTank.stopMoving()}).</p>
 */
public class MoveInputPacket extends BasePacket {
    private DirectionDto direction;
    private Boolean moving;

    public MoveInputPacket() {
        super(PacketType.MOVE_INPUT.wireName());
    }

    public MoveInputPacket(DirectionDto direction, Boolean moving) {
        super(PacketType.MOVE_INPUT.wireName());
        this.direction = direction;
        this.moving = moving;
    }

    public DirectionDto getDirection() {
        return direction;
    }

    public void setDirection(DirectionDto direction) {
        this.direction = direction;
    }

    public Boolean getMoving() {
        return moving;
    }

    public void setMoving(Boolean moving) {
        this.moving = moving;
    }
}
