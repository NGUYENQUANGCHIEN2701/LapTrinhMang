# Roadmap: Migrate code hiện tại sang Java Web stack mới

## Mục tiêu

Migrate toàn bộ chức năng đang có sang stack mới, không mở rộng gameplay trong đợt này.

```text
Website shell: Spring Boot + Thymeleaf
Gameplay client: LibGDX HTML5/GWT
Realtime: WebSocket
Server: Java 21 + Spring Boot
Database: PostgreSQL
```

Các tính năng mới trong hai PDF được giữ ở backlog. Đợt này chỉ giữ parity với code hiện tại: connect, join game, campaign hiện tại, movement, shooting, bullet, collision, HP, AI, map, victory/defeat, leaderboard hiện có.

## Các phase

- [x] **Phase 0: Baseline và contract** - Đóng băng behavior hiện tại, chốt protocol (xong 13/09/2026 - `docs/protocol-v1.md`, `docs/parity-checklist.md`)
- [x] **Phase 1: Tạo structure mới** - Tạo target modules/folders, chưa xóa code cũ (xong 13/09/2026 - 4 module mới + migration-mapping)
- [x] **Phase 2: Migrate backend sang Spring Boot** - Giữ nguyên game engine và behavior (xong 13/09/2026 - 115 tests pass + manual parity PASS)
- [~] **Phase 3: Migrate client sang LibGDX HTML5** - Desktop launcher parity OK (13/09/2026); HTML5/GWT build pending Phase 3.5
- [~] **Phase 4: Migrate persistence và website** - PostgreSQL leaderboard done (13/09/2026); Thymeleaf website defer Phase 5/4.5
- [ ] **Phase 5: Cutover và verification** - Chạy end-to-end, gỡ legacy sau khi parity pass
- [ ] **Phase 3: Migrate client sang LibGDX HTML5** - Thay React Canvas bằng Java gameplay client
- [ ] **Phase 4: Migrate persistence và website** - PostgreSQL, Thymeleaf và leaderboard hiện có
- [ ] **Phase 5: Cutover và verification** - Chạy end-to-end, gỡ legacy sau khi parity pass

## Phase 0: Baseline và contract

### Mục tiêu

Ghi nhận hành vi hiện tại trước migration.

### Kiểm tra

```cmd
mvn -f server\pom.xml test
cd client
npm test
npm run build
```

### Việc cần làm

- Ghi lại flow connect -> join -> move/shoot -> state -> leave.
- Ghi lại message `CONNECT`, `INPUT`, `JOIN_GAME`, `STATE`, `ACK`, `PONG`, `ERROR`.
- Ghi lại map format, tank format, bullet format và status format.
- Ghi lại các test hiện tại làm safety net.
- Chốt endpoint WebSocket vẫn là `/game`.

### Nghiệm thu

- Có protocol document version 1. -> `docs/protocol-v1.md` (13/09/2026)
- Server/client hiện tại build và test pass. -> 112 server tests + 62 client tests + build pass
- Có danh sách behavior phải giữ nguyên. -> `docs/parity-checklist.md`

## Phase 1: Target structure

### Mục tiêu

Tạo cấu trúc đích song song với code cũ để migration từng phần.

### Cấu trúc repository đích

```text
LapTrinhMang/
├── server/                         # Spring Boot + game server
│   └── src/
│       ├── main/java/com/tankzors/
│       │   ├── TankzorsApplication.java
│       │   ├── domain/
│       │   │   ├── entity/
│       │   │   ├── valueobject/
│       │   │   └── rules/
│       │   ├── application/
│       │   │   ├── game/
│       │   │   ├── campaign/
│       │   │   └── leaderboard/
│       │   ├── engine/
│       │   ├── ai/
│       │   ├── map/
│       │   ├── network/
│       │   │   ├── websocket/
│       │   │   └── protocol/
│       │   ├── persistence/
│       │   └── config/
│       └── main/resources/
│           ├── application.yml
│           └── maps/
├── game-client/                    # LibGDX
│   ├── core/src/main/java/
│   ├── html/src/main/java/
│   ├── desktop/src/main/java/
│   └── assets/
├── web/                             # Thymeleaf/static web resources
│   └── src/main/resources/
│       ├── templates/
│       └── static/
├── shared-protocol/                 # Chỉ DTO/constant wire protocol
├── infra/
│   └── docker-compose.yml
└── .planning/
```

### Quy tắc dependency

```text
shared-protocol <- server
shared-protocol <- game-client/core

web -> application services
network -> application services
application -> engine/domain
persistence -> application/domain ports
engine/domain -> không phụ thuộc Spring/WebSocket/JPA
```

Không dùng chung server entity với LibGDX client. Chỉ dùng chung protocol DTO/enum/constant tối giản.

## Phase 2: Migrate backend sang Spring Boot

### Mục tiêu

Thay Javalin bằng Spring Boot nhưng giữ `GameLoop`, AI, collision, map và protocol behavior.

### Mapping class hiện tại

| Code hiện tại | Vị trí đích | Cách xử lý |
|---|---|---|
| `GameLoop` | `engine` | Giữ logic, bỏ dependency network cụ thể |
| `GameState` | `engine`/`domain` | Giữ state simulation |
| `Tank`, `PlayerTank`, `AITank` | `domain/entity` | Giữ behavior, tách DTO |
| `Bullet`, `CollisionManager` | `engine`/`domain/rules` | Giữ logic |
| `AIController`, behaviors | `ai` | Giữ thuật toán hiện tại |
| `MapLoader`, `GameMap` | `map` | Giữ map JSON và loader |
| `InputMessage`, `StateMessage` | `shared-protocol`/`network/protocol` | Chuẩn hóa DTO |
| `MessageHandler` | `network/websocket` | Spring WebSocket adapter |
| `MessageSender` | `network/websocket` | Publisher adapter |
| `WebSocketServer` | Spring configuration/handler | Thay Javalin endpoint |
| `LeaderboardService` | `application/leaderboard` | Tách port và persistence adapter |

### Việc cần làm

1. Thêm Spring Boot entry point và configuration.
2. Tạo WebSocket handler tại `/game`.
3. Tạo `GameSessionService` quản lý session/player.
4. Tạo input port để handler enqueue input.
5. Tạo state publisher port để engine không biết WebSocket.
6. Inject `GameLoop` bằng Spring nhưng không đưa Spring annotation vào domain entity.
7. Giữ graceful shutdown.
8. Giữ HTTP health endpoint.
9. Chạy client React cũ với server Spring Boot để kiểm tra parity.

### Nghiệm thu

- Spring Boot start thành công.
- React client cũ vẫn connect `/game`.
- Join, move, shoot, collision, AI, victory/defeat hoạt động.
- Tất cả server tests pass.

## Phase 3: Migrate client sang LibGDX HTML5

### Mục tiêu

Thay `client/src/core/Renderer.ts`, `GameLoop.ts`, `GameCanvas.tsx` bằng LibGDX gameplay client chạy trong browser.

### Mapping client hiện tại

| Client hiện tại | LibGDX đích |
|---|---|
| `Renderer.ts` | `GameRenderer` + `SpriteBatch` |
| `GameLoop.ts` | LibGDX `render(delta)` |
| `SpriteManager.ts` | `AssetManager` |
| `InputHandler.ts` | `InputProcessor`/input polling |
| `GameCanvas.tsx` | `GameScreen` mount trong HTML target |
| `GameStateData` | client snapshot model |
| `WebSocketClient.ts` | browser WebSocket bridge |

### Thứ tự migrate

1. Tạo LibGDX `core`, `html`, `desktop` modules.
2. Chạy một map và một tank bằng desktop backend.
3. Chuyển asset loading.
4. Chuyển map renderer.
5. Chuyển tank/bullet renderer.
6. Chuyển keyboard input.
7. Kết nối WebSocket tới Spring Boot.
8. Parse snapshot và cập nhật entity.
9. Thêm animation/effect/HUD đang có.
10. Build HTML5 và nhúng vào website.

### Nghiệm thu

- LibGDX desktop test chạy.
- HTML5 target chạy trong browser.
- Game hiện tại có cùng map/entity/controls.
- Snapshot server được render đúng.
- React client cũ vẫn được giữ đến khi cutover hoàn tất.

## Phase 4: PostgreSQL và Thymeleaf

### Mục tiêu

Đưa các dữ liệu đang có vào database và chuyển website shell sang Thymeleaf; không thêm shop/research/save nâng cao.

### Database hiện tại cần migrate

- Leaderboard hiện có.
- Match result hiện có nếu đã được tạo.
- Player profile tối thiểu nếu flow hiện tại cần.

### Việc cần làm

- Tạo PostgreSQL trong `infra/docker-compose.yml`.
- Tạo datasource config bằng environment variables.
- Tạo migration/schema cho leaderboard và match result.
- Tách `LeaderboardRepository` interface khỏi implementation PostgreSQL.
- Tạo Thymeleaf pages cho main menu, game selection, campaign selection và leaderboard hiện có.
- Tạo trang play chứa LibGDX HTML5 output.
- Giữ UI tối giản tương đương behavior hiện tại.

### Nghiệm thu

- Restart server không mất leaderboard.
- Website Thymeleaf mở được bằng browser.
- Có thể từ menu vào game.
- Có thể xem leaderboard từ PostgreSQL.

## Phase 5: Cutover và verification

### Việc cần làm

- Chạy end-to-end: website -> play -> WebSocket -> game loop -> result.
- Test nhiều browser session.
- Test disconnect/reconnect/leave.
- So sánh behavior client mới với React client cũ.
- Xóa legacy code chỉ sau khi parity checklist pass.
- Cập nhật README, architecture diagram và runbook.
- Cập nhật `TRELLO-TASKS.md` hoặc đánh dấu task legacy.

### Nghiệm thu cuối phase

- Không còn flow production phụ thuộc React/Javalin cũ.
- Không có game rule trong controller/WebSocket handler.
- Game engine chạy được bằng unit test.
- Website chạy bằng Spring Boot + Thymeleaf.
- Gameplay chạy bằng LibGDX HTML5.
- Dữ liệu leaderboard đọc/ghi PostgreSQL.

## Backlog sau migration

Chỉ làm sau Phase 5:

- Save/load nhiều slot.
- Shop và research tree.
- Practice procedural map.
- Online Levels.
- Vũ khí/tank/AI nâng cao.
- Fog of war, terrain effect, boss, ally tank.
- Touch/mobile optimization.
