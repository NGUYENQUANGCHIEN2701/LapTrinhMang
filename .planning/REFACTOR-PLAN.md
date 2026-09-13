# Kế hoạch refactor và migrate code hiện tại

## Mục tiêu

Chuyển các chức năng đang có sang stack mới, đồng thời tổ chức lại codebase để dễ mở rộng. Không thêm gameplay mới trong đợt này.

```text
Code hiện tại:
React + TypeScript + Vite + Canvas
Java 21 + Maven + Javalin + Gson + WebSocket

Code đích:
Spring Boot + Thymeleaf
LibGDX HTML5/GWT
WebSocket
PostgreSQL
```

## Chức năng phải giữ nguyên

- Connect/disconnect WebSocket.
- Join campaign/level.
- Movement và stop.
- Shooting và cooldown.
- Bullet movement.
- Collision với map/tank.
- HP, victory, defeat.
- AI hiện tại.
- Map JSON hiện tại.
- Campaign progression hiện có.
- Leaderboard hiện có.

## Chưa làm trong migration

- Shop.
- Research tree.
- Save slot nâng cao.
- Practice procedural map.
- Online Levels mới.
- Vũ khí/tank/AI mới.
- Fog of war, radar, boss, ally tank.

## Cấu trúc đích

```text
LapTrinhMang/
├── server/                         # Spring Boot + authoritative game server
│   └── src/main/java/com/tankzors/
│       ├── TankzorsApplication.java
│       ├── config/
│       ├── domain/
│       │   ├── entity/
│       │   ├── valueobject/
│       │   └── rules/
│       ├── engine/
│       ├── ai/
│       ├── map/
│       ├── application/
│       │   ├── game/
│       │   ├── campaign/
│       │   └── leaderboard/
│       ├── network/
│       │   ├── websocket/
│       │   └── protocol/
│       └── persistence/
├── game-client/                    # LibGDX
│   ├── core/
│   ├── html/
│   ├── desktop/
│   └── assets/
├── shared-protocol/                # DTO/enum/constant wire protocol
├── infra/docker-compose.yml
└── .planning/
```

## Dependency rule

```text
shared-protocol <- server
shared-protocol <- game-client/core
network -> application -> engine/domain
persistence -> application/domain ports
engine/domain -> không phụ thuộc Spring, Javalin, WebSocket, JPA
```

Chỉ chia sẻ wire DTO, enum và constant. Không dùng server entity trực tiếp trong LibGDX.

## Phase 0 - Baseline và contract

- [x] Chạy `mvn -f server\pom.xml test`. -> 112 tests pass (13/09/2026)
- [x] Chạy `npm test` và `npm run build`. -> 62 tests + build pass (13/09/2026)
- [x] Ghi flow connect -> join -> move/shoot -> state -> leave. -> `docs/protocol-v1.md` mục 1 & 3
- [x] Ghi message hiện tại và field bắt buộc. -> `docs/protocol-v1.md` mục 2 & 3
- [x] Tạo `docs/protocol-v1.md`.
- [x] Tạo parity checklist cho các chức năng phải giữ. -> `docs/parity-checklist.md`

**Không migration trong phase này.** (Đã xác nhận: không sửa behavior code.)

## Phase 1 - Tạo target structure

- [x] Tạo Spring Boot skeleton. -> `server-spring/` (port 8081 placeholder, 2 tests pass)
- [x] Tạo LibGDX `core`, `html`, `desktop` skeleton. -> `game-client/` (Gradle multi-project, source placeholder)
- [x] Tạo `shared-protocol` tối giản. -> `shared-protocol/` (15 class + 7 tests pass)
- [x] Tạo PostgreSQL Docker Compose skeleton. -> `infra/docker-compose.yml` (postgres:16-alpine)
- [x] Mapping từng class cũ sang vị trí đích. -> `docs/migration-mapping.md`
- [x] Giữ nguyên code cũ để rollback. -> server/ Javalin 112 tests + client/ React 62 tests vẫn PASS.

**Nghiệm thu**: structure mới build được; code cũ chưa bị xóa. -> Xong 13/09/2026.

## Phase 2 - Migrate backend

### Mapping chính

```text
GameLoop          -> engine               ✅ DONE
GameState         -> engine/domain        ✅ DONE
Tank/Bullet       -> domain/entity        ✅ DONE (copy nguyên)
CollisionManager  -> engine/rules         ✅ DONE (copy nguyên)
AI classes        -> ai                   ✅ DONE (copy nguyên)
MapLoader         -> map                  ✅ DONE (copy nguyên)
MessageHandler    -> network/websocket    ✅ DONE (GameWebSocketHandler)
MessageSender     -> network adapter      ✅ DONE (WebSocketStatePublisher)
WebSocketServer   -> Spring WebSocket     ✅ DONE (WebSocketConfig)
LeaderboardService-> application/leaderboard  ⏳ Phase 4
```

### Công việc

1. Tách input port và state publisher port. ✅ `StatePublisherPort`
2. Cho `GameLoop` không phụ thuộc Javalin hoặc `WsContext`. ✅ refactor constructor + broadcastState
3. Đưa protocol DTO vào `shared-protocol` hoặc package protocol độc lập. ⏳ defer Phase 4/5
4. Tạo Spring WebSocket endpoint `/game`. ✅
5. Tạo health endpoint `/api/health`. ✅ port 8081
6. Tạo `GameSessionService`. ✅
7. Giữ game loop single-threaded và 30Hz. ✅ đo 31-32/s
8. Chạy React client cũ với Spring Boot server để kiểm tra parity. ✅ 13/09/2026: CONNECT+JOIN_GAME+ACK+STATE(tanks=2,map) PASS
9. Chỉ gỡ Javalin khi parity test pass. ⏳ Phase 5

**Nghiệm thu**: React cũ vẫn join/move/shoot/leave được với Spring Boot server. ✅ Manual parity test 13/09/2026.

## Phase 3 - Migrate gameplay client

### Mapping chính

```text
Renderer.ts      -> GameRenderer
GameLoop.ts      -> LibGDX render(delta)
SpriteManager.ts -> AssetManager
InputHandler.ts  -> InputProcessor
GameCanvas.tsx   -> GameScreen
WebSocketClient  -> browser WebSocket bridge
```

### Công việc

1. Render map hiện tại bằng LibGDX desktop.
2. Chuyển asset/tank/bullet/effect hiện tại.
3. Chuyển keyboard input.
4. Kết nối WebSocket Spring Boot.
5. Parse snapshot và render state.
6. Build HTML5/GWT.
7. Nhúng output vào Thymeleaf page.
8. Giữ React Canvas cho đến khi LibGDX parity pass.

**Nghiệm thu**: Browser chơi được các flow hiện tại bằng LibGDX HTML5.

## Phase 4 - Migrate website và dữ liệu hiện có

- [ ] Tạo Thymeleaf layout.
- [ ] Migrate main menu/game selection hiện có.
- [ ] Tạo trang play chứa LibGDX output.
- [ ] Tạo PostgreSQL datasource.
- [ ] Migrate leaderboard hiện có sang PostgreSQL.
- [ ] Tạo repository/service tách khỏi game loop.
- [ ] Test restart server không mất leaderboard.

Không làm shop/research/save nâng cao trong phase này.

## Phase 5 - Cutover và dọn legacy

- [ ] End-to-end: Thymeleaf -> LibGDX -> WebSocket -> Spring Boot -> GameLoop.
- [ ] Test nhiều browser/session.
- [ ] Test disconnect/reconnect/leave.
- [ ] So sánh client cũ và client mới.
- [ ] Xóa React/Javalin legacy chỉ sau parity checklist.
- [ ] Cập nhật README, diagram và runbook.

## Definition of Done

- [ ] Chức năng hiện tại không bị mất.
- [ ] Website chạy bằng Spring Boot + Thymeleaf.
- [ ] Gameplay chạy bằng LibGDX HTML5.
- [ ] WebSocket vẫn là transport realtime.
- [ ] Leaderboard hiện có lưu PostgreSQL.
- [ ] Engine không phụ thuộc UI/network/database.
- [ ] Full test pass.
- [ ] Feature mới được giữ ở backlog riêng.
