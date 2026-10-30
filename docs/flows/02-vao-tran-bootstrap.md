# 02. Vào trận: từ `/play` đến khi xe xuất hiện

**Tóm tắt:** Trang `/play` nhúng game vào một iframe. Game tải xong thì gọi `GET /api/game/bootstrap`. Server kiểm tra quyền, trả về bản đồ, `runId` và ví của người chơi. Client dựng mô phỏng rồi hiện màn hình nhiệm vụ, người chơi bấm Enter thì trận bắt đầu.

**File chính:** [PageController.java](../../server-spring/src/main/java/com/tankzors/web/PageController.java), [play.html](../../server-spring/src/main/resources/templates/play.html), [HtmlLauncher.java](../../game-client/html/src/main/java/com/tankzors/client/html/HtmlLauncher.java), [GameClient.java](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java), [LocalCampaignSession.java](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java), [GameApiController.java](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java)

## Sơ đồ

```text
Trình duyệt: GET /play?levelId=campaign_2
  └─ play()                              PageController.java:183
       kiểm tra màn tồn tại và còn được chơi
       trả play.html chứa <iframe src="/game/libgdx/index.html?levelId=...">

Iframe tải index.html → tải bundle GWT → chạy game
  └─ createApplicationListener()         HtmlLauncher.java:41
       đọc levelId từ URL, tạo LocalCampaignSession
  └─ create()                            GameClient.java:95
       tải asset, tạo renderer, input
       └─ start()                        LocalCampaignSession.java:214
            └─ requestBootstrap()        LocalCampaignSession.java:509
                 └─ requestLevel()       HtmlLauncher.java:130
                      XMLHttpRequest GET /api/game/bootstrap?levelId=...

Server:
  └─ bootstrap()                         GameApiController.java:58
       identity.resolve → isUnlocked → MapLoader.load → sinh runId → đọc ví
       trả JSON

Client nhận JSON:
  └─ onLevelLoadedWithShop()             LocalCampaignSession.java:552
       nạp ví một lần
       └─ onLevelLoaded()                LocalCampaignSession.java:564
            tạo GameSimulation mới, gắn callback kết thúc trận
            └─ startCampaignLevel()      GameSimulation.java:187
                 đặt xe, sinh địch, nạp nhiệm vụ
            mở briefing, phase = PLAYING
```

## Bước 1: Server trả trang chơi

Hàm `play()` kiểm tra rồi mới cho vào. `levelId` trống nghĩa là người chơi bấm Continue, luồng đó nằm ở [05](05-tien-trinh-va-chuyen-man.md).

[PageController.java:203-223](../../server-spring/src/main/java/com/tankzors/web/PageController.java#L203-L223)
```java
CampaignLevelView level = campaignQueryService.getLevel(levelId).orElse(null);
if (level == null) {
    // ...
    return "redirect:/campaigns";
}
try {
    if (!progressService.isUnlocked(playerId, levelId)) {
        redirectAttributes.addFlashAttribute("flashError",
            "Màn chơi này đã hoàn thành hoặc chưa được mở. Vui lòng chọn màn hiện tại.");
        return "redirect:/campaigns";
    }
} catch (Exception ex) {
    return progressUnavailable(ex, redirectAttributes);
}
pageModelFactory.apply(model, "Play - " + level.getName(), "play");
model.addAttribute("selectedLevelId", level.getId());
// ...
return "play";
```

## Bước 2: Trang nhúng game vào iframe

`levelId` và âm lượng được truyền qua URL của iframe:

[play.html:34-36](../../server-spring/src/main/resources/templates/play.html#L34-L36)
```html
<iframe id="game-frame"
        class="game-frame"
        th:src="@{/game/libgdx/index.html(levelId=${selectedLevelId},soundVol=${settings != null ? settings.soundVolume : 80},musicVol=${settings != null ? settings.musicVolume : 70})}"
```

`/game/libgdx/**` là file tĩnh do Spring phục vụ, được sinh ra lúc build (xem [08](08-build-va-trien-khai.md)). [index.html](../../game-client/html/src/main/webapp/index.html#L50) nạp bundle GWT.

## Bước 3: Game khởi động

Khi bundle chạy, LibGDX gọi `createApplicationListener()`. Đây là nơi "lắp ráp" game:

[HtmlLauncher.java:56-65](../../game-client/html/src/main/java/com/tankzors/client/html/HtmlLauncher.java#L56-L65)
```java
if ("campaign".equalsIgnoreCase(mode)) {
    BootstrapMapSource mapSource = new BootstrapMapSource();
    // ResultHandler: kết quả VICTORY gửi REST (Bước D BrowserResultGateway).
    LocalCampaignSession session = new LocalCampaignSession(
            browserPlayerId(), levelId, mapSource,
            (MatchResult r) -> { GwtAppBridge.get().submitResult(r); return true; });
    session.setShopSyncHandler((coins, stars, researchJson, inventoryJson) -> {
        GwtAppBridge.get().syncShop(coins, stars, researchJson, inventoryJson);
    });
    client.setSession(session);
```

Hai callback ở đây rất quan trọng: chúng nối phần game (không biết gì về mạng) với lớp gọi mạng. Kết quả trận đi qua `submitResult`, ví đi qua `syncShop`.

Sau đó LibGDX gọi `GameClient.create()`, tải hình ảnh, âm thanh, tạo renderer và bộ xử lý phím, cuối cùng [bắt đầu phiên chơi](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java#L269).

## Bước 4: Gửi yêu cầu bootstrap

[LocalCampaignSession.java:509-517](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L509-L517)
```java
private void requestBootstrap(String targetLevelId) {
    if (inFlight && targetLevelId.equals(pendingLevelId)) {
        return; // cùng request đã bay, không duplicate.
    }
    pendingLevelId = targetLevelId;
    inFlight = true;
    phase = Phase.LOADING_NEXT;
    requestToken++;
    final int myToken = requestToken;
```

- `inFlight`: chặn gửi trùng khi người chơi bấm nhiều lần.
- `requestToken`: mỗi yêu cầu có một số thứ tự. Phản hồi của yêu cầu cũ về muộn sẽ [bị bỏ qua](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L530), không ghi đè màn đang chơi.

Vì mã Java của game không gọi trực tiếp được API trình duyệt, `HtmlLauncher` dùng JSNI (viết JavaScript ngay trong Java) để gọi `XMLHttpRequest`:

[HtmlLauncher.java:138-145](../../game-client/html/src/main/java/com/tankzors/client/html/HtmlLauncher.java#L138-L145)
```java
var xhr = new XMLHttpRequest();
xhr.open("GET", url, true);
xhr.onreadystatechange = $entry(function() {
    if (xhr.readyState !== 4) return;
    if (xhr.status === 200) {
        try {
            var resp = JSON.parse(xhr.responseText);
            var mapJson = JSON.stringify(resp.map);
```

Thành công thì gọi `onLevelLoadedWithShop`, thất bại thì gọi `onLoadError` (hiện lỗi tải, giữ nguyên trạng thái cũ).

## Bước 5: Server xử lý bootstrap

[GameApiController.java:61-73](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java#L61-L73)
```java
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
```

| Kết quả | Khi nào |
|---|---|
| 403 | Màn chưa mở hoặc đã thắng rồi |
| 404 | Không có file bản đồ |
| 200 | Trả JSON gồm `map`, `runId`, `mapVersion`, `nextLevelId`, `unlockedLevels`, `totalCoins`, `totalStars`, `researchJson`, `inventoryJson` |

[MapLoader.load](../../server-spring/src/main/java/com/tankzors/map/MapLoader.java#L19) đọc file `maps/<levelId>.json` trong tài nguyên của Spring. Server chỉ coi đây là dữ liệu, không tạo đối tượng game nào. Ví được đọc qua [`getShopData`](../../server-spring/src/main/java/com/tankzors/application/PlayerShopService.java#L38).

`runId` **không được lưu trên server**. Client giữ nó và gửi lại khi nộp kết quả.

## Bước 6: Client dựng trận

[LocalCampaignSession.java:556-560](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L556-L560) nạp ví đúng một lần cho cả phiên, để màn sau không bị ghi đè bằng dữ liệu cũ:

```java
if (!shopStateLoaded) {
    shopState.loadState(totalCoins, totalStars, researchJson, inventoryJson);
    shopStateLoaded = true;
}
onLevelLoaded(loadedLevelId, mapJson, newRunId, newMapVersion);
```

Rồi `onLevelLoaded` dựng mô phỏng mới. Bản đồ được parse trước, chỉ khi hợp lệ mới thay thế mô phỏng hiện tại:

[LocalCampaignSession.java:566-598](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L566-L598)
```java
GameSimulation next = new GameSimulation(seedFor(
        newRunId != null ? newRunId : "local-" + loadedLevelId, loadedLevelId));
next.setShopState(shopState);
MapDefinition nextMap = MapLoader.parse(mapJson);
if (nextMap == null) {
    // ...
    return;
}
// Atomic swap.
this.sim = next;
this.levelId = loadedLevelId;
// ...
sim.setMatchEndHandler(this::onMatchEnd);
// ...
sim.startCampaignLevel(loadedMap, levelId, playerId);
openBriefing();
levelStartInventory = shopState.snapshotInventory();
levelStartWeaponId = shopState.getActiveWeaponId();
inventoryMenuOpen = false;
phase = Phase.PLAYING;
inFlight = false;
```

- `seedFor`: sinh seed cho bộ số ngẫu nhiên [từ `runId` và màn](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L721-L728). Cùng seed thì AI hành xử giống nhau.
- `setMatchEndHandler`: đăng ký hàm được gọi khi trận kết thúc, nối sang [04](04-luu-ket-qua-tran.md).
- `levelStartInventory`: chụp lại túi đồ đầu màn, dùng khi chơi lại sau khi thua.

[GameSimulation.java:187-197](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L187-L197) đặt mọi thứ lên bản đồ:

```java
public void startCampaignLevel(MapDefinition map, String levelId, String playerId) {
    this.localPlayerId = playerId;
    this.currentLevelId = levelId != null ? levelId : map.getName();
    this.coopMode = false;
    world.setCurrentMap(map);
    resetMatch();
    spawnPlayer(playerId);
    spawnMapEnemies();
    setupMission();
    world.setStatus(GameWorld.Status.PLAYING);
}
```

## Bước 7: Màn hình nhiệm vụ

Nếu bản đồ có lời dặn, [`openBriefing`](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L457-L460) bật cờ `briefingOpen`. Mô phỏng đứng yên vì [`update` chỉ chạy khi cờ này tắt](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L278). Bấm Enter thì `onContinue` gọi [`dismissBriefing`](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java#L207-L211), trận bắt đầu và chuyển sang [03](03-vong-lap-game.md).

## Các trạng thái của phiên chơi

```text
INIT ──start()──> LOADING_NEXT ──thành công──> PLAYING (briefing → chơi → VICTORY/DEFEAT)
                       │                          │
                    thất bại                 chơi lại / màn kế
                       ▼                          │
                  LOAD_ERROR <────────────────────┘ (quay lại LOADING_NEXT)
```

Xem [`enum Phase`](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L60) và [`status()`](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L445-L454).

## Dữ liệu thay đổi

| Bảng | Khi nào |
|---|---|
| `player_progress` | Người chơi mới: [`getUnlocked`](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java#L47-L54) tự chèn `campaign_1` ở lần đầu |

## Lưu ý

- `/play` nằm trong danh sách công khai (`/play/**`), còn `/api/game/**` phải đăng nhập. Trong luồng thường người chơi đã qua `/campaigns` hoặc `/game-menu` (phải đăng nhập) nên đã có phiên.

## Test kiểm chứng

- [GameApiControllerTest.java](../../server-spring/src/test/java/com/tankzors/web/GameApiControllerTest.java): `bootstrapRejectsLevelAlreadyWon`, `bootstrapPassesMissionScriptThrough`
- [LocalCampaignSessionTest.java](../../game-client/core/src/test/java/com/tankzors/client/session/LocalCampaignSessionTest.java): `asyncBootstrapStartsSessionAndAcceptsMovementInput`, `invalidBootstrapMapKeepsSessionWaitingThenLoadError`, `staleCallbackDoesNotOverwriteCurrentSession`, `briefingHoldsSimulationUntilDismissed`
- [PageControllerTest.java](../../server-spring/src/test/java/com/tankzors/web/PageControllerTest.java): `explicitCompletedAndLockedLevelsRedirectBeforeLoadingGame`
