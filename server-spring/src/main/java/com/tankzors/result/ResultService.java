package com.tankzors.result;

import com.tankzors.application.LeaderboardService;
import com.tankzors.application.PlayerProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Lưu kết quả trận client-reported (Bước D). Idempotent và transactional.
 *
 * <p>Không recompute score, không replay. Body không quyết định identity:
 * caller resolve từ HTTP session qua {@link com.tankzors.identity.PlayerIdentityResolver}.</p>
 *
 * <p>Phân nhánh kết quả:</p>
 * <ul>
 *   <li>cùng {@code runId} chưa có → insert + (VICTORY campaign) unlock + bestScore.</li>
 *   <li>cùng {@code runId} + cùng {@code payloadHash} → trả đã lưu (idempotent retry, 200).</li>
 *   <li>cùng {@code runId} + khác payload → {@link ConflictException} (409).</li>
 * </ul>
 */
@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);

    private final MatchResultRepository resultRepo;
    private final PlayerProgressService progressService;
    private final LeaderboardService leaderboardService;

    public ResultService(MatchResultRepository resultRepo,
                         PlayerProgressService progressService,
                         LeaderboardService leaderboardService) {
        this.resultRepo = resultRepo;
        this.progressService = progressService;
        this.leaderboardService = leaderboardService;
    }

    @Transactional
    public SubmitOutcome submit(ResultPayload payload, String playerId) {
        if (payload == null) throw new IllegalArgumentException("payload null");
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId empty");
        validate(payload);

        String hash = payloadHash(payload);

        // 1. Đã có runId?
        MatchResultEntity existing = resultRepo.findByRunId(payload.runId).orElse(null);
        if (existing != null) {
            if (hash.equals(existing.getPayloadHash())) {
                // Idempotent retry: trả đã lưu, không mutate.
                return SubmitOutcome.idempotent(view(existing, playerId));
            }
            throw new ConflictException("runId " + payload.runId + " đã tồn tại với payload khác");
        }

        // 2. Insert mới.
        MatchResultEntity row = new MatchResultEntity();
        row.setRunId(payload.runId);
        row.setPlayerId(playerId);
        row.setPlayerName(playerNameFor(playerId));
        row.setMapId(payload.levelId);
        row.setMapVersion(payload.mapVersion == null ? "" : payload.mapVersion);
        row.setMode(payload.mode == null ? "CAMPAIGN" : payload.mode);
        row.setStatus(payload.status);
        row.setScore(payload.score);
        row.setAiKills(payload.aiKills);
        row.setTimeSeconds(payload.elapsedSeconds);
        row.setHpRemain(payload.hpRemain);
        row.setPayloadHash(hash);
        row.setEndedAt(Instant.now());
        resultRepo.save(row);
        log.info("[Result] Lưu runId={} player={} status={} score={}",
                payload.runId, playerId, payload.status, payload.score);

        // 3. Chỉ VICTORY campaign unlock + bestScore.
        int bestScore = payload.score;
        List<String> unlocked = List.of();
        if ("VICTORY".equalsIgnoreCase(payload.status) && "CAMPAIGN".equalsIgnoreCase(row.getMode())) {
            String next = progressService.markVictory(playerId, payload.levelId);
            unlocked = next != null
                    ? List.of(payload.levelId, next)
                    : List.of(payload.levelId);
            bestScore = leaderboardService.submitScore(
                    playerNameFor(playerId), payload.levelId,
                    payload.score, payload.elapsedSeconds, payload.hpRemain).getScore();
        }
        return SubmitOutcome.saved(view(row, playerId), bestScore, unlocked);
    }

    private void validate(ResultPayload p) {
        if (p.runId == null || p.runId.isBlank()) throw new IllegalArgumentException("runId trống");
        if (p.status == null || p.status.isBlank()) throw new IllegalArgumentException("status trống");
        if (!"VICTORY".equalsIgnoreCase(p.status) && !"DEFEAT".equalsIgnoreCase(p.status)) {
            throw new IllegalArgumentException("status phải VICTORY/DEFEAT, được " + p.status);
        }
        if (p.levelId == null || p.levelId.isBlank()) throw new IllegalArgumentException("levelId trống");
        // Giới hạn trên hợp lý (chống bound).
        if (p.score < 0) throw new IllegalArgumentException("score âm");
        if (p.score > 1_000_000) throw new IllegalArgumentException("score quá lớn");
        if (p.aiKills < 0) p.aiKills = 0;
        if (p.aiKills > 1_000) throw new IllegalArgumentException("aiKills quá lớn");
        if (p.hpRemain < 0) p.hpRemain = 0;
        if (p.hpRemain > 100_000) throw new IllegalArgumentException("hpRemain quá lớn");
        if (p.elapsedSeconds < 0) p.elapsedSeconds = 0;
        if (p.elapsedSeconds > 86400) throw new IllegalArgumentException("elapsedSeconds quá lớn");
    }

    /** Payload hash ổn định để phân biệt retry và conflict. */
    private static String payloadHash(ResultPayload p) {
        String s = p.mode + "|" + p.levelId + "|" + p.mapVersion + "|" + p.status
                + "|" + p.score + "|" + p.hpRemain + "|" + p.aiKills + "|" + p.elapsedSeconds;
        // FNV-1a 64 bit (deterministic, không phụ thuộc SHA/MessageDigest thu sẵn).
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return Long.toHexString(h);
    }

    private static String playerNameFor(String playerId) {
        // Tên hiển thị ngắn; hợp nhất với leaderboard key.
        return "Player-" + playerId.substring(0, Math.min(6, playerId.length()));
    }

    private static ResultView view(MatchResultEntity row, String playerId) {
        ResultView v = new ResultView();
        v.runId = row.getRunId();
        v.status = row.getStatus();
        v.score = row.getScore();
        v.savedAt = row.getEndedAt() == null ? null : row.getEndedAt().toString();
        return v;
    }

    // ===== public types =====

    public static final class ResultPayload {
        public String runId;
        public String mode;       // CAMPAIGN / COOP
        public String levelId;
        public String mapVersion;
        public String status;     // VICTORY / DEFEAT
        public int score;
        public int hpRemain;
        public int aiKills;
        public int elapsedSeconds;
    }

    public static final class ResultView {
        public String runId;
        public String status;
        public int score;
        public String savedAt;
    }

    public static final class SubmitOutcome {
        public final ResultView view;
        public final int bestScore;
        public final List<String> unlockedLevels;
        public final boolean alreadySaved;

        private SubmitOutcome(ResultView view, int bestScore, List<String> unlocked, boolean already) {
            this.view = view; this.bestScore = bestScore; this.unlockedLevels = unlocked; this.alreadySaved = already;
        }
        static SubmitOutcome saved(ResultView v, int best, List<String> u) { return new SubmitOutcome(v, best, u, false); }
        static SubmitOutcome idempotent(ResultView v) { return new SubmitOutcome(v, v.score, List.of(), true); }
    }

    /** 409 khi cùng runId + payload khác. */
    public static final class ConflictException extends RuntimeException {
        public ConflictException(String msg) { super(msg); }
    }
}
