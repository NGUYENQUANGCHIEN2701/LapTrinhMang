package com.tankzors.engine;

/**
 * Output port: cổngtout cho {@link GameLoop} phát state mà không biết
 * chi tiết transport (Spring WebSocket hay Javalin).
 *
 * <p>Dependency rule: {@code engine} KHÔNG được phụ thuộc package
 * {@code network}, vì vậy ta đặt interface này ngay trong engine
 * để inversion-of-control. Implementation nằm ở {@code network}.</p>
 *
 * <p>Phase 2: thay cho {@code com.tankzors.network.MessageSender} của
 * server Javalin cũ. Tránh refactor {@code network.protocol.StateMessage}
 * sang shared-protocol trong phase này - ta sẽ giữ {@code StateMessage}
 * để không phá 112 server test của module legacy.</p>
 */
public interface StatePublisherPort {
    /**
     * Broadcast một object (thường là StateMessage) tới toàn bộ client đang kết nối.
     *
     * @param message payload đã được serialize sẵn của server
     */
    void broadcast(Object message);
}
