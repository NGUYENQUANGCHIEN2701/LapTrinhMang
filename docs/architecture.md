# Kiến trúc Tankzors hiện tại

## Ranh giới trách nhiệm

### LibGDX client

Client là nguồn sự thật của trạng thái trận đấu. `GameSimulation` chạy fixed-step, cập nhật tank, AI, đạn, collision, điểm và trạng thái thắng/thua. `GameRenderer` chỉ vẽ snapshot; `ClientInputProcessor` chỉ phát lệnh vào session.

### Spring Boot server

Server không có tank/entity/AI/GameLoop. Server chỉ:

- xác thực và phân quyền;
- render website Thymeleaf;
- cấp map và `runId` qua REST;
- kiểm tra, chống ghi trùng và lưu kết quả;
- quản lý progress, leaderboard, admin;
- relay packet co-op giữa host và guest.

### PostgreSQL

Flyway quản lý schema. JPA lưu user, match result, progress, totals và leaderboard.

## Luồng campaign

```text
play.html
  -> iframe /game/libgdx/index.html
  -> HtmlLauncher
  -> LocalCampaignSession
  -> GET /api/game/bootstrap
  -> MapDefinition + GameSimulation
  -> GameRenderer
  -> POST /api/game/results
  -> ResultService -> PostgreSQL
```

`/game/libgdx` là namespace static. WebSocket gameplay `/game` không còn tồn tại.

## Luồng co-op

```text
HostGameSession -- WORLD_STATE/MATCH_RESULT --> /coop relay --> GuestGameSession
GuestGameSession -- PLAYER_INPUT -----------> /coop relay --> HostGameSession
```

Host chạy simulation; relay chỉ xác thực membership/quyền host và chuyển tiếp packet. Phần ghép launcher/game frame với lobby vẫn là hạng mục cần hoàn thiện; relay và session không được hiểu là engine phía server.

## Cấu trúc code chính

```text
game-client/core/.../gameplay/   simulation, AI, collision, entity, map, rules
game-client/core/.../session/    local campaign, host, guest
game-client/core/.../render/     render snapshot
game-client/html/                browser launcher + REST bridge
game-client/desktop/             desktop development launcher

server-spring/.../web/           MVC và REST controllers
server-spring/.../application/   use cases/query services
server-spring/.../result/        lưu và chống trùng kết quả
server-spring/.../progress/      tiến trình campaign
server-spring/.../leaderboard/   bảng xếp hạng
server-spring/.../security/      auth/roles/admin
server-spring/.../network/       relay /coop
server-spring/.../room/          room registry
server-spring/.../map/           DTO/loader map, không simulation
```

## Quy tắc phụ thuộc

- Gameplay mới chỉ đặt trong `game-client/core`.
- Server có thể đọc map metadata nhưng không tạo gameplay entity.
- Renderer không sửa world state.
- Campaign browser dùng REST, không dùng WebSocket.
- Không sửa bundle generated trong `static/game/libgdx` bằng tay.

## Tính toàn vẹn dữ liệu tài khoản

`game_run` lưu lượt đã cấp và owner/level/version/hạn dùng. `PlayerStateLock` khóa row ví theo tài khoản trước khi kiểm tra/ghi trong các use case. `ResultService` ghi kết quả, cộng thưởng, cập nhật progress/leaderboard trong cùng transaction. `PlayerShopService` áp dụng journal theo catalog và revision, không nhận snapshot số dư tùy chọn.

Browser dùng `CampaignPersistence` và một outbox chung cho shop/result, gửi tuần tự, retry giữ nguyên body và chờ xác nhận trước khi next. SessionStorage hỗ trợ khôi phục ghi khi reload cùng tab, không lưu world. Chi tiết ở [networking](networking.md).
