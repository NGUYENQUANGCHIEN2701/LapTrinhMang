# 05. Tiến trình: mở màn, Continue, chơi lại, sang màn kế

**Tóm tắt:** Tiến trình lưu trong bảng `player_progress`. Mỗi dòng là một màn đã mở của người chơi, cột `completed = true` nghĩa là đã thắng và bị khoá. "Màn được chơi" là các màn đã mở mà chưa thắng. Quy tắc mỗi màn chơi một lần được chặn ở cả server lẫn client.

**File chính:** [PlayerProgressService.java](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java), [PageController.java](../../server-spring/src/main/java/com/tankzors/web/PageController.java), [LocalCampaignSession.java](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java), [GameClient.java](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java)

## Ví dụ dữ liệu

Người chơi `user:an` đã thắng màn 1 và đang ở màn 2:

| session_id | unlocked_level | completed |
|---|---|---|
| user:an | campaign_1 | true |
| user:an | campaign_2 | false |

Cột `session_id` thực chất chứa `playerId`.

Hàm nền tảng của cả luồng:

[PlayerProgressService.java:47-60](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java#L47-L60)
```java
public synchronized List<String> getUnlocked(String sessionId) {
    List<PlayerProgress> rows = repository.findBySessionId(sessionId);
    if (rows.isEmpty()) {
        // First-touch: insert campaign_1 default
        repository.save(new PlayerProgress(sessionId, "campaign_1"));
        logger.info("[PlayerProgress] init session={} -> campaign_1", sessionId);
        return new ArrayList<>(List.of("campaign_1"));
    }
    return rows.stream()
        .filter(row -> !row.isCompleted())
        .map(PlayerProgress::getUnlockedLevel)
        .sorted(Comparator.comparingInt(PlayerProgressService::orderOf))
        .collect(Collectors.toList());
}
```

Thứ tự màn cố định ở [`CAMPAIGN_ORDER`](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java#L35-L37).

## Luồng A: Xem danh sách màn

```text
GET /campaigns
  └─ campaigns()                         PageController.java:110
       getAvailableLevels()              danh sách 3 màn cố định
       resolveUnlockedLevels()           màn còn chơi được
       resolveCompletedLevels()          màn đã thắng
       trả campaigns.html
```

Danh sách 3 màn nằm trong [StaticCampaignQueryService.java](../../server-spring/src/main/java/com/tankzors/application/StaticCampaignQueryService.java#L19-L26). [campaigns.html:24-25](../../server-spring/src/main/resources/templates/campaigns.html#L24-L25) chỉ hiện nút Bắt đầu cho màn đang mở:

```html
th:if="${unlockedLevels.contains(lvl.id)}"
th:href="@{/play(levelId=${lvl.id})}">▶ BẮT ĐẦU</a>
```

## Luồng B: Nút Continue

```text
GET /game-menu
  └─ gameMenu()                          PageController.java:89
       getContinueLevel()                PlayerProgressService.java:79
       có màn → bật nút, hiện "Tiếp tục <tên màn>"
       không còn màn → tắt nút, hiện "đã hoàn thành toàn bộ chiến dịch"

Bấm Continue → GET /play (không có levelId)
  └─ play()                              PageController.java:183
       tính lại getContinueLevel()
       hết màn → redirect /campaigns kèm thông báo
```

Continue lấy màn xa nhất đang mở:

[PlayerProgressService.java:79-84](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java#L79-L84)
```java
public synchronized Optional<String> getContinueLevel(String sessionId) {
    List<String> unlocked = getUnlocked(sessionId);
    return CAMPAIGN_ORDER.stream()
        .filter(unlocked::contains)
        .reduce((previous, current) -> current);
}
```

Nút trong [game-menu.html:22](../../server-spring/src/main/resources/templates/game-menu.html#L22) trỏ tới `/play` **không kèm levelId**. Server tính lại lúc bấm, vì trang menu có thể đã cũ nếu người chơi vừa thắng ở tab khác:

[PageController.java:190-199](../../server-spring/src/main/java/com/tankzors/web/PageController.java#L190-L199)
```java
String playerId = identity.resolve(request);
try {
    if (levelId == null || levelId.isBlank()) {
        levelId = progressService.getContinueLevel(playerId).orElse(null);
        if (levelId == null) {
            redirectAttributes.addFlashAttribute("flashSuccess",
                "Bạn đã hoàn thành toàn bộ chiến dịch.");
            return "redirect:/campaigns";
        }
    }
```

Nếu database lỗi, [`progressUnavailable`](../../server-spring/src/main/java/com/tankzors/web/PageController.java#L226-L231) đưa về `/game-menu` kèm thông báo, không tự đoán là màn 1.

## Luồng C: Thắng, mở màn kế

Được gọi từ `ResultService.submit` ([04](04-luu-ket-qua-tran.md)).

[PlayerProgressService.java:92-117](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java#L92-L117)
```java
public synchronized String markVictory(String sessionId, String levelId) {
    // Ensure session initialized
    getUnlocked(sessionId);
    Map<String, PlayerProgress> rows = repository.findBySessionId(sessionId).stream()
        .collect(Collectors.toMap(PlayerProgress::getUnlockedLevel, Function.identity()));
    PlayerProgress won = rows.get(levelId);
    if (won == null) {
        won = new PlayerProgress(sessionId, levelId);
    }
    if (!won.isCompleted()) {
        won.setCompleted(true);
        repository.save(won);
        // ...
    }
    int idx = CAMPAIGN_ORDER.indexOf(levelId);
    if (idx < 0 || idx >= CAMPAIGN_ORDER.size() - 1) {
        return null;
    }
    String nextLevel = CAMPAIGN_ORDER.get(idx + 1);
    if (!rows.containsKey(nextLevel)) {
        repository.save(new PlayerProgress(sessionId, nextLevel));
        // ...
        return nextLevel;
    }
    return null;
}
```

Thắng màn cuối thì không mở thêm màn nào.

## Luồng D: Sau khi thắng, trên màn hình kết quả

```text
Trận thắng → status = VICTORY → GameRenderer vẽ VictoryOverlay
Enter / Space → victoryAction()          GameClient.java:64
  ├─ action 1 "Tiếp tục" → continueVictory()   GameClient.java:55
  │     mở cửa hàng, đặt cờ continueAfterWorkshop
  │     đóng cửa hàng → onShopClosed()         GameClient.java:115
  │        └─ nextLevel()                      LocalCampaignSession.java:493
  │             └─ requestBootstrap(màn kế)    nhận runId mới
  └─ action 2 "Về danh sách màn" → handler → /campaigns
```

[VictoryOverlay.java:34-37](../../game-client/core/src/main/java/com/tankzors/client/ui/VictoryOverlay.java#L34-L37) đổi phím thành hành động. Màn cuối không còn màn kế thì [`continueVictory`](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java#L55-L62) quay về danh sách màn sau khi rời cửa hàng.

`nextLevel` chỉ chạy khi đã thắng:

[LocalCampaignSession.java:493-503](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L493-L503)
```java
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
```

Nếu lưu kết quả đã thất bại, server chưa mở màn kế nên bootstrap trả 403 và người chơi thấy lỗi tải (`LOAD_ERROR`).

## Luồng E: Thua, chơi lại

```text
Trận thua → status = DEFEAT → GameRenderer vẽ DefeatOverlay
R → defeatAction(1)                      GameClient.java:49
  └─ restart()                           LocalCampaignSession.java:471
       khôi phục túi đồ đầu màn
       requestBootstrap(cùng màn)        nhận runId mới
Esc → defeatAction(2) → về /campaigns
```

[LocalCampaignSession.java:471-483](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L471-L483)
```java
public void restart() {
    // Mỗi màn chỉ chơi một lần: thắng rồi thì màn bị khoá, không cho chơi lại.
    if (phase == Phase.PLAYING && sim.getWorld().getStatus() == GameWorld.Status.VICTORY) return;
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
```

Mỗi lần chơi lại có `runId` mới, nên kết quả lượt sau không bị coi là trùng lượt trước.

## "Mỗi màn chơi một lần" được chặn ở đâu

| Lớp | Code | Tác dụng |
|---|---|---|
| Database | [V7__player_progress_completed.sql](../../server-spring/src/main/resources/db/migration/V7__player_progress_completed.sql#L4) | Lưu trạng thái đã thắng |
| Trang web | [campaigns.html](../../server-spring/src/main/resources/templates/campaigns.html#L24-L25) | Không hiện nút Bắt đầu |
| Trang `/play` | [PageController.java:211](../../server-spring/src/main/java/com/tankzors/web/PageController.java#L211) | Chuyển về danh sách màn |
| API | [GameApiController.java:63](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java#L63) | Bootstrap trả 403 |
| Client | [LocalCampaignSession.java:473](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L473) | `restart()` bị bỏ qua sau khi thắng |

## Lưu ý

- `POST /api/progress/reset` ([ProgressController.java:52-57](../../server-spring/src/main/java/com/tankzors/web/ProgressController.java#L52-L57)) đặt lại tiến trình về màn 1 nhưng không thu hồi Sao/Xu.
- `submit` không dùng các kiểm tra trên: chúng chỉ chặn ở bước vào màn (xem hạn chế ở [04](04-luu-ket-qua-tran.md)).

## Dữ liệu thay đổi

| Bảng | Khi nào |
|---|---|
| `player_progress` | Lần đầu đọc (chèn `campaign_1`), khi thắng, khi reset |

## Test kiểm chứng

- [PlayerProgressServiceTest.java](../../server-spring/src/test/java/com/tankzors/application/PlayerProgressServiceTest.java): `victoryLevel1UnlocksLevel2`, `victoryLocksCompletedLevelSoItIsPlayedOnce`, `finishingWholeCampaignLeavesNothingPlayable`, `continueChoosesFurthestPlayableLevelFromLegacyUnlockedRows`
- [PageControllerTest.java](../../server-spring/src/test/java/com/tankzors/web/PageControllerTest.java): `continueOpensCurrentAccountLevel2`, `completedCampaignDisablesContinueInsteadOfReopeningLevel1`
- [ContinueProgressFailureTest.java](../../server-spring/src/test/java/com/tankzors/web/ContinueProgressFailureTest.java): database lỗi thì không đoán màn 1
- [LocalCampaignSessionTest.java](../../game-client/core/src/test/java/com/tankzors/client/session/LocalCampaignSessionTest.java): `restartAfterVictoryIsBlockedBecauseLevelIsPlayedOnce`, `restartRebootstrapIssuesNewRunId`, `nextLevelIsBlockedUntilVictory`
