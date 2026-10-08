package com.tankzors.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tankzors.application.PlayerProgressService;

import com.tankzors.identity.PlayerIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST endpoint cho progression phía client query.
 *
 * <p>Danh tính người chơi lấy qua {@link PlayerIdentityResolver}: đã đăng nhập
 * thì là {@code user:<tên>}, giống API bootstrap/result nơi màn được mở khoá.</p>
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final PlayerProgressService progressService;
    private final PlayerIdentityResolver identity;

    public ProgressController(PlayerProgressService progressService, PlayerIdentityResolver identity) {
        this.progressService = progressService;
        this.identity = identity;
    }

    /**
     * Lấy danh sách màn đã unlock cho session hiện tại.
     * Trả về dạng JSON cho website hoặc LibGDX client.
     */
    @GetMapping
    public Map<String, Object> getProgress(HttpServletRequest request) {
        String sid = identity.resolve(request);
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
        String sid = identity.resolve(request);
        progressService.reset(sid);
        return Map.of("status", "OK", "unlockedLevels", progressService.getUnlocked(sid));
    }
}
