package com.tankzors.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tankzors.application.LeaderboardService;
import com.tankzors.leaderboard.LeaderboardEntry;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Thymeleaf page controller (Phase 4.5).
 *
 * <p>Routes:</p>
 * <ul>
 *   <li>{@code GET /} -> Home/menu page</li>
 *   <li>{@code GET /campaign} -> Campaign level selection</li>
 *   <li>{@code GET /play?levelId=campaign_X} -> Play page (iframe React build)</li>
 *   <li>{@code GET /leaderboard?mapId=...} -> Leaderboard page</li>
 * </ul>
 *
 * <p>REST endpoints ({@code /api/*}) vẫn nằm ở {@link HealthController} và
 * {@link LeaderboardController}.</p>
 */
@Controller
public class PageController {

    private static final List<Map<String, Object>> CAMPAIGN_LEVELS = List.of(
        levelConfig("campaign_1", "Màn 1 - Huấn luyện", "EASY", "Dễ", 1, "Bản đồ mở rộng, làm quen điều khiển và ngắm bắn."),
        levelConfig("campaign_2", "Màn 2 - Chiến hào", "MEDIUM", "Trung bình", 2, "Chiến hào quanh co và chướng ngại vật chông sắt chống tăng."),
        levelConfig("campaign_3", "Màn 3 - Pháo đài", "HARD", "Khó", 3, "Pháo đài kiên cố, kẻ địch tinh nhuệ bao vây 3 hướng.")
    );

    private static final List<String> LEADERBOARD_MAPS = List.of(
        "campaign_1", "campaign_2", "campaign_3"
    );

    private final LeaderboardService leaderboardService;

    public PageController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {
        model.addAttribute("activePage", "home");
        return "home";
    }

    @GetMapping("/campaign")
    public String campaign(Model model) {
        model.addAttribute("activePage", "campaign");
        model.addAttribute("levels", CAMPAIGN_LEVELS);
        return "campaign";
    }

    @GetMapping("/play")
    public String play(@RequestParam(name = "levelId", defaultValue = "campaign_1") String levelId, Model model) {
        model.addAttribute("activePage", "play");
        model.addAttribute("levelId", levelId);
        return "play";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(
        @RequestParam(name = "mapId", defaultValue = "campaign_1") String mapId,
        Model model
    ) {
        model.addAttribute("activePage", "leaderboard");
        model.addAttribute("maps", LEADERBOARD_MAPS);
        model.addAttribute("currentMap", mapId);
        model.addAttribute("entries", leaderboardService.topScores(mapId));
        return "leaderboard";
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

    private static Map<String, Object> levelConfig(String id, String title, String difficulty,
                                                    String difficultyLabel, int enemiesCount, String desc) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("difficulty", difficulty);
        m.put("difficultyLabel", difficultyLabel);
        m.put("enemiesCount", enemiesCount);
        m.put("desc", desc);
        return m;
    }
}
