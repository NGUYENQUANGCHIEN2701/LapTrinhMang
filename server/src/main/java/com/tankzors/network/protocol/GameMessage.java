package com.tankzors.network.protocol;

/**
 * Lớp cơ sở (base class) cho mọi message truyền nhận qua WebSocket.
 *
 * <p>Mỗi message có một trường {@code type} định danh loại gói tin
 * và {@code timestamp} ghi nhận thời điểm tạo message.</p>
 */
public abstract class GameMessage {

    protected String type;
    protected long timestamp;

    public GameMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public GameMessage(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public GameMessage(String type, long timestamp) {
        this.type = type;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
