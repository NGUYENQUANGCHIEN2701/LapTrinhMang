package com.tankzors.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tankzors.map.GameMap;
import com.tankzors.map.MapLoader;
import com.tankzors.progress.*;
import com.tankzors.result.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Validates account transactions; gameplay stays client-authoritative. */
@Service
public class PlayerShopService {
    public static final String INITIAL_INVENTORY = "{\"0\":99,\"19\":3,\"17\":2}";
    public record ShopData(int totalCoins, int totalStars, String researchJson, String inventoryJson, long revision) {}
    public record Operation(String type, Integer itemId, String techId, Integer amount, Integer col, Integer row) {}
    public record SyncRequest(String runId, String requestId, Long expectedRevision, List<Operation> operations) {}
    private final PlayerTotalsRepository totals;
    private final PlayerShopStateRepository states;
    private final ShopRequestRepository requests;
    private final GameRunService runs;
    private final PlayerStateLock lock;
    private final EconomyCatalog catalog;
    private final ObjectMapper mapper;
    public PlayerShopService(PlayerTotalsRepository totals, PlayerShopStateRepository states, ShopRequestRepository requests,
                             GameRunService runs, PlayerStateLock lock, EconomyCatalog catalog, ObjectMapper mapper) {
        this.totals = totals; this.states = states; this.requests = requests; this.runs = runs;
        this.lock = lock; this.catalog = catalog; this.mapper = mapper;
    }
    @Transactional
    public ShopData getShopData(String playerId) {
        PlayerTotals wallet = lock.acquire(playerId);
        PlayerShopState state = state(playerId);
        return view(wallet, state);
    }
    private PlayerShopState state(String playerId) {
        return states.findByPlayerId(playerId).orElseGet(() -> states.save(new PlayerShopState(playerId, "{}", INITIAL_INVENTORY)));
    }
    @Transactional
    public ShopData beginRun(String playerId) {
        PlayerTotals wallet = lock.acquire(playerId);
        PlayerShopState state = state(playerId);
        Map<String, Integer> inventory = parse(state.getInventoryJson());
        boolean hasAmmo = false;
        for (int id = 0; id <= 14; id++) if (inventory.getOrDefault(String.valueOf(id), 0) > 0) hasAmmo = true;
        // Account recovery policy: an empty arsenal must not permanently trap a new campaign.
        if (!hasAmmo && wallet.getTotalCoins() < 5) {
            inventory.put("0", 99); state.setInventoryJson(json(inventory)); wallet.advanceRevision();
        }
        return view(wallet, state);
    }
    @Transactional
    public ShopData syncShopData(String playerId, SyncRequest req) {
        if (req == null || req.runId() == null || req.runId().length() > 120 || req.requestId() == null
                || !req.requestId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") || req.expectedRevision() == null
                || req.operations() == null || req.operations().isEmpty() || req.operations().size() > 1000)
            throw new IllegalArgumentException("Giao dịch không hợp lệ; cần runId, requestId, expectedRevision và operations");
        PlayerTotals wallet = lock.acquire(playerId);
        GameRun run = runs.requireOwner(req.runId(), playerId);
        String key = playerId + ":" + req.requestId();
        String hash = hash(req);
        ShopRequest previous = requests.findById(key).orElse(null);
        PlayerShopState state = state(playerId);
        if (previous != null) {
            if (!previous.getPlayerId().equals(playerId) || !previous.getPayloadHash().equals(hash))
                throw new ResultService.ConflictException("requestId đã có nội dung khác");
            return view(wallet, state);
        }
        runs.requireActive(run);
        if (req.expectedRevision() != wallet.getRevision())
            throw new ResultService.ConflictException("Dữ liệu đã thay đổi ở phiên khác. Tải lại trang.");
        Map<String, Integer> inventory = parse(state.getInventoryJson());
        Map<String, Integer> research = parse(state.getResearchJson());
        Map<String, Integer> floor = parse(run.getShopFloor());
        Set<String> claims = readClaims(run.getClaimedPickups());
        for (Operation op : req.operations()) {
            if (op == null || op.type() == null) throw new IllegalArgumentException("Thiếu loại giao dịch");
            switch (op.type()) {
                case "OPEN" -> { floor.clear(); floor.putAll(inventory); floor.put("_open", 1); }
                case "BUY", "SELL" -> trade(op, wallet, inventory, research, floor);
                case "RESEARCH" -> {
                    int[] prices = catalog.research(op.techId());
                    int level = research.getOrDefault(op.techId(), 0);
                    if (level >= prices.length || wallet.getTotalStars() < prices[level]) throw new IllegalArgumentException("Không đủ Sao hoặc đã nghiên cứu tối đa");
                    wallet.setTotalStars(wallet.getTotalStars() - prices[level]); research.put(op.techId(), level + 1);
                }
                case "CONSUME", "INSTALL" -> {
                    int id = itemId(op), amount = amount(op);
                    if (id != -3) catalog.item(id);
                    if (id == -1 || id == 22 || id == 23) throw new IllegalArgumentException("Trang bị không tiêu hao");
                    String item = String.valueOf(id);
                    if (inventory.getOrDefault(item, 0) < amount) throw new IllegalArgumentException("Không đủ vật phẩm");
                    if ("INSTALL".equals(op.type())) {
                        if (id != 20 || inventory.getOrDefault("-3", 0) + amount > catalog.limit(catalog.item(20), research, inventory))
                            throw new IllegalArgumentException("Khiên không hợp lệ");
                        inventory.merge("-3", amount, Integer::sum);
                    }
                    inventory.put(item, inventory.getOrDefault(item, 0) - amount);
                }
                case "REPAIR", "REPAIR_ALLIES" -> {
                    int missing = amount(op);
                    int rate = research.getOrDefault("armorResearch", 0) >= 3
                            ? ("REPAIR".equals(op.type()) ? 3 : 2) : ("REPAIR".equals(op.type()) ? 6 : 5);
                    debit(wallet, missing * rate);
                }
                case "PICKUP" -> {
                    if (run.getFinishedAt() != null) throw new IllegalArgumentException("Lượt chơi đã kết thúc");
                    pickup(op, run, wallet, inventory, claims);
                }
                default -> throw new IllegalArgumentException("Loại giao dịch không hợp lệ");
            }
        }
        state.setInventoryJson(json(inventory)); state.setResearchJson(json(research));
        state.setUpdatedAt(java.time.Instant.now());
        run.setShopFloor(json(floor)); run.setClaimedPickups(json(claims));
        wallet.advanceRevision(); totals.save(wallet); states.save(state);
        requests.save(new ShopRequest(key, playerId, req.runId(), hash));
        return view(wallet, state);
    }
    private void trade(Operation op, PlayerTotals wallet, Map<String, Integer> inventory,
                       Map<String, Integer> research, Map<String, Integer> floor) {
        EconomyCatalog.Item item = catalog.item(itemId(op));
        catalog.requireUnlocked(item, research, inventory);
        String key = String.valueOf(item.id()); int count = inventory.getOrDefault(key, 0);
        int price = catalog.price(item, research, inventory);
        if ("BUY".equals(op.type())) {
            int received = Math.min(item.pack(), catalog.limit(item, research, inventory) - count);
            if (received <= 0 || wallet.getTotalCoins() < price) throw new IllegalArgumentException("Không đủ Xu hoặc đã đủ vật phẩm");
            debit(wallet, price - price * (item.pack() - received) / item.pack());
            inventory.put(key, count + received);
        } else {
            if (floor.getOrDefault("_open", 0) != 1 || Set.of(-1, -2, 21, 22, 23).contains(item.id())
                    || count - item.pack() < floor.getOrDefault(key, 0))
                throw new IllegalArgumentException("Chỉ bán lại vật phẩm mua trong lần mở cửa hàng này");
            wallet.setTotalCoins(Math.addExact(wallet.getTotalCoins(), price)); inventory.put(key, count - item.pack());
        }
    }
    private void pickup(Operation op, GameRun run, PlayerTotals wallet, Map<String, Integer> inventory, Set<String> claims) {
        if (op.col() == null || op.row() == null) throw new IllegalArgumentException("Thiếu ô vật phẩm");
        GameMap map = MapLoader.load(run.getLevelId());
        int x = op.col(), y = op.row();
        if (map == null || x < 0 || y < 0 || y >= map.getGrid().length || x >= map.getGrid()[y].length)
            throw new IllegalArgumentException("Ô vật phẩm không hợp lệ");
        String cell = x + ":" + y;
        if (!claims.add(cell)) throw new ResultService.ConflictException("Vật phẩm đã được ghi nhận");
        switch (map.getGrid()[y][x]) {
            case 7, 8, 10 -> { /* transient gameplay effects, no account credit */ }
            case 11 -> wallet.setTotalStars(Math.addExact(wallet.getTotalStars(), 1));
            case 12 -> wallet.setTotalCoins(Math.addExact(wallet.getTotalCoins(), 35));
            case 14 -> inventory.merge("-2", 1, Integer::sum);
            case 13 -> {
                JsonNode mission = mapper.valueToTree(map.getMission());
                JsonNode boxes = mission.path("boxes");
                if (boxes.isArray()) for (JsonNode box : boxes) {
                    if (box.path("x").asInt(-1) == x && box.path("y").asInt(-1) == y) {
                        int id = box.path("itemId").asInt(-1), count = box.path("count").asInt(0);
                        if (id >= 0 && count > 0) { catalog.item(id); inventory.merge(String.valueOf(id), count, Math::addExact); }
                        break;
                    }
                }
            }
            default -> throw new IllegalArgumentException("Ô không có vật phẩm");
        }
    }
    private static int itemId(Operation op) {
        if (op.itemId() == null) throw new IllegalArgumentException("Thiếu vật phẩm"); return op.itemId();
    }
    private static int amount(Operation op) {
        if (op.amount() == null || op.amount() < 1 || op.amount() > 10000) throw new IllegalArgumentException("Số lượng không hợp lệ"); return op.amount();
    }
    private static void debit(PlayerTotals wallet, int amount) {
        if (amount < 0 || wallet.getTotalCoins() < amount) throw new IllegalArgumentException("Không đủ Xu");
        wallet.setTotalCoins(wallet.getTotalCoins() - amount);
    }
    private Map<String, Integer> parse(String value) {
        try { return mapper.readValue(value, new TypeReference<TreeMap<String, Integer>>() {}); }
        catch (Exception e) { throw new IllegalStateException("Dữ liệu shop không hợp lệ", e); }
    }
    private Set<String> readClaims(String value) {
        try { return mapper.readValue(value, new TypeReference<TreeSet<String>>() {}); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String hash(SyncRequest req) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(req).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static ShopData view(PlayerTotals wallet, PlayerShopState state) {
        return new ShopData(wallet.getTotalCoins(), wallet.getTotalStars(), state.getResearchJson(), state.getInventoryJson(), wallet.getRevision());
    }
    public int extraInterestPercent(String playerId) {
        int level = parse(state(playerId).getResearchJson()).getOrDefault("extraInterest", 0);
        return Math.min(2, Math.max(0, level)) * 5;
    }
}
