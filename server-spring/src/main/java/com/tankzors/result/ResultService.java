package com.tankzors.result;

import com.tankzors.application.LeaderboardService;
import com.tankzors.application.PlayerProgressService;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.progress.PlayerTotals;
import com.tankzors.progress.PlayerTotalsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
    private final PlayerTotalsRepository totalsRepo;
    private final GameRunService runs;
    private final com.tankzors.application.PlayerStateLock lock;
    private final com.tankzors.application.PlayerShopService shop;
    private final com.tankzors.leaderboard.LeaderboardRepository leaderboardRepo;

    public ResultService(MatchResultRepository resultRepo,
                         PlayerProgressService progressService,
                         LeaderboardService leaderboardService,
                         PlayerTotalsRepository totalsRepo, GameRunService runs,
                         com.tankzors.application.PlayerStateLock lock,
                         com.tankzors.application.PlayerShopService shop,
                         com.tankzors.leaderboard.LeaderboardRepository leaderboardRepo) {
        this.resultRepo = resultRepo;
        this.progressService = progressService;
        this.leaderboardService = leaderboardService;
        this.totalsRepo = totalsRepo;
        this.runs = runs; this.lock = lock; this.shop = shop; this.leaderboardRepo = leaderboardRepo;
    }

    @Transactional
    public SubmitOutcome submit(ResultPayload payload, String playerId) {
        if (payload == null) throw new IllegalArgumentException("payload null");
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId empty");
        validate(payload);

        PlayerTotals wallet = lock.acquire(playerId);
        GameRun run = runs.requireOwner(payload.runId, playerId);
        if (!run.getLevelId().equals(payload.levelId) || !run.getMapVersion().equals(payload.mapVersion))
            throw new IllegalArgumentException("Kết quả không khớp màn/phiên bản đã cấp");

        String hash = payloadHash(payload);

        // 1. Đã có runId?
        MatchResultEntity existing = resultRepo.findByRunId(payload.runId).orElse(null);
        if (existing != null) {
            if (!playerId.equals(existing.getPlayerId())) throw new ConflictException("Lượt chơi thuộc tài khoản khác");
            if (hash.equals(existing.getPayloadHash())) {
                // Idempotent retry: trả đã lưu, không mutate.
                return currentOutcome(existing, playerId, true);
            }
            throw new ConflictException("runId " + payload.runId + " đã tồn tại với payload khác");
        }
        runs.requireActive(run);
        if (run.getFinishedAt() != null) throw new ConflictException("Lượt chơi đã kết thúc");

        boolean campaignVictory = "VICTORY".equalsIgnoreCase(payload.status)
                && (payload.mode == null || "CAMPAIGN".equalsIgnoreCase(payload.mode));
        // Mỗi màn chỉ thắng một lần: chặn kết quả của màn chưa mở hoặc đã hoàn thành.
        if (campaignVictory && !progressService.isUnlocked(playerId, payload.levelId)) {
            throw new IllegalArgumentException("Màn chưa mở hoặc đã hoàn thành: " + payload.levelId);
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

        // Reward is derived from server metadata and confirmed research. DEFEAT -> 0.
        int[] reward = serverReward(payload, row, shop.extraInterestPercent(playerId));
        row.setStarsEarned(reward[0]);
        row.setCoinsEarned(reward[1]);

        row.setPayloadHash(hash);
        row.setEndedAt(Instant.now());
        resultRepo.save(row);
        log.info("[Result] Lưu runId={} player={} status={} score={} stars={} coins={}",
                payload.runId, playerId, payload.status, payload.score, reward[0], reward[1]);

        // Account lock covers result, wallet, progress and leaderboard until commit.
        wallet.setTotalStars(Math.addExact(wallet.getTotalStars(), reward[0]));
        wallet.setTotalCoins(Math.addExact(wallet.getTotalCoins(), reward[1]));
        wallet.advanceRevision();
        totalsRepo.save(wallet);
        run.finish();
        if (campaignVictory) {
            // Màn vừa thắng bị khoá; danh sách trả về là các màn còn được chơi.
            progressService.markVictory(playerId, payload.levelId);
            leaderboardService.submitScore(
                    playerNameFor(playerId), payload.levelId,
                    payload.score, payload.elapsedSeconds, payload.hpRemain);
        }
        return currentOutcome(row, playerId, false);
    }

    private SubmitOutcome currentOutcome(MatchResultEntity row, String playerId, boolean already) {
        PlayerTotals wallet = totalsRepo.findByPlayerId(playerId).orElseThrow();
        int best = leaderboardRepo.findByPlayerNameAndMapId(playerNameFor(playerId), row.getMapId())
                .map(com.tankzors.leaderboard.LeaderboardEntry::getScore).orElse(row.getScore());
        return new SubmitOutcome(view(row, playerId), best, progressService.getUnlocked(playerId), already,
                wallet.getTotalStars(), wallet.getTotalCoins(), wallet.getRevision());
    }

    /**
     * Fixed map rewards plus server-confirmed interest; client claims cannot set the wallet.
     */
    private static int[] serverReward(ResultPayload payload, MatchResultEntity row, int extraPercent) {
        boolean victory = "VICTORY".equalsIgnoreCase(payload.status)
                && "CAMPAIGN".equalsIgnoreCase(row.getMode());
        if (!victory) return new int[]{0, 0};
        GameMap map = MapLoader.load(payload.levelId);
        if (map == null || map.getRewards() == null) return new int[]{0, 0};
        int maxStars = Math.max(0, map.getRewards().getStars());
        int maxCoins = Math.max(0, map.getRewards().getCoins());
        int stars = maxStars;
        int coins = maxCoins + maxCoins * extraPercent / 100;
        return new int[]{stars, coins};
    }

    private void validate(ResultPayload p) {
        requireText(p.runId, 120, "runId");
        requireText(p.levelId, 30, "levelId");
        requireText(p.mapVersion, 60, "mapVersion");
        requireText(p.status, 20, "status");
        if (p.mode == null) p.mode = "CAMPAIGN";
        if (!"CAMPAIGN".equalsIgnoreCase(p.mode)) throw new IllegalArgumentException("Chưa hỗ trợ lưu mode này");
        p.mode = "CAMPAIGN";
        p.status = p.status.toUpperCase(java.util.Locale.ROOT);
        if (!"VICTORY".equalsIgnoreCase(p.status) && !"DEFEAT".equalsIgnoreCase(p.status)) {
            throw new IllegalArgumentException("status phải VICTORY/DEFEAT, được " + p.status);
        }
        // Giới hạn trên hợp lý (chống bound).
        if (p.score < 0) throw new IllegalArgumentException("score âm");
        if (p.score > 1_000_000) throw new IllegalArgumentException("score quá lớn");
        if (p.aiKills < 0) p.aiKills = 0;
        if (p.aiKills > 1_000) throw new IllegalArgumentException("aiKills quá lớn");
        if (p.hpRemain < 0) p.hpRemain = 0;
        if (p.hpRemain > 100_000) throw new IllegalArgumentException("hpRemain quá lớn");
        if (p.elapsedSeconds < 0) p.elapsedSeconds = 0;
        if (p.elapsedSeconds > 86400) throw new IllegalArgumentException("elapsedSeconds quá lớn");
        if (p.starsEarned < 0) p.starsEarned = 0;
        if (p.starsEarned > 100_000) throw new IllegalArgumentException("starsEarned quá lớn");
        if (p.coinsEarned < 0) p.coinsEarned = 0;
        if (p.coinsEarned > 1_000_000) throw new IllegalArgumentException("coinsEarned quá lớn");
    }
    private static void requireText(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException(field + " không hợp lệ");
    }

    /** Payload hash ổn định để phân biệt retry và conflict. */
    private static String payloadHash(ResultPayload p) {
        String s = p.mode + "|" + p.levelId + "|" + p.mapVersion + "|" + p.status
                + "|" + p.score + "|" + p.hpRemain + "|" + p.aiKills + "|" + p.elapsedSeconds
                + "|" + p.starsEarned + "|" + p.coinsEarned;
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String playerNameFor(String playerId) {
        String accountName = com.tankzors.identity.PlayerNames.accountName(playerId);
        if (accountName != null) return accountName;
        // Giữ khóa khách cũ để không gộp điểm các phiên khách vào một tên chung.
        return "Player-" + playerId.substring(0, Math.min(6, playerId.length()));
    }

    private static ResultView view(MatchResultEntity row, String playerId) {
        ResultView v = new ResultView();
        v.runId = row.getRunId();
        v.status = row.getStatus();
        v.score = row.getScore();
        v.savedAt = row.getEndedAt() == null ? null : row.getEndedAt().toString();
        v.starsEarned = row.getStarsEarned() == null ? 0 : row.getStarsEarned();
        v.coinsEarned = row.getCoinsEarned() == null ? 0 : row.getCoinsEarned();
        return v;
    }

    // ===== public types =====

    public static final class ResultPayload {
        public String runId;
        public String mode;       // CAMPAIGN; co-op persistence needs its own issued-run policy
        public String levelId;
        public String mapVersion;
        public String status;     // VICTORY / DEFEAT
        public int score;
        public int hpRemain;
        public int aiKills;
        public int elapsedSeconds;
        public int starsEarned;   // reported for diagnostics; actual reward comes from server metadata
        public int coinsEarned;
    }

    public static final class ResultView {
        public String runId;
        public String status;
        public int score;
        public String savedAt;
        public int starsEarned;
        public int coinsEarned;
    }

    public static final class SubmitOutcome {
        public final ResultView view;
        public final int bestScore;
        public final List<String> unlockedLevels;
        public final boolean alreadySaved;
        public final int totalStars;
        public final int totalCoins;
        public final long revision;

        private SubmitOutcome(ResultView view, int bestScore, List<String> unlocked, boolean already,
                              int totalStars, int totalCoins, long revision) {
            this.view = view;
            this.bestScore = bestScore;
            this.unlockedLevels = unlocked;
            this.alreadySaved = already;
            this.totalStars = totalStars;
            this.totalCoins = totalCoins;
            this.revision = revision;
        }
    }

    /** 409 khi cùng runId + payload khác. */
    public static final class ConflictException extends RuntimeException {
        public ConflictException(String msg) { super(msg); }
    }
}
