package com.tankzors.network.protocol;

import com.tankzors.entity.Direction;

/**
 * Message gửi từ client lên server biểu thị hành động của người chơi (input).
 *
 * <p>Ví dụ:
 * <pre>{@code
 *   {"type": "INPUT", "action": "MOVE", "direction": "UP", "moving": true, "timestamp": 1234567890}
 *   {"type": "INPUT", "action": "SHOOT", "timestamp": 1234567890}
 *   {"type": "JOIN_GAME", "playerName": "Player1", "levelId": "campaign_1", "timestamp": 1234567890}
 * }</pre>
 * </p>
 */
public class InputMessage extends GameMessage {

    private String action;          // "MOVE", "SHOOT", "JOIN_GAME", ...
    private Direction direction;    // UP, DOWN, LEFT, RIGHT
    private Boolean moving;         // true nếu đang bấm giữ phím di chuyển
    private String playerName;      // Tên người chơi khi tham gia
    private String levelId;         // ID bản đồ (ví dụ: campaign_1, online_1)
    private Object payload;         // Dữ liệu mở rộng tuỳ chọn nếu client gửi payload lồng nhau

    public InputMessage() {
        super("INPUT");
    }

    public InputMessage(String action) {
        super("INPUT");
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Boolean getMoving() {
        return moving;
    }

    public void setMoving(Boolean moving) {
        this.moving = moving;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "InputMessage{" +
                "type='" + type + '\'' +
                ", action='" + action + '\'' +
                ", direction=" + direction +
                ", moving=" + moving +
                ", playerName='" + playerName + '\'' +
                ", levelId='" + levelId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
