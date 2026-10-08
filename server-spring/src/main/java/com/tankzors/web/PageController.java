package com.tankzors.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tankzors.application.CampaignLevelView;
import com.tankzors.application.CampaignQueryService;
import com.tankzors.application.LeaderboardService;
import com.tankzors.application.PlayerProgressService;
import com.tankzors.leaderboard.LeaderboardEntry;

import com.tankzors.identity.PlayerIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Thymeleaf page controller for the website shell.
 *
 * <p>Routes:</p>
 * <ul>
 *   <li>{@code GET /, /menu} -> Main menu</li>
 *   <li>{@code GET /game-menu} -> Chọn chế độ chơi</li>
 *   <li>{@code GET /campaigns} -> Campaign level selection (unlock state server-side)</li>
 *   <li>{@code GET /load-game} -> Save placeholder</li>
 *   <li>{@code GET/POST /settings} -> Settings (session-based)</li>
 *   <li>{@code GET /leaderboard?mapId=...} -> Leaderboard page</li>
 *   <li>{@code GET /play?levelId=...} -> Play page (LibGDX placeholder)</li>
 *   <li>{@code GET /error/404} -> Not found page</li>
 * </ul>
 *
 * <p>Controller chỉ đọc parameter, gọi application service và đưa dữ liệu
 * đơn giản vào Model. Gameplay, collision và AI chạy trong LibGDX client.</p>
 *
 * <p>REST endpoints ({@code /api/*}) nằm ở {@link HealthController},
 * {@link LeaderboardController}, {@link ProgressController}.</p>
 */
@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    static final String DEFAULT_LEVEL = "campaign_1";
    static final List<String> LEADERBOARD_MAPS = List.of("campaign_1", "campaign_2", "campaign_3");

    private static final DateTimeFormatter CREATED_AT_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final CampaignQueryService campaignQueryService;
    private final LeaderboardService leaderboardService;
    private final PlayerProgressService progressService;
    private final SettingsService settingsService;
    private final PageModelFactory pageModelFactory;
    private final PlayerIdentityResolver identity;

    public PageController(CampaignQueryService campaignQueryService,
                          LeaderboardService leaderboardService,
                          PlayerProgressService progressService,
                          SettingsService settingsService,
                          PageModelFactory pageModelFactory,
                          PlayerIdentityResolver identity) {
        this.campaignQueryService = campaignQueryService;
        this.leaderboardService = leaderboardService;
        this.progressService = progressService;
        this.settingsService = settingsService;
        this.pageModelFactory = pageModelFactory;
        this.identity = identity;
    }

    @GetMapping({"/", "/menu"})
    public String menu(Model model) {
        pageModelFactory.apply(model, "Main Menu", "menu");
        return "menu";
    }

    @GetMapping("/game-menu")
    public String gameMenu(Model model) {
        pageModelFactory.apply(model, "Game Menu", "game-menu");
        return "game-menu";
    }

    @GetMapping("/campaigns")
    public String campaigns(Model model, HttpServletRequest request) {
        pageModelFactory.apply(model, "Campaigns", "campaigns");
        model.addAttribute("levels", campaignQueryService.getAvailableLevels());
        model.addAttribute("unlockedLevels", resolveUnlockedLevels(request));
        return "campaigns";
    }

    /** Alias cũ -> route contract mới. */
    @GetMapping("/campaign")
    public String campaignAlias() {
        return "redirect:/campaigns";
    }

    @GetMapping("/load-game")
    public String loadGame(Model model) {
        pageModelFactory.apply(model, "Load Game", "load-game");
        return "load-game";
    }

    @GetMapping("/settings")
    public String settingsForm(Model model, HttpServletRequest request) {
        pageModelFactory.apply(model, "Settings", "settings");
        model.addAttribute("settings", settingsService.load(request.getSession()));
        return "settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
        @RequestParam(name = "musicVolume", required = false) String musicVolume,
        @RequestParam(name = "soundVolume", required = false) String soundVolume,
        @RequestParam(name = "language", required = false) String language,
        @RequestParam(name = "showFps", required = false, defaultValue = "false") boolean showFps,
        @RequestParam(name = "gameScale", required = false) String gameScale,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) {
        GameSettings settings = GameSettings.fromRequest(
            GameSettings.parseIntOrNull(musicVolume),
            GameSettings.parseIntOrNull(soundVolume),
            language,
            showFps,
            GameSettings.parseIntOrNull(gameScale)
        );
        settingsService.save(request.getSession(), settings);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã lưu cài đặt.");
        return "redirect:/settings";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(
        @RequestParam(name = "mapId", required = false) String mapId,
        Model model
    ) {
        pageModelFactory.apply(model, "Leaderboard", "leaderboard");
        String safeMapId = mapId != null && LEADERBOARD_MAPS.contains(mapId)
            ? mapId : DEFAULT_LEVEL;
        model.addAttribute("maps", LEADERBOARD_MAPS);
        model.addAttribute("currentMap", safeMapId);
        try {
            model.addAttribute("leaderboardEntries",
                toRows(leaderboardService.topScores(safeMapId)));
        } catch (Exception ex) {
            logger.warn("[PageController] leaderboard unavailable for map={}: {}",
                safeMapId, ex.getMessage());
            model.addAttribute("leaderboardEntries", List.of());
            model.addAttribute("errorMessage",
                "Bảng xếp hạng tạm thời không truy cập được (database chưa sẵn sàng).");
        }
        return "leaderboard";
    }

    @GetMapping("/play")
    public String play(
        @RequestParam(name = "levelId", defaultValue = DEFAULT_LEVEL) String levelId,
        Model model,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes
    ) {
        CampaignLevelView level = campaignQueryService.getLevel(levelId).orElse(null);
        if (level == null) {
            logger.warn("[PageController] unknown levelId requested: {}", levelId);
            redirectAttributes.addFlashAttribute("flashError",
                "Không tìm thấy level: " + levelId);
            return "redirect:/campaigns";
        }
        pageModelFactory.apply(model, "Play - " + level.getName(), "play");
        model.addAttribute("selectedLevelId", level.getId());
        model.addAttribute("levelName", level.getName());
        model.addAttribute("settings", settingsService.load(request.getSession()));
        return "play";
    }

    @GetMapping("/error/404")
    public String error404(Model model) {
        pageModelFactory.apply(model, "404", "error");
        return "error/404";
    }

    @PostMapping("/api/leaderboard/submit-from-client")
    @ResponseBody
    public Map<String, Object> submitScoreFromClient(@RequestParam Map<String, String> body) {
        String playerName = body.getOrDefault("playerName", "Anonymous");
        String mapId = body.getOrDefault("mapId", "campaign_1");
        int score = Integer.parseInt(body.getOrDefault("score", "0"));
        int timeSeconds = Integer.parseInt(body.getOrDefault("timeSeconds", "0"));
        int hpRemain = Integer.parseInt(body.getOrDefault("hpRemain", "0"));
        LeaderboardEntry saved = leaderboardService.submitScore(playerName, mapId, score, timeSeconds, hpRemain);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "OK");
        resp.put("bestScore", saved.getScore());
        return resp;
    }

    /**
     * Unlock state từ {@link PlayerProgressService}; nếu database chưa chạy
     * thì degrade nhẹ nhàng (mở toàn bộ level) thay vì crash page.
     */
    private List<String> resolveUnlockedLevels(HttpServletRequest request) {
        try {
            // Cùng khoá người chơi với API bootstrap/result (user:<tên> khi đã đăng nhập),
            // nếu không trang chọn màn sẽ đọc tiến trình của session ẩn danh.
            return progressService.getUnlocked(identity.resolve(request));
        } catch (Exception ex) {
            logger.warn("[PageController] progress service unavailable, unlock all levels: {}",
                ex.getMessage());
            return campaignQueryService.getAvailableLevels().stream()
                .map(CampaignLevelView::getId)
                .toList();
        }
    }

    /** Map entity leaderboard thành row đơn giản cho template (kèm created time format). */
    private static List<Map<String, Object>> toRows(List<LeaderboardEntry> entries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            Map<String, Object> row = new HashMap<>();
            row.put("rank", i + 1);
            row.put("playerName", entry.getPlayerName());
            row.put("mapId", entry.getMapId());
            row.put("score", entry.getScore());
            row.put("timeSeconds", entry.getTimeSeconds());
            row.put("hpRemain", entry.getHpRemain());
            row.put("createdAt",
                entry.getCreatedAt() == null ? "" : CREATED_AT_FORMAT.format(entry.getCreatedAt()));
            rows.add(row);
        }
        return rows;
    }
}
