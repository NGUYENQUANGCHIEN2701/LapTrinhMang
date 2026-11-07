package com.tankzors.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.transaction.annotation.Transactional;

import com.tankzors.leaderboard.LeaderboardEntry;
import com.tankzors.progress.PlayerProgressRepository;
import com.tankzors.progress.PlayerShopStateRepository;
import com.tankzors.progress.PlayerTotalsRepository;
import com.tankzors.leaderboard.LeaderboardRepository;
import com.tankzors.result.MatchResultEntity;
import com.tankzors.result.MatchResultRepository;
import com.tankzors.web.PageModelFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Dashboard — chỉ ROLE_ADMIN mới truy cập được (xem {@link SecurityConfig}).
 *
 * <p>Cung cấp dashboard quản lý:</p>
 * <ul>
 *   <li>Danh sách tất cả user (xem / enable / disable)</li>
 *   <li>Danh sách leaderboard toàn bộ</li>
 *   <li>Danh sách match results gần đây</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final UserRepository userRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final MatchResultRepository matchResultRepository;
    private final PageModelFactory pageModelFactory;
    private final PlayerProgressRepository progressRepository;
    private final PlayerTotalsRepository totalsRepository;
    private final PlayerShopStateRepository shopStateRepository;

    public AdminController(UserRepository userRepository,
                           LeaderboardRepository leaderboardRepository,
                           MatchResultRepository matchResultRepository,
                           PageModelFactory pageModelFactory,
                           PlayerProgressRepository progressRepository,
                           PlayerTotalsRepository totalsRepository,
                           PlayerShopStateRepository shopStateRepository) {
        this.userRepository = userRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.matchResultRepository = matchResultRepository;
        this.pageModelFactory = pageModelFactory;
        this.progressRepository = progressRepository;
        this.totalsRepository = totalsRepository;
        this.shopStateRepository = shopStateRepository;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        pageModelFactory.apply(model, "Admin Dashboard", "admin");

        // User stats
        List<UserEntity> users = userRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("adminCount",
            users.stream().filter(u -> "ROLE_ADMIN".equals(u.getRole())).count());
        model.addAttribute("userCount",
            users.stream().filter(u -> "ROLE_USER".equals(u.getRole())).count());

        // Users as rows
        List<Map<String, Object>> userRows = new ArrayList<>();
        for (UserEntity u : users) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("displayName", u.getDisplayName());
            row.put("email", u.getEmail());
            row.put("role", u.getRole());
            row.put("enabled", u.isEnabled());
            row.put("createdAt", u.getCreatedAt() == null ? "" : DT_FMT.format(u.getCreatedAt()));
            userRows.add(row);
        }
        model.addAttribute("users", userRows);

        // Leaderboard data
        List<LeaderboardEntry> allScores = leaderboardRepository.findAll();
        allScores.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        model.addAttribute("totalScores", allScores.size());
        model.addAttribute("leaderboardEntries", toLeaderboardRows(allScores));

        // Match results
        List<MatchResultEntity> matches = matchResultRepository.findAll();
        matches.sort((a, b) -> {
            if (a.getEndedAt() == null) return 1;
            if (b.getEndedAt() == null) return -1;
            return b.getEndedAt().compareTo(a.getEndedAt());
        });
        // Limit to latest 100
        if (matches.size() > 100) matches = matches.subList(0, 100);
        model.addAttribute("totalMatches", matchResultRepository.count());
        model.addAttribute("matchResults", toMatchRows(matches));

        return "admin";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("flashError", "Không tìm thấy user id=" + id);
            return "redirect:/admin";
        }
        if ("ROLE_ADMIN".equals(user.getRole())) {
            redirectAttributes.addFlashAttribute("flashError",
                "Không thể vô hiệu hóa tài khoản Admin.");
            return "redirect:/admin";
        }
        user.setEnabled(!user.isEnabled());
        user.setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);
        logger.info("[AdminController] Toggle user {} -> enabled={}", user.getUsername(), user.isEnabled());
        redirectAttributes.addFlashAttribute("flashSuccess",
            "Đã " + (user.isEnabled() ? "kích hoạt" : "vô hiệu hóa") + " user: " + user.getUsername());
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/delete")
    @Transactional
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("flashError", "Không tìm thấy user id=" + id);
            return "redirect:/admin";
        }
        if ("ROLE_ADMIN".equals(user.getRole())) {
            redirectAttributes.addFlashAttribute("flashError",
                "Không thể xóa tài khoản Admin.");
            return "redirect:/admin";
        }
        String username = user.getUsername();
        // Dữ liệu chơi khoá theo tên tài khoản: xoá cùng user, nếu không người đăng ký
        // lại đúng tên này sẽ thừa kế tiến trình, ví và thứ hạng của người cũ.
        String playerId = "user:" + username;
        progressRepository.deleteBySessionId(playerId);
        totalsRepository.findByPlayerId(playerId).ifPresent(totalsRepository::delete);
        shopStateRepository.findByPlayerId(playerId).ifPresent(shopStateRepository::delete);
        leaderboardRepository.deleteByPlayerName(username);
        userRepository.delete(user);
        logger.info("[AdminController] Deleted user: {}", username);
        redirectAttributes.addFlashAttribute("flashSuccess",
            "Đã xóa user: " + username);
        return "redirect:/admin";
    }

    // ===== Helper methods =====

    private static List<Map<String, Object>> toLeaderboardRows(List<LeaderboardEntry> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int limit = Math.min(entries.size(), 100);
        for (int i = 0; i < limit; i++) {
            LeaderboardEntry e = entries.get(i);
            Map<String, Object> row = new HashMap<>();
            row.put("rank", i + 1);
            row.put("playerName", e.getPlayerName());
            row.put("mapId", e.getMapId());
            row.put("score", e.getScore());
            row.put("createdAt", e.getCreatedAt() == null ? "" : DT_FMT.format(e.getCreatedAt()));
            rows.add(row);
        }
        return rows;
    }

    private static List<Map<String, Object>> toMatchRows(List<MatchResultEntity> matches) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MatchResultEntity m : matches) {
            Map<String, Object> row = new HashMap<>();
            row.put("runId", m.getRunId());
            row.put("playerName", m.getPlayerName());
            row.put("mapId", m.getMapId());
            row.put("mode", m.getMode());
            row.put("status", m.getStatus());
            row.put("score", m.getScore());
            row.put("endedAt", m.getEndedAt() == null ? "" : DT_FMT.format(m.getEndedAt()));
            rows.add(row);
        }
        return rows;
    }
}
