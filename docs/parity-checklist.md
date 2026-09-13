# Parity checklist - Tankzors Pro migration

Checklist behavior phải giữ nguyên qua mỗi phase migration
(Javalin -> Spring Boot, React Canvas -> LibGDX HTML5, in-memory ->
PostgreSQL). Mỗi item có Manual test (MT) và/или Automated test (AT) ở
baseline Phase 0.

Ký hiệu:
- [ ] chưa verify sau phase_migration.
- [x] đã verify sau phase này (đánh dấu khi hoàn thành).

Cross-reference: chi tiết wire format xem `docs/protocol-v1.md`.

## Kiểm tra baseline (Phase 0 - đã PASS)

| Suite | Số test | Kết quả | Ghi chú |
|---|---|---|---|
| `mvn -f server/pom.xml test` | 112 | PASS | Unit + integration Java |
| `npm test` (vitest) | 62 | PASS | Unit + integration TS |
| `npm run build` | 1 bundle | PASS | 228.09 KB JS, 0.25 KB CSS |

Server test classes (mẫu):

- `CollisionManagerTest` - AABB tank wall, sliding, corner nudge.
- `BulletManagerTest` - bullet lifecycle.
- `PlayerTankTest` - 14 case, multi-direction + cooldown.
- `EndToEndIntegrationTest` - 6 case, join -> kill AI -> VICTORY.
- `GameLoopRestartTest` - reset trận khi player join thứ 2.
- `CampaignProgressionTest` + `CampaignMapsTest` + `MapLoaderTest` - 3
  campaign maps load đúng cấu hình enemies.
- `ShootBehaviorTest`, `AITest`.

Client test files:

- `__tests__/Renderer.test.ts` - render map/tank/bullet.
- `__tests__/EndToEndIntegration.test.ts` - join -> state.
- `__tests__/CampaignProgression.test.ts` - unlock kèo server STATE.

Các test này là safety net -> server Spring Boot và LibGDX client sau
Phase 2/3 phải pass tương đương.

## 1. Connect / Disconnect

- [x] MT: client mở `ws://localhost:8080/game` -> nhận `CONNECT` với
  `sessionId`.
- [x] MT: server log "Client đã kết nối" + connection count tăng.
- [x] MT: client chủ động `disconnect()` -> server log "Client đã ngắt
  kết nối" -> state dọn player tank -> статус `WAITING`.
- [x] MT: mất mạng -> React client thử kết nối lại 3 lần cách nhau 2s
  (`WebSocketClient.maxRetries = 3`).
- [x] AT: `GameLoopRestartTest` assert reset match khi player mới vào sau
  khi player cũ rời.

**Parity Phase 2 yêu cầu**: Spring Boot endpoint `/game` phải send
`CONNECT` với `sessionId` giống hệt format v1.

## 2. JOIN_GAME / Match lifecycle

- [x] MT: gửi JOIN_GAME với levelId mới -> server load map mới, setCurrentMap,
  reset enemies + bullets, setStatus PLAYING, spawn player tại SPAWN_PLAYER
  spawn.
- [x] MT: JOIN_GAME với cùng levelId khi đang PLAYING và player còn sống ->
  không reset.
- [x] MT: JOIN_GAME khi player đã chết (hp<=0 hoặc không có PlayerTank) ->
  reset trận.
- [x] MT: JOIN_GAME trong mode "campaign" -> nếu có player khác (session
  khác) thì remove tank cũ, chỉ giữ 1 player duy nhất.
- [x] AT: `EndToEndIntegrationTest` cover reset khi status không phải
  PLAYING.
- [x] AT: `CampaignMapsTest` load đủ 3 campaign maps.

**Parity Phase 2 yêu cầu**: `GameSessionService` ở Spring Boot phải tái
lập chính xác flow "shouldResetMatch" và thứ tự:

1. Remove player cũ (campaign).
2. Load map mới nếu level thay đổi.
3. shouldResetMatch = `levelChanged OR status != PLAYING OR no alive
   player`.
4. clear enemies+bullets -> spawn map enemies -> set PLAYING.
5. Reset PlayerTank tại spawn.
6. broadcastState ngay.

## 3. Movement

- [ ] MT: bấm W -> MOVE_INPUT UP moving=true -> tank đi lên.
- [ ] MT: nhả W -> MOVE_INPUT UP moving=false -> tank dừng đi lên nhưng
  nếu D vẫn giữ thì tiếp tục phải.
- [ ] MT: giữ W + D -> tank đi chéo; tốc độ chéo = tốc độ cardinal
  (vector normalized).
- [ ] MT: tank áp tường không xuyên; có thể trượt dọc.
- [ ] MT: tank không vượt biên map.
- [ ] MT: tank không đi vào ô WATER.
- [ ] AT: `PlayerTankTest` 14 case + `CollisionManagerTest`.

**Parity constant**: `PlayerTank.DEFAULT_SPEED = 150.0`, diagonal
normalize, corner nudge ≤6px, margin 4px.

## 4. Shoot / Bullet / Cooldown

- [ ] MT: bấm Space -> SHOOT_INPUT -> tank bắn 1 viên.
- [ ] MT: giữ Space không spam được (client chỉ gửi 1 lần onkeydown,
  không repeat).
- [ ] MT: bắn liên tục bị cooldown 0.5s chặn; tank chỉ bắn lại sau khi
  cooldown hết.
- [ ] MT: đạn đi thẳng theo `rotation` của tank; ko xoay curve.
- [ ] MT: đạn chạm wall (tile 1) -> biến mất ngay trong tick đó.
- [ ] MT: đạn chạm biên map -> biến mất.
- [ ] MT: đạn đi qua ô WATER/SPAWN_* ko bị mất.
- [ ] MT: đạn của A không trúng A (ownerId == tank.id bị skip).
- [ ] AT: `BulletManagerTest`, `EndToEndIntegrationTest`.

**Parity constant**: `Bullet.DEFAULT_SPEED = 300.0`,
`DEFAULT_DAMAGE = 25`, kích thước 8x8, công thức `(sin, -cos)`.

## 5. HP / Damage / Victory / Defeat

- [ ] MT: tank bị trúng đạn -> hp -= damage (25). HP clamp 0.
- [ ] MT: hp <= 0 -> alive=false, tick sau bị removal khỏi state.
- [ ] MT: thanh HP client (player HUD + mini bar trên tank) cập nhật grade
  màu xanh (>60%) / vàng (30-60%) / đỏ (<30%).
- [ ] MT: PlayerTank chết -> status=DEFEAT -> STATE broadcast.
- [ ] MT: toàn bộ AITank chết -> status=VICTORY -> STATE broadcast.
- [ ] MT: STATE sau Victory/Defeat vẫn được phát để client hiển thị modal.
- [ ] AT: `EndToEndIntegrationTest` case kill AI -> VICTORY, case leave ->
  WAITING.

**Parity Phase 2**: trạng thái VICTORY/DEFEAT phải tuân theo thứ tự
`cleanupDeadTanks` xóa tank -> kiểm tra hasPlayer/hasAI -> setStatus ->
broadcastState.

## 6. AI

- [ ] MT: AI idle cho tới khi nhìn thấy player (<visionRange và
  LineOfSight clear).
- [ ] MT: AI có reaction delay trước khi đuổi (EASY 0.5s, HARD 0.3s).
- [ ] MT: AI đuổi theo vị trí lastSeen; khi hết sight tiếp tục tìm; hết
  tìm thì patrol.
- [ ] MT: AI bắn khi < 250px và clear LOS và hết shootCooldown.
- [ ] MT: độ chính xác giảm theo difficulty (EASY ±15°, HARD ±6°).
- [ ] AT: `AITest`, `ShootBehaviorTest`, `AIMaps`.

**Parity constant**: bảng `DifficultyLevel` (speed/vision/reaction/
shootCooldown/spread), shoot range `ShootBehavior.SHOOT_RANGE = 250`,
`LineOfSight.hasClearShot`.

## 7. Map loading

- [ ] MT: cả 3 campaign map load được, wymiary 20x15 tileSize 40.
- [ ] MT: spawn player tại ô SPAWN_PLAYER (pixel `(col+0.5)*tileSize`).
- [ ] MT: spawn AI theo `enemies[]` metadata, fallback SPAWN_ENEMY ô.
- [ ] MT: AI spawn không chồng wall -> nếu chồng thì skip và log warning.
- [ ] AT: `CampaignMapsTest`, `MapLoaderTest` (6 case).

## 8. UI / Rendering (React Canvas hiện tại -> LibGDX sau)

- [ ] MT: Canvas 800x600, vẽ map 20x15 tileSize 40.
- [ ] MT: tile 0/3/4 vẽ ground, tile 1 vẽ wall, tile 2 vẽ spikes overlay,
  tile 5 (future bush) vẽ bush (hiện chưa có map nào dùng 5).
- [ ] MT: sprite tank 16 frame, 22.5° mỗi frame, frame index
  `((rotation+11.25)/22.5 + 8) % 16`.
- [ ] MT: bullet sprite 24x24 render tại tâm `bullet.x/y`, rotate theo
  direction.
- [ ] MT: HUD player HP panel góc trên-trái, mini health bar trên mỗi tank.
- [ ] MT: hiệu ứng nổ (explosion_big hoặc gradient fallback) 600ms.
- [ ] AT: `Renderer.test.ts`.

**Parity Phase 3 yêu cầu**: LibGDX GameRenderer dùng cùng sprite sheet,
cùng frame mapping, cùng tile mapping, cùng màu fallback.

## 9. Campaign progression (Client-side localStorage)

- [ ] MT: vào lần đầu chỉ campaign_1 mở.
- [ ] MT: thắng campaign_1 -> campaign_2 được unlock và ghi vào
  `localStorage['tankzors_unlocked_levels']`.
- [ ] MT: restart trình duyệt -> unlocked persist nhờ localStorage.
- [ ] MT: navigate tiếp/previous/restart giữ được levelId.
- [ ] AT: `CampaignProgression.test.ts`.

> Lưu ý quan trọng: progression hiện NẰM Ở CLIENT. Migration sang Postgres
> (Phase 4) phải rename strategy cẩn thận, vì nếu server chưa lưu thì
>_fm reset localStorage sẽ mất unlock. Trong phạm vi Phase 2 Migration
> backend, KO được chuyển progression sang server nếu không có field wire
> mới (protocol-v2).

## 10. Leaderboard / Persistence

- [x] `LeaderboardService.java` hiện rỗng (placeholder) - chưa có wire
  flow. Phase 0 baseline: không có test tích hợp leaderboard.
- [ ] Phase 4: PostgreSQL repository phải pass test restart-không-mất-data
  riêng.

**Parity Phase 4 yêu cầu**: chỉ triển khai sau khi schema đã chốt. Không
đưa ghi DB vào `GameLoop.step()`.

## Phụ lục - Phân loại thay đổi được phép trong từng phase

| Phase | Được phép | Không được |
|---|---|---|
| 0 (current) | Viết doc, không sửa code | Thay đổi behavior |
| 1 | Tạo skeleton mới, mapping class | Xoá code legacy, đổi wire format |
| 2 | Migrate backend, đổi Javalin -> Spring Boot | Đổi AI/collision, đổi protocol-v1 |
| 3 | Migrate canvas -> LibGDX HTML5 | Thay đổi input mapping, tick rate |
| 4 | Postgres + Thymeleaf page | Đưa DB call vào GameLoop.tick |
| 5 | Xoá React/Javalin legacy | Bỏ parity test trước khi pass |
