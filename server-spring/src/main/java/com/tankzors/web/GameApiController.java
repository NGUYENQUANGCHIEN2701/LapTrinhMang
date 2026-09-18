package com.tankzors.web;

import com.tankzors.application.PlayerProgressService;
import com.tankzors.identity.PlayerIdentityResolver;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.result.ResultService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API cho client-authoritative gameplay (Bước D):
 * <ul>
 *   <li>{@code GET /api/game/bootstrap?levelId=...}: trả map JSON + runId + metadata.</li>
 *   <li>{@code POST /api/game/results}: nhận kết quả client-reported (idempotent).</li>
 * </ul>
 *
 * <p>Server chỉ phục vụ dữ liệu + lưu kết quả; KHÔNG tạo tank/world, KHÔNG recompute score.</p>
 */
@RestController
@RequestMapping("/api/game")
public class GameApiController {

    private static final Logger log = LoggerFactory.getLogger(GameApiController.class);
    private static final String PROTOCOL_VERSION = "v2";

    private final PlayerIdentityResolver identity;
    private final PlayerProgressService progress;
    private final ResultService resultService;

    public GameApiController(PlayerIdentityResolver identity,
                              PlayerProgressService progress,
                              ResultService resultService) {
        this.identity = identity;
        this.progress = progress;
        this.resultService = resultService;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<Map<String, Object>> bootstrap(
            @RequestParam(name = "levelId", defaultValue = "campaign_1") String levelId,
            HttpServletRequest request) {
        String playerId = identity.resolve(request);
        // Kiểm tra level tồn tại + quyền tải (campaign unlock).
        if (!progress.isUnlocked(playerId, levelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Level chưa unlock: " + levelId));
        }
        GameMap map = MapLoader.load(levelId);
        if (map == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy map: " + levelId));
        }
        String runId = "run-" + UUID.randomUUID();
        String nextLevelId = nextLevel(levelId);

        Map<String, Object> mapPayload = new HashMap<>();
        mapPayload.put("name", map.getName());
        mapPayload.put("difficulty", map.getDifficulty());
        mapPayload.put("width", map.getWidth());
        mapPayload.put("height", map.getHeight());
        mapPayload.put("tileSize", map.getTileSize());
        // grid
        int[][] grid = map.getGrid();
        if (grid != null) {
            mapPayload.put("grid", grid);
        }
        // enemies
        if (map.getEnemies() != null) {
            List<Map<String, Object>> enemies = map.getEnemies().stream()
                    .map(e -> {
                        Map<String, Object> em = new HashMap<>();
                        em.put("type", e.getType());
                        em.put("difficulty", e.getDifficulty());
                        em.put("spawnX", e.getSpawnX());
                        em.put("spawnY", e.getSpawnY());
                        return em;
                    }).toList();
            mapPayload.put("enemies", enemies);
        }
        if (map.getRewards() != null) {
            Map<String, Object> r = new HashMap<>();
            r.put("stars", map.getRewards().getStars());
            r.put("coins", map.getRewards().getCoins());
            mapPayload.put("rewards", r);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("protocolVersion", PROTOCOL_VERSION);
        body.put("playerId", playerId);
        body.put("levelId", levelId);
        body.put("nextLevelId", nextLevelId);
        body.put("mapVersion", "v1"); // version dữ liệu map hiện tại
        body.put("map", mapPayload);
        body.put("settings", Map.of());
        body.put("unlockedLevels", progress.getUnlocked(playerId));
        body.put("runId", runId);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/results")
    public ResponseEntity<Map<String, Object>> submitResult(
            @RequestBody ResultService.ResultPayload payload,
            HttpServletRequest request) {
        String playerId = identity.resolve(request);
        ResultService.SubmitOutcome out = resultService.submit(payload, playerId);
        Map<String, Object> body = new HashMap<>();
        body.put("runId", out.view.runId);
        body.put("status", out.view.status);
        body.put("score", out.view.score);
        body.put("bestScore", out.bestScore);
        body.put("unlockedLevels", out.unlockedLevels);
        body.put("saved", true);
        body.put("alreadySaved", out.alreadySaved);
        body.put("starsEarned", out.view.starsEarned);
        body.put("coinsEarned", out.view.coinsEarned);
        body.put("totalStars", out.totalStars);
        body.put("totalCoins", out.totalCoins);
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(ResultService.ConflictException.class)
    public ResponseEntity<Map<String, Object>> onConflict(ResultService.ConflictException e) {
        log.warn("[Result] conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> onBad(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private static String nextLevel(String current) {
        if ("campaign_1".equals(current)) return "campaign_2";
        if ("campaign_2".equals(current)) return "campaign_3";
        return null;
    }
}
