package com.tankzors.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tankzors.application.LeaderboardService;
import com.tankzors.leaderboard.LeaderboardEntry;

/**
 * REST endpoint đọc leaderboard. Điểm chỉ được ghi qua {@code POST /api/game/results}
 * ({@link com.tankzors.result.ResultService}), không có API ghi điểm trực tiếp.
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Top 10 score của một map.
     */
    @GetMapping("/{mapId}")
    public List<LeaderboardEntry> topScores(@PathVariable String mapId) {
        return leaderboardService.topScores(mapId);
    }
}
