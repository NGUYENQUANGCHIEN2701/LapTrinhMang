package com.tankzors.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.tankzors.application.PlayerProgressService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * REST endpoint cho progression phía client query.
 *
 * <p>Cần WebSocket sessionId của player (được client submit trong login flow).
 * Tạm thời dùng HttpSession ID làm surrogate. Khi LibGDX HTML5 rendelkezja
 * cookie JSESSIONID sẽ tự đồng nhất.</p>
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final PlayerProgressService progressService;

    public ProgressController(PlayerProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * Lấy danh sách màn đã unlock cho session hiện tại.
     * Trả về dạng JSON để React/LibGDX client render.
     */
    @GetMapping
    public Map<String, Object> getProgress(HttpServletRequest request) {
        String sid = resolveSid(request);
        List<String> unlocked = progressService.getUnlocked(sid);
        return Map.of(
            "sessionId", sid,
            "unlockedLevels", unlocked
        );
    }

    /**
     * Founder mode: reset tiến trình về mặc định (chỉ campaign_1).
     */
    @PostMapping("/reset")
    public Map<String, Object> reset(HttpServletRequest request) {
        String sid = resolveSid(request);
        progressService.reset(sid);
        return Map.of("status", "OK", "unlockedLevels", progressService.getUnlocked(sid));
    }

    /**
     * Lấy SID dùng để tracking. Ưu tiên header X-Player-Session để React
     * client gửi WS sessionId (từ CONNECT packet), fallback HttpSession.
     */
    private static String resolveSid(HttpServletRequest request) {
        String sid = request.getHeader("X-Player-Session");
        if (sid != null && !sid.isBlank()) return sid;
        HttpSession session = request.getSession(false);
        if (session != null) return session.getId();
        // Auto-create nếu chưa có
        return request.getSession(true).getId();
    }
}
