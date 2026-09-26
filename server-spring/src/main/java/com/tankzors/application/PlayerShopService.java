package com.tankzors.application;

import com.tankzors.progress.PlayerShopState;
import com.tankzors.progress.PlayerShopStateRepository;
import com.tankzors.progress.PlayerTotals;
import com.tankzors.progress.PlayerTotalsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service lưu trữ và đồng bộ hóa trạng thái cửa hàng, túi đồ và cây nghiên cứu của player.
 */
@Service
public class PlayerShopService {

    private static final Logger log = LoggerFactory.getLogger(PlayerShopService.class);

    private final PlayerTotalsRepository totalsRepo;
    private final PlayerShopStateRepository shopStateRepo;

    public PlayerShopService(PlayerTotalsRepository totalsRepo,
                             PlayerShopStateRepository shopStateRepo) {
        this.totalsRepo = totalsRepo;
        this.shopStateRepo = shopStateRepo;
    }

    public static record ShopData(int totalCoins, int totalStars, String researchJson, String inventoryJson) {}

    public static record SyncRequest(Integer coins, Integer stars, String researchJson, String inventoryJson) {}

    @Transactional(readOnly = true)
    public ShopData getShopData(String playerId) {
        PlayerTotals totals = totalsRepo.findByPlayerId(playerId)
                .orElseGet(() -> new PlayerTotals(playerId, 0, 0));
        PlayerShopState state = shopStateRepo.findByPlayerId(playerId)
                .orElseGet(() -> new PlayerShopState(playerId, "{}", "{}"));
        return new ShopData(totals.getTotalCoins(), totals.getTotalStars(), state.getResearchJson(), state.getInventoryJson());
    }

    @Transactional
    public ShopData syncShopData(String playerId, SyncRequest req) {
        PlayerTotals totals = totalsRepo.findByPlayerId(playerId)
                .orElseGet(() -> new PlayerTotals(playerId, 0, 0));
        PlayerShopState state = shopStateRepo.findByPlayerId(playerId)
                .orElseGet(() -> new PlayerShopState(playerId, "{}", "{}"));

        if (req.coins != null && req.coins >= 0) {
            totals.setTotalCoins(req.coins);
        }
        if (req.stars != null && req.stars >= 0) {
            totals.setTotalStars(req.stars);
        }
        totals.setUpdatedAt(Instant.now());
        totalsRepo.save(totals);

        if (req.researchJson != null) {
            state.setResearchJson(req.researchJson);
        }
        if (req.inventoryJson != null) {
            state.setInventoryJson(req.inventoryJson);
        }
        state.setUpdatedAt(Instant.now());
        shopStateRepo.save(state);

        log.info("[ShopSync] playerId={} coins={} stars={}", playerId, totals.getTotalCoins(), totals.getTotalStars());
        return new ShopData(totals.getTotalCoins(), totals.getTotalStars(), state.getResearchJson(), state.getInventoryJson());
    }
}

