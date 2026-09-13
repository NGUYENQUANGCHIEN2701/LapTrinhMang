# AI Implementation Guide - Migrate code hiện tại sang stack mới

## Nhiệm vụ

Migrate đúng các chức năng đang có từ code hiện tại sang stack mới:

```text
Code cũ:
  React + TypeScript + Vite + Canvas
  Java 21 + Maven + Javalin + Gson + WebSocket

Code đích:
  Spring Boot + Thymeleaf
  LibGDX HTML5/GWT
  WebSocket
  PostgreSQL
```

Không thêm gameplay mới trong quá trình migration. Các feature trong PDF là backlog sau khi migration hoàn thành.

## Nguyên tắc migration

1. Migration behavior, không rewrite tùy ý.
2. Mỗi phase phải có phiên bản chạy được.
3. Giữ client React cũ làm test client cho đến cuối Phase 3.
4. Giữ Javalin adapter cho đến khi Spring Boot parity pass.
5. Chỉ engine/domain được dùng chung logic; UI và transport là adapter.
6. Không dùng server entity trực tiếp làm client model.
7. Chỉ chia sẻ DTO/enum/constant của wire protocol.
8. Không gọi database trong `GameLoop` tick.
9. Server authoritative: client gửi input, server quyết định state.
10. Sau mỗi task phải chạy test liên quan và ghi kết quả.

## Target folder structure

```text
LapTrinhMang/
├── server/
│   └── src/main/
│       ├── java/com/tankzors/
│       │   ├── TankzorsApplication.java
│       │   ├── config/
│       │   ├── domain/
│       │   │   ├── entity/
│       │   │   ├── valueobject/
│       │   │   └── rules/
│       │   ├── engine/
│       │   ├── ai/
│       │   ├── map/
│       │   ├── application/
│       │   │   ├── game/
│       │   │   └── leaderboard/
│       │   ├── network/
│       │   │   ├── websocket/
│       │   │   └── protocol/
│       │   └── persistence/
│       └── resources/
│           ├── application.yml
│           ├── templates/
│           └── static/
├── game-client/
│   ├── core/
│   ├── html/
│   ├── desktop/
│   └── assets/
├── shared-protocol/
├── infra/docker-compose.yml
└── .planning/
```

## Phase 0 - Baseline

### Đọc trước

```text
server/src/main/java/com/tankzors/engine/GameLoop.java
server/src/main/java/com/tankzors/engine/GameState.java
server/src/main/java/com/tankzors/network/MessageHandler.java
server/src/main/java/com/tankzors/network/MessageSender.java
server/src/main/java/com/tankzors/network/WebSocketServer.java
server/src/main/java/com/tankzors/network/protocol/*.java
client/src/core/Renderer.ts
client/src/core/GameLoop.ts
client/src/core/GameCanvas.tsx
client/src/core/network/*.ts
```

### Việc cần làm

- [x] Chạy `mvn -f server\pom.xml test`. -> 112 pass
- [x] Chạy `npm test` và `npm run build`. -> 62 tests + build pass
- [x] Ghi sample của connect, join, input, state, ACK, PONG, error. -> `docs/protocol-v1.md`
- [x] Ghi behavior cần giữ: map, controls, tick rate, HP, AI, victory/defeat. -> `docs/parity-checklist.md`

### Output

Tạo `docs/protocol-v1.md` hoặc file tương đương, không thay đổi behavior. -> Xong 13/09/2026. Thêm `docs/parity-checklist.md`.

## Phase 1 - Tạo target structure

### Việc cần làm

- [x] Tạo Spring Boot module/entry point nhưng chưa migrate endpoint. -> `server-spring/` xong (Application context + `/game` placeholder + `/api/health`, 2 tests pass)
- [x] Tạo LibGDX `core`, `html`, `desktop` skeleton. -> `game-client/` xong (Gradle multi-project, 3 marker class)
- [x] Tạo `shared-protocol` chỉ chứa DTO/enum/constant wire format. -> `shared-protocol/` xong (15 class + 7 tests pass)
- [x] Tạo `infra/docker-compose.yml` cho PostgreSQL nhưng chưa bắt buộc dùng trong game. -> xong (postgres:16-alpine)
- [x] Tạo package đích và mapping class cũ -> class mới. -> `docs/migration-mapping.md`
- [x] Chưa xóa client React, Javalin hoặc Maven game engine cũ. -> Xác nhận: server/ legacy 112 tests PASS, client/ 62 tests PASS.

### Nghiệm thu

- Mỗi module build được độc lập hoặc có README build rõ ràng. -> OK (mỗi module đều có README + build pass)
- Không có code cũ bị xóa. -> OK
- Có target structure đúng như tài liệu. -> OK (matching ROADMAP.md mục Phase 1)

## Phase 2 - Migrate backend

### Mapping

```text
GameLoop          -> engine
GameState         -> engine/domain
Tank/Bullet       -> domain/entity
CollisionManager  -> engine/rules
AI classes        -> ai
MapLoader         -> map
MessageHandler    -> network/websocket adapter
MessageSender     -> network/websocket publisher adapter
WebSocketServer   -> Spring WebSocket configuration/handler
LeaderboardService-> application/leaderboard
```

### Việc cần làm

1. Tách output abstraction để `GameLoop` không nhận `MessageSender` cụ thể. ✅ `engine.StatePublisherPort`
2. Tách input port để WebSocket handler chỉ enqueue `InputMessage`. ✅ `application.GameSessionService` + `network.InputPacketParser`
3. Di chuyển protocol DTO vào shared protocol hoặc package protocol độc lập. ⏳ defer (Phase 4/5)
4. Tạo Spring Boot WebSocket endpoint `/game`. ✅ `network.WebSocketConfig`
5. Tạo Spring health endpoint `/api/health`. ✅ `web.HealthController` port 8081
6. Tạo `GameSessionService` quản lý session và player mapping. ✅
7. Inject game engine bằng Spring configuration/service. ✅ `config.EngineConfig` + `config.EngineLifecycle`
8. Giữ game loop single-threaded và tick rate hiện tại. ✅ single-thread `GameLoop-Thread`, đo 31-32 tick/s
9. Chạy React client cũ với Spring Boot server. ✅ 13/09/2026: parity PASS (CONNECT+ACK+STATE tanks=2)
10. Chỉ gỡ Javalin sau khi join/move/shoot/leave parity pass. ⏳ Phase 5

### Không được làm

- Không chuyển game rule vào controller. ✅ chỉ routing trong `GameWebSocketHandler`
- Không thêm database call vào tick. ✅ chưa có DB
- Không đổi logic AI/collision. ✅ 112 legacy tests pass nguyên
- Không đổi protocol nếu chưa cập nhật compatibility. ✅ wire format v1 untouched

### Nghiệm thu

- Spring Boot start. ✅ Started in 1.9s
- React cũ connect được `/game`. ✅ verified manual với ws client Node
- Join, movement, shoot, bullet, collision, HP, AI và result giữ nguyên. ✅ 115 tests pass + 3 context test pass
- Server tests pass. ✅

## Phase 3 - Migrate gameplay client

### Mapping

```text
Renderer.ts       -> LibGDX GameRenderer
GameLoop.ts       -> LibGDX render(delta)
SpriteManager.ts  -> LibGDX AssetManager
InputHandler.ts   -> InputProcessor/input polling
GameCanvas.tsx    -> LibGDX GameScreen
GameStateData     -> client snapshot model
WebSocketClient   -> HTML/GWT WebSocket bridge
```

### Thứ tự

1. Chạy map nền bằng LibGDX desktop.
2. Load asset hiện tại.
3. Render map.
4. Render player tank và enemy tank.
5. Render bullet và HP.
6. Thêm keyboard input.
7. Kết nối WebSocket tới Spring Boot.
8. Parse snapshot server.
9. Cập nhật entity từ snapshot.
10. Thêm animation/effect đang có.
11. Build HTML5 output.
12. Nhúng output vào Thymeleaf page.

### Nghiệm thu

- Browser mở được LibGDX HTML5.
- Game có behavior tương đương React Canvas.
- WebSocket input/state hoạt động.
- Client không tự quyết định damage, score hoặc victory.

## Phase 4 - Migrate website và PostgreSQL

### Website

- [ ] Tạo Thymeleaf layout.
- [ ] Tạo main menu.
- [ ] Tạo game selection/campaign selection hiện có.
- [ ] Tạo trang play chứa LibGDX canvas.
- [ ] Tạo leaderboard page.

### Database

- [ ] Chạy PostgreSQL bằng Docker Compose.
- [ ] Tạo datasource config bằng environment variables.
- [ ] Tạo schema tối thiểu cho leaderboard và match result hiện có.
- [ ] Tách repository interface khỏi PostgreSQL adapter.
- [ ] Migrate dữ liệu leaderboard hiện tại nếu có.
- [ ] Test restart server không mất dữ liệu.

Không triển khai shop, research, save slot nâng cao hoặc inventory trong phase này.

## Phase 5 - Cutover

- [ ] Chạy end-to-end website -> LibGDX -> WebSocket -> Spring Boot -> GameLoop.
- [ ] Test nhiều browser.
- [ ] Test reconnect, leave và server shutdown.
- [ ] So sánh React client với LibGDX client.
- [ ] Xóa legacy React/Javalin chỉ khi parity checklist pass.
- [ ] Cập nhật README và sơ đồ kiến trúc.

## Test checklist

### Server

- [ ] Unit test entity.
- [ ] Unit test collision/bullet.
- [ ] Unit test AI.
- [ ] Unit test map loader.
- [ ] WebSocket integration test.
- [ ] Spring Boot context test.
- [ ] PostgreSQL repository test.

### Client

- [ ] LibGDX desktop smoke test.
- [ ] LibGDX HTML5 build.
- [ ] Browser load test.
- [ ] Input test.
- [ ] Snapshot rendering test.

## Definition of Done

- [ ] Chức năng hiện tại được giữ nguyên.
- [ ] Website chạy bằng Spring Boot + Thymeleaf.
- [ ] Gameplay chạy bằng LibGDX HTML5.
- [ ] Realtime chạy bằng WebSocket.
- [ ] Leaderboard hiện có lưu PostgreSQL.
- [ ] Game engine không phụ thuộc trực tiếp vào transport/UI/database.
- [ ] Full test pass.
- [ ] Legacy code chỉ bị xóa sau parity verification.

## Những thứ để TODO sau

- Shop.
- Research tree.
- Save/load nhiều slot.
- Practice procedural map.
- Online Levels mở rộng.
- Vũ khí đặc biệt.
- Tank/AI nâng cao.
- Fog of war, radar, boss, ally tank.
- Touch/mobile optimization.
