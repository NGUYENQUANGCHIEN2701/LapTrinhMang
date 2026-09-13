package com.tankzors.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tankzors.application.LeaderboardService;
import com.tankzors.leaderboard.LeaderboardEntry;

/**
 * REST endpoints cho leaderboard (Phase 4, mới — không trên wire v1).
 *
 * <p>Endpoint đặt tách rời /api/leaderboard để không phá wire protocol-v1
 * của WebSocket /game. Phía game-client LibGDX desktop chưa submit score;
 * React client cũ cũng chưa. Phase 5/3.5 sẽ gọi endpoint này từ client khi
 * match kết thúc.</p>
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

    /**
     * Submit score (upsert theo player+map).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@RequestBody SubmitScoreRequest req) {
        if (req.playerName == null || req.mapId == null || req.score < 0) {
            return ResponseEntity.badRequest().body(Map.of("status", "INVALID"));
        }
        LeaderboardEntry saved = leaderboardService.submitScore(
            req.playerName, req.mapId, req.score,
            req.timeSeconds == null ? 0 : req.timeSeconds,
            req.hpRemain == null ? 0 : req.hpRemain
        );
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "score", saved.getScore(),
            "createdAt", saved.getCreatedAt().toString()
        ));
    }

    /**
     * Request body schema cho POST /api/leaderboard.
     */
    public static class SubmitScoreRequest {
        public String playerName;
        public String mapId;
        public Integer score;
        public Integer timeSeconds;
        public Integer hpRemain;
    }
}
