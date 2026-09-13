package com.tankzors.protocol;

/**
 * Base packet v1: mọi packet trên wire đều có {@code type} và
 * {@code timestamp} (epoch millis).
 *
 * <p>Tương đương {@code BasePacket} của {@code client/src/core/network/types.ts}
 * và {@code GameMessage} của server旧的.</p>
 */
public class BasePacket {
    private String type;
    private long timestamp;

    public BasePacket() {
    }

    public BasePacket(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
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
