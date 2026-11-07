package com.tankzors.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Resolve player identity thống nhất cho page / REST API / WS handshake.
 *
 * <p>Khi đã đăng nhập, username là khóa bền vững của tài khoản. Session ẩn danh
 * chỉ là fallback (profile test tắt bảo mật). Không nhận identity từ header do client
 * tự gửi: ai cũng giả được.</p>
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
        // 1. Tài khoản đã đăng nhập phải luôn thắng header/session để dữ liệu
        // campaign không đổi khi JSESSIONID được tạo lại.
        Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return "user:" + principal.getName();
        }
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
}
