# 04. Kết thúc trận và lưu kết quả

**Tóm tắt:** Khi trận kết thúc, client tính điểm, tự cộng thưởng vào ví, đồng bộ ví rồi gửi `POST /api/game/results` kèm `runId`. Server dùng `runId` để phân biệt 3 trường hợp: kết quả mới, gửi lại y hệt, hoặc cùng `runId` nhưng nội dung khác. Nhờ vậy dù client gửi lại nhiều lần do mạng chập chờn, kết quả chỉ được ghi và cộng thưởng một lần.

**File chính:** [LocalCampaignSession.java](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java), [GwtAppBridge.java](../../game-client/html/src/main/java/com/tankzors/client/html/GwtAppBridge.java), [GameApiController.java](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java), [ResultService.java](../../server-spring/src/main/java/com/tankzors/result/ResultService.java)

## Sơ đồ

```text
fireMatchEnd()                           GameSimulation.java:1577
  └─ onMatchEnd()                        LocalCampaignSession.java:641
       ├─ calculate()                    ScoreCalculator.java:11      tính điểm
       ├─ thắng: cộng Xu, Sao vào ví, syncShopState()                 xem [06]
       └─ resultHandler.onResult(matchResult)
            └─ submitResult()            GwtAppBridge.java:21
                 └─ postJsonWithRetry()  GwtAppBridge.java:85         POST /api/game/results

Server:
  └─ submitResult()                      GameApiController.java:148
       identity.resolve(request)
       └─ submit()                       ResultService.java:52
            validate(payload)            sai → 400
            hash = FNV-1a của nội dung
            tìm theo runId:
              ├─ đã có, cùng hash   → 200 alreadySaved=true, không đổi gì
              ├─ đã có, khác hash   → 409
              └─ chưa có:
                   clampReward(), chèn match_result
                   nếu VICTORY và CAMPAIGN:
                     markVictory()      khoá màn, mở màn kế      xem [05]
                     submitScore()      cập nhật xếp hạng        xem [07]
                     creditTotals()     cộng Sao/Xu vào player_totals
                   200 saved
```

## Bước 1: Client chốt kết quả

[LocalCampaignSession.java:641-663](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L641-L663)
```java
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
```

Công thức điểm:

[ScoreCalculator.java:11-16](../../game-client/core/src/main/java/com/tankzors/client/gameplay/rules/ScoreCalculator.java#L11-L16)
```java
public static int calculate(int hpRemain, int aiKills, int timeSeconds) {
    int hp = Math.max(0, hpRemain) * 10;
    int kill = Math.max(0, aiKills) * 200;
    int timeBonus = Math.max(0, 1000 - Math.max(0, timeSeconds) * 10);
    return hp + kill + timeBonus;
}
```

Trận thua cũng được gửi lên, với Sao và Xu bằng 0.

## Bước 2: Gửi đi, lỗi thì thử lại

JSON được [ghép tay](../../game-client/html/src/main/java/com/tankzors/client/html/GwtAppBridge.java#L44-L61) vì GWT không có sẵn thư viện JSON như trên server. Khi gửi:

[GwtAppBridge.java:88-98](../../game-client/html/src/main/java/com/tankzors/client/html/GwtAppBridge.java#L88-L98)
```java
var xhr = new XMLHttpRequest();
xhr.open("POST", url, true);
xhr.setRequestHeader("Content-Type", "application/json");
xhr.onreadystatechange = $entry(function() {
    if (xhr.readyState !== 4) return;
    var ok = (xhr.status >= 200 && xhr.status < 300) || xhr.status === 409;
    if (!ok && attempt < 3) {
        $wnd.setTimeout(function() {
            self.@com.tankzors.client.html.GwtAppBridge::postJsonWithRetry(Ljava/lang/String;Ljava/lang/String;I)(url, body, attempt + 1);
        }, 1000 * (attempt + 1));
    } else if (!ok) {
```

- Lỗi thì thử lại tối đa 3 lần, chờ 1 giây, 2 giây, 3 giây. Một kết quả có thể tới server tối đa 4 lần.
- Coi 409 là "đã xong", không thử lại vì thử nữa cũng vẫn 409.
- Hết lượt thử thì chỉ ghi log, người chơi không thấy thông báo.

Vì vậy server **bắt buộc** phải chịu được việc nhận trùng. Đó là việc của bước 4.

## Bước 3: Controller nhận yêu cầu

[GameApiController.java:148-152](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java#L148-L152)
```java
public ResponseEntity<Map<String, Object>> submitResult(
        @RequestBody ResultService.ResultPayload payload,
        HttpServletRequest request) {
    String playerId = identity.resolve(request);
    ResultService.SubmitOutcome out = resultService.submit(payload, playerId);
```

`playerId` lấy từ phiên đăng nhập, không lấy từ thân yêu cầu. Hai loại lỗi được chuyển thành mã HTTP ở [cuối file](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java#L168-L177): `ConflictException` → 409, `IllegalArgumentException` → 400.

## Bước 4: Phân ba nhánh

Đây là phần cốt lõi của cơ chế chống ghi trùng.

[ResultService.java:55-67](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L55-L67)
```java
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
```

| Tình huống | Hash | Kết quả | Dữ liệu |
|---|---|---|---|
| `runId` mới | | 200, `alreadySaved=false` | Chèn và cộng thưởng |
| `runId` đã có | Giống | 200, `alreadySaved=true` | Không đổi |
| `runId` đã có | Khác | 409 | Không đổi |

- [`validate`](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L143-L163) kiểm tra các trường có hợp lệ và không vượt ngưỡng (ví dụ điểm trong khoảng 0 đến 1.000.000).
- [`payloadHash`](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L166-L177) băm nội dung kết quả bằng FNV-1a 64 bit, không gồm `runId`.
- Chốt chặn cuối là ràng buộc [`match_result_run_unique`](../../server-spring/src/main/resources/db/migration/V3__match_result_dedup.sql#L16) trong database: mỗi `run_id` chỉ có một dòng.

## Bước 5: Kẹp phần thưởng

Client tự khai Sao và Xu, nên server không tin mà đối chiếu với tệp bản đồ của chính nó:

[ResultService.java:119-130](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L119-L130)
```java
private static int[] clampReward(ResultPayload payload, MatchResultEntity row) {
    boolean victory = "VICTORY".equalsIgnoreCase(payload.status)
            && "CAMPAIGN".equalsIgnoreCase(row.getMode());
    if (!victory) return new int[]{0, 0};
    GameMap map = MapLoader.load(payload.levelId);
    if (map == null || map.getRewards() == null) return new int[]{0, 0};
    int maxStars = Math.max(0, map.getRewards().getStars());
    int maxCoins = Math.max(0, map.getRewards().getCoins());
    int stars = Math.max(0, Math.min(payload.starsEarned, maxStars));
    int coins = Math.max(0, Math.min(payload.coinsEarned, maxCoins));
    return new int[]{stars, coins};
}
```

## Bước 6: Chỉ trận thắng mới có tác dụng phụ

[ResultService.java:99-110](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L99-L110)
```java
if ("VICTORY".equalsIgnoreCase(payload.status) && "CAMPAIGN".equalsIgnoreCase(row.getMode())) {
    // Màn vừa thắng bị khoá; danh sách trả về là các màn còn được chơi.
    progressService.markVictory(playerId, payload.levelId);
    unlocked = progressService.getUnlocked(playerId);
    bestScore = leaderboardService.submitScore(
            playerNameFor(playerId), payload.levelId,
            payload.score, payload.elapsedSeconds, payload.hpRemain).getScore();
    // Credit totals đúng 1 lần: gate bởi insert row UNIQUE(runId).
    int[] totals = creditTotals(playerId, reward[0], reward[1]);
    totalStars = totals[0];
    totalCoins = totals[1];
}
```

Toàn bộ `submit` nằm trong [một transaction](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L51). Lỗi giữa chừng thì không có gì được ghi.

Client không dùng phản hồi của API này: `postJsonWithRetry` chỉ xét mã trạng thái.

## Dữ liệu thay đổi

| Bảng | Thay đổi | Khi nào |
|---|---|---|
| `match_result` | Chèn một dòng | Mọi kết quả mới, kể cả thua |
| `player_progress` | Khoá màn, mở màn kế | Thắng |
| `leaderboard` | Thêm hoặc nâng điểm | Thắng |
| `player_totals` | Cộng Sao/Xu | Thắng |

## Hạn chế cần biết

Các điểm sau đọc được trực tiếp từ code, chưa dựng kịch bản khai thác để xác nhận:

- **`runId` không được đối chiếu.** [Bootstrap sinh `runId`](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java#L72) nhưng không lưu, nên `submit` chấp nhận mọi `runId` mới. Cơ chế này chống gửi lại, không chống tự tạo kết quả.
- **`submit` không kiểm tra màn đang chơi.** Một yêu cầu thắng tự tạo vẫn được xử lý như lượt mới.
- **Điểm không bị kẹp theo bản đồ**, chỉ chặn ngưỡng 1.000.000. Server không tính lại điểm.
- **Thứ tự sync và results.** Client gọi sync ví trước khi gửi kết quả, cả hai đều bất đồng bộ. Nếu sync được xử lý trước, `creditTotals` cộng thêm lần nữa lên số đã gồm thưởng, cho tới lần sync kế tiếp. Xem [06](06-cua-hang-va-tui-do.md).
- **Gửi đồng thời cùng `runId`.** Ràng buộc UNIQUE chỉ cho một dòng thành công, nhưng lỗi vi phạm chưa được xử lý riêng nên yêu cầu còn lại nhiều khả năng nhận lỗi 500. Hiện chưa có test đồng thời.

## Test kiểm chứng

- [ResultServiceTest.java](../../server-spring/src/test/java/com/tankzors/result/ResultServiceTest.java): `retrySameRunIdPayloadDoesNotDoubleCredit`, `retrySameRunIdDifferentPayloadConflicts`, `clientOverreportClampedToMapRewards`, `defeatDoesNotCredit`, `victoryCreditsTotalsAndStoresRewardRow`
- [LocalCampaignSessionTest.java](../../game-client/core/src/test/java/com/tankzors/client/session/LocalCampaignSessionTest.java): `victoryMatchResultCarriesMapReward`
