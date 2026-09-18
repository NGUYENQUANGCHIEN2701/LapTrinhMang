# Migration Mapping — Client-Authoritative Tankzors

Tài liệu đối chiếu code thực tế (server-authoritative hiện tại) với thiết kế đích (client-authoritative). Sản phẩm của Bước A trong [CLIENT-AUTHORITATIVE-IMPLEMENTATION-PLAN.md](CLIENT-AUTHORITATIVE-IMPLEMENTATION-PLAN.md). Mọi giá trị số, thứ tự thao tác, hằng số dưới đây đã được đối chiếu trực tiếp với source (cite `file:line` của `server-spring/src/main/java/com/tankzors/`).

> Ghi chú: đây là migration **lần hai**, khác với [migration-mapping.md](migration-mapping.md) (phase-0/1 cũ: Javalin→Spring Boot + React→LibGDX). Lần này chuyển gameplay từ server sang client core.

## 0. Nguyên tắc nền

- Toàn bộ gameplay port vào `com.tankzors.client.*` (bên trong source path `client` của `Tankzors.gwt.xml`) để GWT biên dịch. Code đặt ở `com.tankzors.engine` / `com.tankzors.entity` / `com.tankzors.ai` sẽ **bị GWT loại silently**.
- Core KHÔNG import: `org.springframework.*`, `jakarta.*`, `org.slf4j`, `java.util.concurrent.*`, `java.util.UUID#randomUUID`, `java.util.random.RandomGenerator`, `java.lang.reflect.*`, `com.google.gson.*`, `java.net.*`, `com.badlogic.gdx.backends.*`.
- Core chạy single-thread trên render thread. Mọi side-effect mạng/DB phải qua queue rồi áp dụng trong `render()`.
- Mega-constant parity: giữ nguyên mọi giá trị số ở mục 3 để hành vi campaign không đổi.

## 1. Mapping lớp: engine/server → client core

Bảng liệt kê nơi logic đặt trên server nay chuyển sang. Hàm `GameLoop.step(double dt)` (`engine/GameLoop.java:218-231`) là **canonical deterministic entry point** — chỉ cần mirror đúng nó.

| Lớp server (sẽ bị gỡ/loại) | → Client core mới (`com.tankzors.client.gameplay.*`) | Ghi chú port |
|---|---|---|
| `engine/GameState.java` | `gameplay/GameWorld.java` | Bỏ `ConcurrentHashMap`/`CopyOnWriteArrayList`/`volatile` → `HashMap`/`ArrayList`/field thường |
| `engine/GameLoop.java` (chỉ phần `step`) | `gameplay/GameSimulation.java` (driver fixed-step, KHÔNG scheduler) | Scheduler `ScheduledExecutorService` bỏ; `processInputs` dùng `ArrayDeque` thường |
| `engine/CollisionManager.java` | `gameplay/collision/Collision.java` | Port nguyên vẹn (AABB, inset 4px, wall-slide) |
| `engine/BulletManager.java` | gộp vào `GameWorld` + `entity/Bullet.java` | `BulletManager` hiện chỉ wrap list + log; không cần riêng |
| `entity/Tank.java`, `PlayerTank.java`, `AITank.java` | `gameplay/entity/Tank.java`, `PlayerTank.java`, `AITank.java` | Giữ field/hành vi y nguyên |
| `entity/Bullet.java` | `gameplay/entity/Bullet.java` | Thay `UUID.randomUUID()` bằng counter deterministic |
| `entity/Wall.java`, `Position.java`, `Rectangle.java`, `Direction.java` | `gameplay/entity/*` | Port y nguyên (POJO) |
| `entity/Item.java` | (không port) | Stub rỗng, chưa bao giờ dùng |
| `ai/*` (9 file) | `gameplay/ai/*` | Thay `RandomGenerator`→`java.util.Random`; seed từ match |
| `map/GameMap.java`, `TileType.java` | `gameplay/map/MapDefinition.java`, `TileType.java` | Port `initializeMap()`; thay map loader Gson → `JsonReader` |
| `map/MapLoader.java` | `gameplay/map/MapLoader.java` (GWT-safe) | Đọc `JsonValue` tường minh, không reflection |
| `interfaces/*` (8 file) | giữ các interface thực dùng: `ITank`, `IBullet`, `ICollidable`, `IMap`, `IAIBehavior`, `IUpdatable` | Bỏ interface rỗng/không dùng (`IWeapon`, `IGameObserver`) |
| `application/ScoreCalculator.java` | `gameplay/rules/ScoreCalculator.java` | Công thức y nguyên |
| `engine/MatchEndObserver.java` | `gameplay/result/MatchResultHandler.java` (callback trong simulation) | Bỏ `@Async`/Spring |
| `config/EngineConfig.java`, `EngineLifecycle.java` | (xóa hoàn toàn) | Engine lifecycle chỉ còn ở client |
| `ServerConfig.java` (hằng số `TICK_RATE`) | `gameplay/GameplayConfig.java` | `TICK_RATE=30`, `STEP=1/30` |

### Logic engine thực dùng (đánh dấu dùng/chưa dùng)
- **DÙNG:** `step(dt)` pipeline, `processInputs`, `applyInput` (MOVE/STOP/SHOOT/JOIN/LEAVE), `updateEntities` (player cooldown → movement → AI → bullets → removeInactive), `checkCollisions`, `cleanupDeadTanks` (terminal detection), `fireMatchEnd` (once-guard), `handleJoinGame` (reset).
- **KHÔNG DÙNG/RỖNG:** `ResultMessage` (không bao giờ emit), `Item` (stub), `IGameObserver.onItemCollected` (callback không có), `ScoreCalculator.estimateAiKillsFromAccumulatedScore` (legacy).

## 2. GWT hitlist (toàn bộ construct phải thay)

| Construct | Nơi | Hành động |
|---|---|---|
| `ScheduledExecutorService`, `Executors`, `ScheduledFuture`, `Thread` daemon | `GameLoop.java:61-62,125-137` | Bỏ; driver fixed-step trong `render()` |
| `ConcurrentLinkedQueue<PlayerInput>` | `GameLoop.java:71` | `ArrayDeque` thường |
| `ConcurrentHashMap` (tanks) | `GameState.java:33` | `HashMap` |
| `CopyOnWriteArrayList` (bullets) | `GameState.java:36` | `ArrayList` |
| `AtomicBoolean`, `AtomicInteger` | `GameLoop.java:63,79` | `boolean`/`int` |
| `volatile` (nhiều field) | `GameLoop.java:51-57,81`; `GameState.java:42,45` | bỏ `volatile` |
| `System.nanoTime()` | `GameLoop.java:121,200` | fixed-step dt; không gọi nanoTime trong core |
| `System.currentTimeMillis()` (luật) | `GameLoop.java:569` (matchStartTime) | simulation-time tích lũy, KHÔNG wall-clock |
| `UUID.randomUUID()` (Bullet id) | `Bullet.java:39,51,65,71` | counter `long` tăng dần per-match |
| `RandomGenerator`/`getDefault()` | `PatrolBehavior.java:9,20` | `java.util.Random` (emulated) |
| `new Random()` wall-clock seed | `ShootBehavior.java:24-29` | `new Random(matchSeed)` |
| SLF4J `Logger` | `GameLoop`, `BulletManager`, v.v. | bỏ hoặc `GWT.log`/`System.err` |
| Spring annotations (`@Service @Bean @Async @PostConstruct...`) | `config/*`, `MatchEndService` | không port |
| `CompletableFuture` | `MatchEndService.java:3` | không port |
| Pattern `record`/sealed/Java 14+ syntax | `GridPathfinder.Cell` | class thường với `equals/hashCode` thủ công (GWT 2.10 không dịch) |

## 3. Bảng parity giá trị (KHÔNG đổi)

| Đại lượng | Giá trị | Nguồn |
|---|---|---|
| Tick rate | 30 Hz, `dt = 1/30` s | `ServerConfig.TICK_RATE` |
| Player speed | 150 px/s | `PlayerTank.java:22` |
| Player HP / maxHP | 100 / 100 | `PlayerTank.java:23-24` |
| Player size | 40×40 | `PlayerTank.java:25-26` |
| Player shoot cooldown | 0.5 s | `PlayerTank.java:29` |
| Bullet speed | 300 px/s | `Bullet.java:16` |
| Bullet damage | 25 HP | `Bullet.java:17` |
| Bullet hitbox | 8×8 | `Bullet.java:18` |
| Tank wall-collision inset | 4 px mỗi phía (= box 32×32) | `CollisionManager.TANK_WALL_MARGIN=4` |
| Movement substep | ≤5 px | `PlayerTank` + `AINavigator` |
| Diagonal speed | = cardinal (normalized) | `PlayerTank.updateMovement` |
| Rotation convention | deg, 0=UP, 90=RIGHT, 180=DOWN, 270=LEFT | `Tank.java` |
| Bullet direction | `dx=sin(rad)`, `dy=-cos(rad)` | `Bullet.update` |
| `Rectangle.intersects` | strict overlap (chép cạnh KHÔNG tính) | `Rectangle.java:43-48` |
| AI `SHOOT_RANGE` | 250 px | `ShootBehavior.java:19` |
| Chase hold | ≤100px hold, >200px resume | `ChaseBehavior.java:38-40` |
| Corner nudge | ≤6px off-center, nudge ≤2px | `CollisionManager.java:147-175` |
| Difficulty table | EASY 80/200/0.5/2.0/15°; MEDIUM 90/240/0.5/2.5/10°; HARD 105/280/0.3/2.0/6.0 | `DifficultyLevel.java` (speed/vision/react/cooldown/spread) |

## 4. Pipeline `step(dt)` — giữ đúng thứ tự

```
guard: dt finite > 0
processInputs(): drain inputQueue theo từng command
  - MOVE/MOVE_INPUT: nếu PLAYING & alive → player.setMoving(dir, moving)
  - STOP: player.stopMoving() hoặc setMoving(dir,false)
  - SHOOT/SHOOT_INPUT: nếu PLAYING & alive → tank.shoot() → addBullet
  - JOIN_GAME: reset match + spawn map
  - LEAVE_GAME: remove tank; nếu hết player → WAITING + clear
nếu PLAYING:
  updateEntities(dt):
    (a) mỗi PlayerTank alive: updateCooldown(dt); updateMovement(dt, map)
    (b) mỗi AITank: ai.update(world, dt)
    (c) mỗi bullet active: bullet.update(dt)
    (d) removeInactiveBullets()
  checkCollisions():
    bullet vs wall → deactivate + continue (wall hấp thụ trước)
    bullet vs tank → takeDamage(25); if !alive → addScore(100) + count.kill; break (1 đạn 1 tank)
  cleanupDeadTanks():
    remove dead; hasPlayer = còn PlayerTank; hasAI = còn AITank
    if !hasPlayer → DEFEAT (ưu tiên)        // TIEBREAK cùng-tick
    else if !hasAI → VICTORY
    fireMatchEnd (once-guard bằng matchEndFired)   // chỉ 1 lần
nếu VICTORY/DEFEAT: chỉ broadcast (không physics nữa)
incrementTick()
```

- **Tiebreak:** `if(!hasPlayer)` đứng trước `else if(!hasAI)` → player + AI cuối cùng chết cùng tick ⇒ **DEFEAT** (`GameLoop.java:546 vs :551`). Bắt buộc test.
- **Score** (chỉ VICTORY): `hpRemain*10 + aiKills*200 + max(0, 1000 - elapsedSec*10)`, mỗi hạng clamp ≥0.
- **aiKills** = số AITank thực tế chết, KHÔNG suy từ display score.

## 5. Map schema (GWT-compatible parsing)

JSON phẳng (trích `campaign_1.json`):
```json
{ "name":"...", "difficulty":"EASY", "width":20, "height":15, "tileSize":40,
  "enemies":[{"type":"AI","difficulty":"EASY","spawnX":16,"spawnY":11}],
  "rewards":{"stars":3,"coins":100},
  "grid":[[1,1,...],...,[1,1,...]] }
```
- Toạ độ: `grid[row][col]`; pixel = `(col*tileSize, row*tileSize)`. Spawn tâm ô `(c+0.5)*tileSize`.
- Tile: `0=GROUND,1=WALL,2=WATER,3=SPAWN_PLAYER,4=SPAWN_ENEMY`; ngoài lưới = WALL; WATER walkable=false nhưng bullet đi qua (chỉ WALL chặn bullet).

**Parser client thay Gson:** dùng `com.badlogic.gdx.utils.JsonReader` → `JsonValue`, đọc field tường minh:
```java
JsonValue root = new JsonReader().parse(jsonText);
String name = root.getString("name");
int width = root.getInt("width"), height = root.getInt("height"), tileSize = root.getInt("tileSize", 40);
for (JsonValue e : root.get("enemies")) { ... e.getInt("spawnX") ... }
int[][] tiles = ...; // root.get("grid").get(row).get(col)
```
Logic `initializeMap()` (build walls/spawns từ grid) port nguyên vẹn — thuần arithmetic.

## 6. Contract ranh giới (API nội bộ client)

### 6.1 GameSession (interface–ranh giới)
```java
public interface GameSession {
    void onCommand(PlayerCommand cmd);     // input từ ClientInputProcessor
    void update(float rawDt);              // driver từ GameClient.render (fixed-step bên trong)
    GameStateSnapshot snapshot();          // cho GameRenderer
    String localPlayerId();                // chọn tank đúng cho HUD/co-op
    Status status();                       // WAITING/PLAYING/VICTORY/DEFEAT
    void restart();
    void nextLevel();
    void dispose();
}
```
- Implement: `LocalCampaignSession`, `HostGameSession`, `GuestGameSession`.
- `ClientInputProcessor.Callbacks` (onMove/onShoot/onAllReleased/onRestart/onNextLevel) → `session.onCommand(...)`. Processor giữ nguyên.

### 6.2 PlayerCommand
```java
public final class PlayerCommand {
    public enum Type { MOVE, STOP, SHOOT, RELEASE_ALL }
    public final String playerId;          // localPlayerId của người gửi
    public final Direction direction;      // nullable
    public final boolean moving;
    public final long seq;                 // guest input sequence; 0 cho local/host
}
```

### 6.3 WorldSnapshot (render)
Tái dùng `client.model.{GameStateSnapshot,TankSnapshot,BulletSnapshot,MapSnapshot}` hiện có. `WorldSnapshotMapper` ánh xạ `GameWorld` → snapshot mỗi frame. `GameRenderer.render(snapshot)` giữ nguyên.

### 6.4 MatchResult
```java
public final class MatchResult {
    public final String runId, mode, levelId, mapVersion, status;
    public final int score, hpRemain, aiKills, elapsedSeconds;
}
```
- Overlay UI và payload REST lấy từ **cùng một** MatchResult.

## 7. Bootstrap và REST API

### 7.1 `GET /api/game/bootstrap?levelId=campaign_1`
Response:
```json
{ "protocolVersion":"v2", "playerId":"<app-id>", "levelId":"campaign_1",
  "nextLevelId":"campaign_2", "mapVersion":"<hash>", "map":{...}, "settings":{},
  "unlockedLevels":["campaign_1"], "runId":"<issued>" }
```
- Server trả dữ liệu + kiểm tra level tồn tại/quyền tải; KHÔNG tạo tank/world. `runId` = metadata lần chơi (chống trùng kết quả).

### 7.2 `POST /api/game/results` (endpoint duy nhất, client-reported)
Body = MatchResult (mục 6.4) + runId.
- Body **không** quyết định identity → resolve từ HTTP session qua `PlayerIdentityResolver`.
- Không recompute score, không replay. Validate: enum hợp lệ, số finite/≥0, giới hạn trên hợp lý.
- Idempotency: cùng runId+cùng payload (retry) → trả đã lưu; cùng runId+payload khác → **409**.
- Atomic transaction: receipt + bestScore + unlock cùng commit; lỗi → rollback, KHÔNG trả ACK trước commit.
- Chỉ VICTORY campaign mở màn kế. Co-op KHÔNG unlock campaign.

## 8. Identity (`PlayerIdentityResolver` thống nhất)

- Một bean duy nhất resolve player cho: page (Thymeleaf), progress API, bootstrap, result API, **WS handshake**.
- Dùng `HttpSession` hiện tại làm identity (MVP, không login mới).
- WS `sessionId` chỉ là connectionId; handshake gán connectionId→playerId (identity ứng dụng).
- Hiện trạng碎: REST/PageController dùng `X-Player-Session`/`HttpSession.id`; WS dùng `WebSocketSession.id`; 3 nguồn không đồng nhất → unifier này sửa đúng chỗ.

## 9. Persistence (migration **V3**)

- `V1__init.sql`, `V2__player_progress.sql` đã apply → kế tiếp là **`V3__<name>.sql`**.
- Bảng `match_result` (tạo ở V1, chưa có service ghi) → V3 thêm cột thiếu (`ai_kills`, `map_version`, `run_id` UNIQUE) rồi dùng cho result endpoint.
- `leaderboard` UNIQUE(player_name,map_id) + best-score upsert giữ nguyên (idempotency nương vào đây).
- `player_progress` PK(session_id,unlocked_level), không có bestScore → đổi `session_id` sang identity mới (V3) hoặc giữ ký hiệu cũ + ghi chú.

## 10. Protocol relay v2

Endpoint `/game`, nâng `protocolVersion` v2. Envelope:
```json
{ "version":"v2", "type":"...", "roomId":"...", "runId":"...", "seq":0, "payload":{...} }
```
Server gắn `senderId` từ connection, **không tin** `senderId` client truyền.

| Gói | Ý nghĩa |
|---|---|
| CONNECT | server trả connectionId, playerId, protocolVersion |
| CREATE_ROOM / JOIN_ROOM / LEAVE_ROOM | membership |
| ROOM_STATE / READY | metadata phòng, readiness |
| START_MATCH | chỉ host; server cấp runId + chốt roster/map |
| MATCH_STARTED | cùng runId/mapVersion/roster cho cả 2 client |
| PLAYER_INPUT | guest→relay→host (dir,moving,shoot,seq) |
| WORLD_STATE | host→relay→guest (tick,status,entities) |
| MATCH_RESULT | host→relay→guest+persistence (một lần) |
| PAUSE_MATCH / RESUME_MATCH | chỉ host |
| ROOM_CLOSED / ERROR | lý do/mã lỗi |
| PING / PONG | heartbeat |

Rooms: LOBBY→PLAYING→FINISHED/CLOSED. Tournament: host 30Hz sim, snapshot 15Hz khởi điểm, full snapshot khi start/terminal. Giới hạn khởi điểm: 64 KiB/gói, 60 input/s/conn. Client v1/protocol cũ → ERROR rõ ràng.

## 11. Test migration

**Port sang core (đổi scheduler/Spring setup → step trực tiếp, thêm JUnit 5 dep + test sourceSet vào `core/build.gradle`):**
- `engine/BulletManagerTest`, `CollisionManagerTest`, `CampaignProgressionTest`, `GameLoopRestartTest`, `EndToEndIntegrationTest`
- `entity/PlayerTankTest`
- `ai/AITest`, `ShootBehaviorTest`, `AIMaps` (fixture)
- `map/CampaignMapsTest`, `MapLoaderTest`

**Giữ server (persistence/REST):** `application/LeaderboardServiceTest`, `PlayerProgressServiceTest`, `ScoreCalculatorTest`, `MatchEndServiceTest` (refactor: bỏ recompute engine, test result persistence), `CampaignQueryServiceTest`; `leaderboard/*`, `web/*`.

**Refactor:** `TankzorsApplicationContextTest` — bỏ `gameLoopStarted()` assert `GameLoop` bean; giữ `contextLoads` + relay-wired.

## 12. Thứ tự triển khai (A→F)

A. Mapping + contract (tài liệu này) — **xong khi duyệt**.
B. Port simulation vào core: entity/collision/AI/map + `GameSimulation` (fixed-step) + `ScoreCalculator`; port 11 test sang core; thêm JUnit 5 + test sourceSet.
C. Campaign local hoàn chỉnh: `GameClient`→`LocalCampaignSession` mặc định (KHÔNG connect WS campaign); nối bootstrap/renderer/restart/next; sửa HTML+Desktop launcher theo `mode`.
D. Lưu kết quả + progression: `PlayerIdentityResolver`, run metadata, `POST /api/game/results` + DTO + idempotency + V3 migration; `BrowserResultGateway` retry; bỏ route submit cũ.
E. Host + server relay: `RoomService`/`RoomRegistry` + protocol v2; `HostGameSession`/`GuestGameSession`; lobby Thymeleaf; co-op rules; disconnect handling.
F. Tắt engine server + docs/build: gỡ `EngineLifecycle`/engine beans + caller cũ; xoá `ScoreCalculator` server (sau khi reference hết); cập nhật CODEBASE-GUIDE/protocol-v2/networking docs.

## 13. Giới hạn thiết kế (phải khai báo, không giấu)

- Điểm là **client-reported**, có thể bị sửa — UI bảng điểm phải ghi rõ độ tin cậy.
- Host rời trận (đóng tab/mất mạng/heartbeat quá hạn) ⇒ phòng đóng `HOST_DISCONNECTED`, **không** ghi thua, **không** lưu điểm co-op.
- Không PvP, không matchmaking, không host migration trong MVP.
- Identity theo phiên trình duyệt; không cam kết giữ khi cookie hết hạn.
