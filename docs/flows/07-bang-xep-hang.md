# 07. Bảng xếp hạng

**Tóm tắt:** Khi lưu một trận thắng, server ghi điểm vào bảng `leaderboard`. Mỗi cặp người chơi và màn chỉ có một dòng, giữ điểm cao nhất. Khi xem, server lấy điểm giảm dần, gộp theo tài khoản và lấy 10 dòng đầu.

**File chính:** [LeaderboardService.java](../../server-spring/src/main/java/com/tankzors/application/LeaderboardService.java), [PageController.java](../../server-spring/src/main/java/com/tankzors/web/PageController.java), [LeaderboardController.java](../../server-spring/src/main/java/com/tankzors/web/LeaderboardController.java), [PlayerNames.java](../../server-spring/src/main/java/com/tankzors/identity/PlayerNames.java)

## Sơ đồ

```text
GHI (trong lúc lưu trận thắng, xem [04])
  └─ submit()                            ResultService.java:52
       playerNameFor(playerId)           "user:an" → "an"
       └─ submitScore()                  LeaderboardService.java:43
            chưa có dòng → chèn
            điểm cao hơn → cập nhật
            thấp hơn     → giữ nguyên

ĐỌC
  GET /leaderboard?mapId=campaign_2
  └─ leaderboard()                       PageController.java:160
       └─ topScores()                    LeaderboardService.java:68
            └─ toRows()                  PageController.java:282
                 trả leaderboard.html

  GET /api/leaderboard/campaign_2 (bản JSON)
  └─ topScores()                         LeaderboardController.java:35
```

## Bước 1: Tên hiển thị

`playerId` có dạng `user:an`, tên hiển thị bỏ tiền tố:

[ResultService.java:179-184](../../server-spring/src/main/java/com/tankzors/result/ResultService.java#L179-L184)
```java
private static String playerNameFor(String playerId) {
    String accountName = com.tankzors.identity.PlayerNames.accountName(playerId);
    if (accountName != null) return accountName;
    // Giữ khóa khách cũ để không gộp điểm các phiên khách vào một tên chung.
    return "Player-" + playerId.substring(0, Math.min(6, playerId.length()));
}
```

## Bước 2: Ghi điểm

Chỉ giữ điểm cao nhất của mỗi người trên mỗi màn:

[LeaderboardService.java:45-64](../../server-spring/src/main/java/com/tankzors/application/LeaderboardService.java#L45-L64)
```java
LeaderboardEntry existing = repository.findByPlayerNameAndMapId(playerName, mapId).orElse(null);
if (existing == null) {
    LeaderboardEntry entry = new LeaderboardEntry(playerName, mapId, score, timeSeconds, hpRemain);
    LeaderboardEntry saved = repository.save(entry);
    // ...
    return saved;
}
if (score > existing.getScore()) {
    existing.setScore(score);
    existing.setTimeSeconds(timeSeconds);
    existing.setHpRemain(hpRemain);
    existing.setCreatedAt(Instant.now());
    LeaderboardEntry saved = repository.save(existing);
    // ...
    return saved;
}
// ...
return existing;
```

Database cũng đảm bảo điều này bằng ràng buộc [`leaderboard_unique`](../../server-spring/src/main/resources/db/migration/V1__init.sql#L14) trên cặp `(player_name, map_id)`.

## Bước 3: Đọc 10 người cao nhất

[LeaderboardService.java:68-94](../../server-spring/src/main/java/com/tankzors/application/LeaderboardService.java#L68-L94)
```java
public List<LeaderboardEntry> topScores(String mapId) {
    Map<String, LeaderboardEntry> bestByPlayer = new LinkedHashMap<>();
    for (LeaderboardEntry entry : repository.findByMapIdOrderByScoreDesc(mapId)) {
        String name = entry.getPlayerName();
        String key = "account:" + name;
        if (PlayerNames.isLegacyGeneratedName(name)) {
            // ...
        }
        if (!bestByPlayer.containsKey(key)) {
            // ...
            bestByPlayer.put(key, view);
        }
        if (bestByPlayer.size() == 10) break;
    }
    return new ArrayList<>(bestByPlayer.values());
}
```

Danh sách đã sắp giảm dần, nên dòng đầu tiên gặp của mỗi người là điểm cao nhất của họ.

**Bản ghi cũ** mang tên tự sinh `Player-xxxxxx` từ trước khi đổi sang tên tài khoản. Hàm truy ngược qua bảng `match_result` tìm chủ sở hữu ([dòng 73-82](../../server-spring/src/main/java/com/tankzors/application/LeaderboardService.java#L73-L82)): tìm được đúng một tài khoản thì gộp vào tài khoản đó, không thì hiện "Người chơi cũ". Việc này chỉ áp dụng trên bản sao để hiển thị, dữ liệu gốc không bị sửa.

## Bước 4: Hiển thị

[PageController.java:165-171](../../server-spring/src/main/java/com/tankzors/web/PageController.java#L165-L171)
```java
String safeMapId = mapId != null && LEADERBOARD_MAPS.contains(mapId)
    ? mapId : DEFAULT_LEVEL;
model.addAttribute("maps", LEADERBOARD_MAPS);
model.addAttribute("currentMap", safeMapId);
try {
    model.addAttribute("leaderboardEntries",
        toRows(leaderboardService.topScores(safeMapId)));
```

`mapId` lạ sẽ quay về màn mặc định. Database lỗi thì [trang vẫn hiện](../../server-spring/src/main/java/com/tankzors/web/PageController.java#L172-L178) kèm thông báo. Giao diện ở [leaderboard.html](../../server-spring/src/main/resources/templates/leaderboard.html).

## Dữ liệu thay đổi

| Bảng | Khi nào |
|---|---|
| `leaderboard` | Trận thắng có điểm mới của người chơi trên màn đó, hoặc điểm cao hơn điểm cũ |

## Hạn chế cần biết

- **Điểm do client khai**, server không tính lại, chỉ chặn ngưỡng 1.000.000.
- **Chỉ một đường ghi:** điểm chỉ vào bảng qua `ResultService` (`POST /api/game/results`). [`LeaderboardController`](../../server-spring/src/main/java/com/tankzors/web/LeaderboardController.java) chỉ còn API đọc.

## Test kiểm chứng

- [LeaderboardServiceTest.java](../../server-spring/src/test/java/com/tankzors/application/LeaderboardServiceTest.java): `submitFirstScoreInserts`, `submitLowerScoreKeepsBest`, `submitHigherScoreUpdates`, `topScoresReturnsSorted`
- [LeaderboardNamesTest.java](../../server-spring/src/test/java/com/tankzors/application/LeaderboardNamesTest.java): `recoversLegacyAccountAndKeepsOnlyItsBestScoreWithoutRewritingHistory`, `ambiguousOrMissingLegacyIdentityNeverGuessesAnAccountName`
- [ResultServiceTest.java](../../server-spring/src/test/java/com/tankzors/result/ResultServiceTest.java): `authenticatedResultStoresFullAccountNameAndDoesNotMergeMatchingInitials`
- [LeaderboardControllerTest.java](../../server-spring/src/test/java/com/tankzors/web/LeaderboardControllerTest.java)
