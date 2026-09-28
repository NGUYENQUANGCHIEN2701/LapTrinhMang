package com.tankzors.client.session;

import com.tankzors.client.GameClient;
import com.tankzors.client.audio.AudioManager;
import com.tankzors.client.gameplay.GameEventListener;
import com.tankzors.client.gameplay.GameSimulation;
import com.tankzors.client.gameplay.GameWorld;
import com.tankzors.client.gameplay.WorldSnapshotMapper;
import com.tankzors.client.gameplay.map.MapDefinition;
import com.tankzors.client.gameplay.map.MapLoader;
import com.tankzors.client.gameplay.result.MatchResult;
import com.tankzors.client.gameplay.rules.ScoreCalculator;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Session campaign local: chạy simulation hoàn toàn trên client, KHÔNG WebSocket.
 * Map được nạp từ JSON (do launcher inject qua REST bootstrap/asset/param).
 * Khi terminal: tính điểm bằng ScoreCalculator, tạo MatchResult, gọi resultHandler.
 *
 * <h3>Luồng tải map (async-safe)</h3>
 * Map có thể tải bất đồng bộ (REST trên HTML5), nên restart/nextLevel <b>không</b>
 * tạo simulation mới ngay rồi gọi {@code loadMapJson()} sync — điều đó sẽ kẹt WAITING
 * vì {@code BootstrapMapSource.loadMapJson()} luôn trả null. Thay vào đó:
 * <ol>
 *   <li>Session sang {@link Phase#LOADING_NEXT}, {@code inFlight=true} (chặn double-request).</li>
 *   <li>Gọi {@link MapSource#requestLevel} với callback nhận kết quả.</li>
 *   <li>Callback thành công → parse vào biến tạm; chỉ khi map hợp lệ mới swap sim/level/run,
 *       đặt {@link Phase#PLAYING}. Thất bại → {@link Phase#LOAD_ERROR}, sim cũ giữ nguyên.</li>
 * </ol>
 * Snapshot trong transition giữ nguyên world cũ (overlay kết quả vẫn hiện).
 */
public class LocalCampaignSession implements GameSession, MapSource.LevelCallback {

    private final String playerId;
    private final String mode = "CAMPAIGN";
    private final ResultHandler resultHandler;
    private final MapSource mapSource;
    private AudioManager audioManager;

    private GameSimulation sim;
    private String levelId;
    private String runId;
    private String mapVersion;
    private MatchResult matchResult;
    private boolean matchResultFired;

    // Map đã nạp, dùng cho spawn khi restart (desktop path sync).
    private MapDefinition loadedMap;
    /** Reward của màn hiện tại (lấy từ MapDefinition.rewards), reset mỗi lần load map. */
    private int currentRewardStars;
    private int currentRewardCoins;

    /** Phase do session quản lý, tách khỏi {@link GameWorld.Status} (vốn chỉ PLAYING/VICTORY/DEFEAT). */
    private enum Phase { INIT, PLAYING, LOADING_NEXT, LOAD_ERROR }
    private Phase phase = Phase.INIT;

    /** Level đang chờ bootstrap (cho cả retry khi LOAD_ERROR). */
    private String pendingLevelId;
    /** Fence: chỉ có tối đa 1 request bootstrap đang bay. */
    private boolean inFlight;
    /**
     * Token tăng mỗi lần request, để reject callback của request cũ về muộn
     * (vd đã restart sang màn khác) — tránh ghi đè session hiện hành.
     */
    private int requestToken;

    /** Callback lưu kết quả (do caller inject: BrowserResultGateway Bước D). */
    public interface ResultHandler {
        /**
         * @param result kết quả đã chốt. Client nên gửi REST idempotent.
         * @return true nếu chấp nhận (illon hiển thị ngay); false nếu lỗi thì vẫn hiển thị.
         */
        boolean onResult(MatchResult result);
    }

    private final com.tankzors.client.shop.PlayerShopState shopState = new com.tankzors.client.shop.PlayerShopState();
    private ShopSyncHandler shopSyncHandler;
    private boolean shopStateLoaded;
    private Map<Integer, Integer> levelStartInventory;
    private int levelStartWeaponId;
    private boolean inventoryMenuOpen;
    private int inventorySelectionIndex;

    public interface ShopSyncHandler {
        void onSyncShop(int coins, int stars, String researchJson, String inventoryJson);
    }

    public com.tankzors.client.shop.PlayerShopState getShopState() {
        return shopState;
    }

    public void setShopSyncHandler(ShopSyncHandler handler) {
        this.shopSyncHandler = handler;
    }

    public void syncShopState() {
        if (shopSyncHandler != null) {
            shopSyncHandler.onSyncShop(shopState.getCoins(), shopState.getStars(),
                    shopState.serializeResearch(), shopState.serializeInventory());
        }
    }

    @Override
    public void cycleWeapon() {
        if (shopState != null) {
            shopState.cycleNextWeapon();
            updateSimPlayerWeapon();
        }
    }

    @Override
    public void selectWeapon(int weaponId) {
        if (shopState != null) {
            shopState.selectWeapon(weaponId);
            updateSimPlayerWeapon();
        }
    }

    private void updateSimPlayerWeapon() {
        if (sim != null && sim.getWorld() != null) {
            for (com.tankzors.client.gameplay.interfaces.ITank t : sim.getWorld().getAllTanks()) {
                if (t instanceof com.tankzors.client.gameplay.entity.PlayerTank) {
                    ((com.tankzors.client.gameplay.entity.PlayerTank) t).setActiveWeaponId(shopState.getActiveWeaponId());
                }
            }
        }
    }

    /** Lazy variant: MapSource sẽ load trong ensureStarted() / requestLevel (Gdx đã init). */
    public LocalCampaignSession(String playerId, String levelId,
                                MapSource mapSource, ResultHandler resultHandler) {
        this(playerId, levelId, mapSource, resultHandler, null);
    }

    public LocalCampaignSession(String playerId, String levelId,
                                MapSource mapSource, ResultHandler resultHandler,
                                AudioManager audioManager) {
        this.playerId = playerId;
        this.levelId = levelId;
        this.runId = mapSource.newRunId(levelId);
        this.mapVersion = mapSource.mapVersion(levelId);
        this.resultHandler = resultHandler;
        this.mapSource = mapSource;
        this.audioManager = audioManager;
        this.sim = new GameSimulation(seedFor(runId, levelId));
        this.sim.setShopState(this.shopState);
        if (audioManager != null) {
            this.sim.setEventListener(createAudioBridge());
        }
        // Chưa start — sẽ load qua requestLevel (async) của launcher.
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
        if (sim != null && audioManager != null) {
            sim.setEventListener(createAudioBridge());
        }
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    private GameEventListener createAudioBridge() {
        return new GameEventListener() {
            @Override public void onShoot(boolean isPlayer) {
                if (audioManager != null) audioManager.playShoot(isPlayer);
            }
            @Override public void onHitWall() {
                if (audioManager != null) audioManager.playHitWall();
            }
            @Override public void onHitTank(boolean isPlayer) {
                if (audioManager != null) audioManager.playHitTank(isPlayer);
            }
            @Override public void onTankDestroyed(boolean isPlayer) {
                if (audioManager != null) audioManager.playTankDestroyed(isPlayer);
            }
            @Override public void onMatchEnd(GameWorld.Status status) {
                if (audioManager != null) {
                    if (status == GameWorld.Status.VICTORY) {
                        audioManager.playVictoryMusic();
                    } else if (status == GameWorld.Status.DEFEAT) {
                        audioManager.playDefeatMusic();
                    }
                }
            }
            @Override public void onPickup() { if (audioManager != null) audioManager.playPickup(); }
            @Override public void onUseRepair() { if (audioManager != null) audioManager.playRepair(); }
            @Override public void onUseSpeed() { if (audioManager != null) audioManager.playSpeed(); }
        };
    }

    /**
     * Khởi tạo bất đồng bộ: yêu cầu map đầu tiên từ MapSource.
     * Launcher (HTML5/desktop) gọi sau khi Gdx đã ready và session đã được gắn.
     */
    public void start() {
        requestBootstrap(levelId);
    }

    private void ensureStarted() {
        if (phase == Phase.PLAYING) return;
        // Phase ban đầu chỉ chuyển sang chơi khi bootstrap về. Không cố load sync.
    }

    private boolean applyMap(String mapJson) {
        loadedMap = MapLoader.parse(mapJson);
        if (loadedMap == null) {
            System.err.println("[LocalCampaignSession] Không parse được map '" + levelId + "'");
            return false;
        }
        int[] rw = readRewards(loadedMap);
        currentRewardStars = rw[0];
        currentRewardCoins = rw[1];
        sim.setMatchEndHandler(this::onMatchEnd);
        if (audioManager != null) {
            sim.setEventListener(createAudioBridge());
            audioManager.playBattleMusic(levelId);
        }
        sim.startCampaignLevel(loadedMap, levelId, playerId);
        return true;
    }

    // ===== INPUT =====
    @Override public void onMove(Direction direction, boolean moving) {
        if (phase != Phase.PLAYING || inventoryMenuOpen) return;
        sim.enqueueMove(toGameplayDirection(direction), moving);
    }
    @Override public void onShoot() {
        if (phase != Phase.PLAYING || inventoryMenuOpen) return;
        sim.enqueueShoot();
    }
    @Override public void onAllReleased() {
        if (phase != Phase.PLAYING) return;
        sim.enqueueStopAll();
    }
    @Override public void onReverse(boolean reversing) {
        if (phase != Phase.PLAYING || inventoryMenuOpen) return;
        sim.enqueueReverse(reversing);
    }

    private static com.tankzors.client.gameplay.entity.Direction toGameplayDirection(Direction d) {
        if (d == null) return null;
        switch (d) {
            case UP: return com.tankzors.client.gameplay.entity.Direction.UP;
            case DOWN: return com.tankzors.client.gameplay.entity.Direction.DOWN;
            case LEFT: return com.tankzors.client.gameplay.entity.Direction.LEFT;
            case RIGHT: return com.tankzors.client.gameplay.entity.Direction.RIGHT;
        }
        return null;
    }

    // ===== UPDATE =====
    @Override public void update(float rawDeltaTime) {
        if (phase == Phase.PLAYING && !inventoryMenuOpen) sim.advance(rawDeltaTime);
    }

    @Override public GameStateSnapshot snapshot() {
        GameWorld w = sim.getWorld();
        int elapsedSec = (int) Math.max(0, sim.getElapsedSeconds());
        GameStateSnapshot snap = WorldSnapshotMapper.snapshot(w, playerId, w.getScore(), elapsedSec);
        // Khi đang transition, báo renderer giữ overlay cũ + hiện trạng tải.
        if (phase == Phase.LOADING_NEXT) {
            snap.transitionStatus = "LOADING";
        } else if (phase == Phase.LOAD_ERROR) {
            snap.transitionStatus = "LOAD_ERROR";
        }
        // Debrief fields: chỉ có ý nghĩa khi terminal — fill luôn (renderer tự check status).
        snap.elapsedSeconds = elapsedSec;
        snap.aiKills = sim.getAiKilledThisMatch();
        snap.hpRemain = 0;
        for (com.tankzors.client.gameplay.interfaces.ITank t : w.getAllTanks()) {
            if (t instanceof com.tankzors.client.gameplay.entity.PlayerTank) {
                snap.hpRemain = t.getHP();
                break;
            }
        }
        // Reward/from MatchResult (chốt cuối trận). 0 khi chưa kết thúc hoặc DEFEAT.
        if (matchResult != null) {
            snap.starsEarned = matchResult.starsEarned;
            snap.coinsEarned = matchResult.coinsEarned;
        }
        if (shopState != null) {
            snap.playerCoins = shopState.getCoins();
            snap.playerStars = shopState.getStars();
            snap.activeWeaponId = shopState.getActiveWeaponId();
            snap.activeWeaponAmmo = shopState.getItemCount(shopState.getActiveWeaponId());
            snap.repairCount = shopState.getItemCount(ShopItem.REPAIR.getId());
            snap.speedCount = shopState.getItemCount(ShopItem.SPEED_UP.getId());
            List<Integer> ids = inventoryMenuItems();
            snap.inventoryItemIds = new int[ids.size()];
            snap.inventoryItemCounts = new int[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                snap.inventoryItemIds[i] = ids.get(i);
                snap.inventoryItemCounts[i] = shopState.getItemCount(ids.get(i));
            }
        }
        sim.appendEffectsTo(snap);
        snap.speedBoostSecondsRemaining = (float) sim.getSpeedBoostRemaining();
        snap.inventoryMenuOpen = inventoryMenuOpen;
        snap.inventorySelectionIndex = inventorySelectionIndex;
        return snap;
    }

    @Override public boolean isInventoryMenuOpen() { return inventoryMenuOpen; }

    @Override public void openInventoryMenu() {
        if (phase != Phase.PLAYING || !sim.getWorld().isPlaying()) return;
        inventoryMenuOpen = true;
        inventorySelectionIndex = 0;
        sim.enqueueStopAll();
    }

    @Override public void moveInventorySelection(int delta) {
        if (!inventoryMenuOpen) return;
        int size = inventoryMenuItems().size();
        if (size == 0) return;
        inventorySelectionIndex = (inventorySelectionIndex + delta) % size;
        if (inventorySelectionIndex < 0) inventorySelectionIndex += size;
    }

    @Override public void confirmInventorySelection() {
        if (!inventoryMenuOpen) return;
        List<Integer> items = inventoryMenuItems();
        if (!items.isEmpty()) {
            int index = Math.max(0, Math.min(inventorySelectionIndex, items.size() - 1));
            int itemId = items.get(index);
            if (itemId >= 0 && itemId <= 5) {
                selectWeapon(itemId);
            } else {
                sim.useInventoryItem(itemId);
            }
        }
        inventoryMenuOpen = false;
        inventorySelectionIndex = 0;
    }

    private List<Integer> inventoryMenuItems() {
        List<Integer> ids = new ArrayList<>();
        for (int id = 0; id <= 5; id++) {
            if (shopState.getItemCount(id) > 0) ids.add(id);
        }
        if (shopState.getItemCount(ShopItem.REPAIR.getId()) > 0) ids.add(ShopItem.REPAIR.getId());
        if (shopState.getItemCount(ShopItem.SPEED_UP.getId()) > 0) ids.add(ShopItem.SPEED_UP.getId());
        return ids;
    }

    @Override public String localPlayerId() { return playerId; }
    @Override public String status() {
        switch (phase) {
            case INIT: return "WAITING";
            case LOADING_NEXT: return "LOADING";
            case LOAD_ERROR: return "LOAD_ERROR";
            case PLAYING: default: return sim.getWorld().getStatus().name();
        }
    }

    // ===== RESTART / NEXT =====
    @Override
    public void restart() {
        matchResult = null;
        matchResultFired = false;
        inventoryMenuOpen = false;
        if (levelStartInventory != null) {
            shopState.restoreInventory(levelStartInventory);
            shopState.setActiveWeaponId(levelStartWeaponId);
        }
        // Re-bootstrap để nhận runId + mapVersion mới (chống trùng idempotency).
        requestBootstrap(levelId);
    }

    @Override
    public void nextLevel() {
        if (phase == Phase.LOADING_NEXT) return; // đang tải, bỏ qua
        if (sim.getWorld().getStatus() != GameWorld.Status.VICTORY) return;
        String next = nextCampaignLevel(levelId);
        if (next == null) return;
        // Chỉ cho chuyển màn kế sau VICTORY; input và session cùng chặn để
        // không thể bỏ qua một màn bằng cách gọi trực tiếp API session.
        matchResult = null;
        matchResultFired = false;
        requestBootstrap(next);
    }

    /**
     * Yêu cầu bootstrap một level. Trả về ngay; kết quả về qua {@link LevelCallback}.
     * Idempotent w.r.t. levelId: gọi lại cùng level trong khi đang bay → bỏ qua (chỉ 1 request).
     */
    private void requestBootstrap(String targetLevelId) {
        if (inFlight && targetLevelId.equals(pendingLevelId)) {
            return; // cùng request đã bay, không duplicate.
        }
        pendingLevelId = targetLevelId;
        inFlight = true;
        phase = Phase.LOADING_NEXT;
        requestToken++;
        final int myToken = requestToken;
        try {
            mapSource.requestLevel(targetLevelId, new MapSource.LevelCallback() {
                @Override public void onLevelLoaded(String lvl, String mapJson, String newRunId, String newMapVersion) {
                    // Reject callback của request cũ (đã chuyển hướng sang level khác).
                    if (myToken != requestToken) return;
                    LocalCampaignSession.this.onLevelLoaded(lvl, mapJson, newRunId, newMapVersion);
                }
                @Override public void onLoadError(String reason) {
                    if (myToken != requestToken) return;
                    LocalCampaignSession.this.onLoadError(reason);
                }
            });
        } catch (UnsupportedOperationException e) {
            // Source desktop path cũ: thử đồng bộ loadMapJson (assets jar).
            String json = mapSource.loadMapJson(targetLevelId);
            if (json != null) {
                onLevelLoaded(targetLevelId, json, null, null);
            } else {
                onLoadError("MapSource không hỗ trợ requestLevel/loadMapJson");
            }
        }
    }

    // ===== LevelCallback: mutation duy nhất của (level, sim, run) =====
    @Override
    public void onLevelLoadedWithShop(String loadedLevelId, String mapJson, String newRunId, String newMapVersion,
                                       int totalCoins, int totalStars, String researchJson, String inventoryJson) {
        // Chỉ bootstrap inventory một lần. Cùng PlayerShopState sống xuyên suốt
        // campaign để màn kế không nạp lại số đạn cũ từ một REST sync về chậm.
        if (!shopStateLoaded) {
            shopState.loadState(totalCoins, totalStars, researchJson, inventoryJson);
            shopStateLoaded = true;
        }
        onLevelLoaded(loadedLevelId, mapJson, newRunId, newMapVersion);
    }

    @Override
    public void onLevelLoaded(String loadedLevelId, String mapJson, String newRunId, String newMapVersion) {
        // Parse vào simulation tạm — chỉ cam kết mutation khi map hợp lệ.
        GameSimulation next = new GameSimulation(seedFor(
                newRunId != null ? newRunId : "local-" + loadedLevelId, loadedLevelId));
        next.setShopState(shopState);
        MapDefinition nextMap = MapLoader.parse(mapJson);
        if (nextMap == null) {
            System.err.println("[LocalCampaignSession] map parse fail khi bootstrap '" + loadedLevelId + "'");
            onLoadError("Map JSON không hợp lệ");
            return;
        }
        // Atomic swap.
        this.sim = next;
        this.levelId = loadedLevelId;
        this.runId = newRunId != null ? newRunId
                : "local-" + loadedLevelId + "-" + nextSeq++;
        this.mapVersion = newMapVersion != null ? newMapVersion : mapSource.mapVersion(loadedLevelId);
        this.loadedMap = nextMap;
        int[] rw = readRewards(nextMap);
        currentRewardStars = rw[0];
        currentRewardCoins = rw[1];
        matchResult = null;
        matchResultFired = false;
        sim.setMatchEndHandler(this::onMatchEnd);
        if (audioManager != null) {
            sim.setEventListener(createAudioBridge());
            audioManager.playBattleMusic(levelId);
        }
        sim.startCampaignLevel(loadedMap, levelId, playerId);
        levelStartInventory = shopState.snapshotInventory();
        levelStartWeaponId = shopState.getActiveWeaponId();
        inventoryMenuOpen = false;
        phase = Phase.PLAYING;
        inFlight = false;
    }

    @Override
    public void onLoadError(String reason) {
        System.err.println("[LocalCampaignSession] bootstrap error: " + reason);
        // Giữ nguyên sim hiện tại. Renderer hiển thị LOAD_ERROR, R/N để retry.
        phase = Phase.LOAD_ERROR;
        inFlight = false;
    }

    /** Nạp map mới (compat path: caller đã có JSON, vd host/guest path hoặc test).
     *  Đồng bộ — không qua requestLevel. parse fail → LOAD_ERROR, không phá world. */
    public void changeLevel(String newLevelId, String mapJson, String runId, String mapVersion) {
        GameSimulation next = new GameSimulation(seedFor(
                runId != null ? runId : "local-" + newLevelId, newLevelId));
        next.setShopState(shopState);
        MapDefinition nextMap = MapLoader.parse(mapJson);
        if (nextMap == null) {
            System.err.println("[LocalCampaignSession] changeLevel parse fail '" + newLevelId + "'");
            onLoadError("Map JSON không hợp lệ");
            return;
        }
        this.sim = next;
        this.levelId = newLevelId;
        this.runId = runId != null ? runId : "local-" + newLevelId + "-" + nextSeq++;
        this.mapVersion = mapVersion != null ? mapVersion : mapSource.mapVersion(newLevelId);
        this.loadedMap = nextMap;
        int[] rw = readRewards(nextMap);
        currentRewardStars = rw[0];
        currentRewardCoins = rw[1];
        matchResult = null;
        matchResultFired = false;
        sim.setMatchEndHandler(this::onMatchEnd);
        sim.startCampaignLevel(loadedMap, levelId, playerId);
        levelStartInventory = shopState.snapshotInventory();
        levelStartWeaponId = shopState.getActiveWeaponId();
        inventoryMenuOpen = false;
        phase = Phase.PLAYING;
        inFlight = false;
    }

    private void onMatchEnd(String lvl, String status, int hpRemain, int aiKills, int elapsedSeconds) {
        if (matchResultFired) return;
        matchResultFired = true;
        int score = ScoreCalculator.calculate(hpRemain, aiKills, elapsedSeconds);
        // Chỉ tính reward khi VICTORY; map reward lấy theo màn hiện tại.
        boolean victory = "VICTORY".equalsIgnoreCase(status);
        int stars = victory ? currentRewardStars : 0;
        int coins = victory ? currentRewardCoins : 0;
        if (victory) {
            int extraPercent = shopState.getExtraInterestPercent();
            if (extraPercent > 0) {
                coins += (coins * extraPercent / 100);
            }
            shopState.addCoins(coins);
            shopState.addStars(stars);
            syncShopState();
        }
        matchResult = new MatchResult(runId, mode, lvl, mapVersion != null ? mapVersion : "",
                status, score, hpRemain, aiKills, elapsedSeconds, stars, coins);
        if (resultHandler != null) {
            try { resultHandler.onResult(matchResult); }
            catch (Exception ignored) { /* kết quả vẫn hiển thị */ }
        }
        // Kết thúc trận → phase vẫn PLAYING (world status = VICTORY/DEFEAT), chờ R/N.
    }

    @Override public MatchResult matchResult() { return matchResult; }
    @Override public boolean canOpenShop() {
        return sim != null && sim.getWorld().getStatus() == GameWorld.Status.VICTORY;
    }
    @Override public void dispose() {}

    public String getLevelId() { return levelId; }
    public String getRunId() { return runId; }

    // ===== helpers =====
    /** Đọc reward (stars/coins) từ map; null-safe. Trả 0 nếu map không khai báo. */
    private static int[] readRewards(MapDefinition map) {
        int stars = 0, coins = 0;
        if (map != null && map.getRewards() != null) {
            stars = Math.max(0, map.getRewards().getStars());
            coins = Math.max(0, map.getRewards().getCoins());
        }
        return new int[]{stars, coins};
    }

    private static long seedFor(String runId, String levelId) {
        // Deterministic: cùng runId+level → cùng seed → AI reproducible.
        long h = 0;
        String s = runId + ":" + levelId;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        if (h == 0) h = 1;
        return h;
    }

    private static long nextSeq = 1;
    private static long nextSeed() { return nextSeq++; } // monotonic; không Math.random
    private static String nextCampaignLevel(String current) {
        if (GameClient.DEFAULT_LEVEL.equals(current)) return "campaign_2";
        if ("campaign_2".equals(current)) return "campaign_3";
        return null;
    }
}
