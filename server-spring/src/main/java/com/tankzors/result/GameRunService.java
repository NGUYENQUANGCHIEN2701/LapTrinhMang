package com.tankzors.result;

import com.tankzors.application.PlayerProgressService;
import com.tankzors.application.PlayerStateLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class GameRunService {
    private final GameRunRepository runs;
    private final PlayerStateLock lock;
    private final PlayerProgressService progress;
    public GameRunService(GameRunRepository runs, PlayerStateLock lock, PlayerProgressService progress) {
        this.runs = runs; this.lock = lock; this.progress = progress;
    }
    @Transactional
    public GameRun issue(String playerId, String levelId) {
        lock.acquire(playerId);
        if (!progress.isUnlocked(playerId, levelId)) throw new IllegalArgumentException("Màn chưa mở hoặc đã hoàn thành");
        return runs.save(new GameRun("run-" + UUID.randomUUID(), playerId, levelId, "v1"));
    }
    public GameRun requireOwner(String runId, String playerId) {
        GameRun run = runs.findById(runId).orElseThrow(() -> new IllegalArgumentException("Lượt chơi chưa được cấp"));
        if (!run.getPlayerId().equals(playerId)) throw new ResultService.ConflictException("Lượt chơi thuộc tài khoản khác");
        return run;
    }
    public void requireActive(GameRun run) {
        if (run.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Lượt chơi đã hết hạn");
    }
}
