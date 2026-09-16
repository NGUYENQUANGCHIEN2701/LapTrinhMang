package com.tankzors.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Resolve player identity thống nhất cho page / REST API / WS handshake.
 *
 * <p>MVP: dùng HttpSession hiện tại làm identity (không login mới trong đợt này).
 * Ưu tiên header {@code X-Player-Session} nếu client gửi (surrogate cho client
 * không có cookie JSESSIONID), fallback HttpSession.</p>
 *
 * <p>WS sessionId chỉ là connectionId — handshake gán connectionId -> playerId.
 * Không dùng WS sessionId làm khóa progression.</p>
 */
@Component
public class PlayerIdentityResolver {

    /** Key lưu playerId trong HttpSession. */
    public static final String SESSION_ATTR = "tankzors.playerId";

    /**
     * Resolve playerId cho request HTTP/REST hiện tại. Nếu chưa có session/identity,
     * tự tạo (HttpSession(true)) và phát id mới.
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) return "anon";
        // 1. Header surrogate (client chủ động gửi).
        String header = request.getHeader("X-Player-Session");
        if (header != null && !header.isBlank()) return header.trim();
        // 2. Thuộc tính trong HttpSession (identity ứng dụng).
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(SESSION_ATTR);
            if (attr instanceof String s && !s.isBlank()) return s;
        }
        // 3. Fallback: tạo session mới + phát id ổn định cho phiên.
        HttpSession newSession = request.getSession(true);
        String sid = newSession.getId();
        newSession.setAttribute(SESSION_ATTR, sid);
        return sid;
    }

    /**
     * Resolve KHÔNG tự tạo session (cho WS handshake chỉ đọc).
     * Trả về id từ header hoặc thuộc tính session hiện có; null nếu chưa có.
     */
    public String resolveExisting(HttpServletRequest request) {
        if (request == null) return null;
        String header = request.getHeader("X-Player-Session");
        if (header != null && !header.isBlank()) return header.trim();
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(SESSION_ATTR);
            if (attr instanceof String s && !s.isBlank()) return s;
            return session.getId();
        }
        return null;
    }
}
