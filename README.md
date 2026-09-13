# Tankzors Pro

Game xe tăng PvE thời gian thực (real-time) cho môn **Lập trình mạng**. Người chơi điều khiển tank, hạ gục AI trên các bản đồ có chướng ngại vật. Server-authoritative qua WebSocket, leaderboard và progression lưu PostgreSQL.

Tài liệu chi tiết:
- 📐 [Architecture](docs/architecture.md) — sơ đồ kiến trúc + mô tả package.
- 📡 [Networking](docs/networking.md) — WebSocket protocol + REST API chi tiết.
- 🚀 [Runbook](docs/runbook.md) — cách build, run, troubleshoot.
- 📋 [Migration Plan](docs/migration-mapping.md) — đã migrate qua các phase.
- ✅ [Phase 5 Verification](docs/phase5-verification.md) — parity report.
- 🔒 [Protocol v1](docs/protocol-v1.md) — wire format đóng băng.
- ✓ [Parity checklist](docs/parity-checklist.md) — behavior giữ nguyên.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | **Java 21** + **Spring Boot 3.3.4** (server-spring) |
| Realtime | **Spring WebSocket** tại `/game`, 30Hz tick, server-authoritative |
| Database | **PostgreSQL 16** (Docker compose) + Flyway migration |
| Persistence | Spring Data JPA + Hibernate |
| Web UI | **Thymeleaf** templates + React build nhúng iframe |
| Desktop client | **LibGDX 1.12.1 + LWJGL3** (game-client) |
| Legacy (kept) | `server/` Javalin port 8080 + `client/` React Canvas |

## Cấu trúc repo

```
LapTrinhMang/
├── server-spring/      # Spring Boot server (port 8081, chính)
│   ├── engine/         # GameLoop 30Hz + GameState + Collision
│   ├── entity/         # Tank, Bullet, Wall, ...
│   ├── ai/             # AIController + Chase/Patrol/Shoot
│   ├── map/            # MapLoader + GameMap + 3 campaign JSON
│   ├── network/        # WebSocket /game handler + adapter
│   ├── application/    # GameSessionService, LeaderboardService, PlayerProgressService
│   ├── leaderboard/    # JPA entity + repository
│   ├── progress/       # JPA entity + repository (Phase 5+)
│   ├── web/            # REST controllers + Thymeleaf PageController
│   └── config/         # EngineConfig, EngineLifecycle, GsonConfig
├── server/              # LEGACY: Javalin WebSocket server (port 8080, kept để parity)
├── client/              # LEGACY: React Canvas client (kept cho parity reference)
├── game-client/         # LibGDX desktop client (parity PASS với React Canvas)
├── shared-protocol/     # DTO/enum/constant wire protocol (chưa plug vào full)
├── infra/               # docker-compose.yml cho PostgreSQL
├── docs/                # tài liệu kỹ thuật
└── .planning/           # kế hoạch migration + task breakdown
```

## Quick start (3 bước)

### Bước 1 — Khởi động PostgreSQL

```bash
docker compose -f infra/docker-compose.yml up -d
# Container tankzors-postgres healthy sau 5s
```

### Bước 2 — Build & chạy Spring Boot

```bash
# Build fat jar
mvn -f server-spring/pom.xml clean package -DskipTests

# Chạy
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar
# Boot xong sau ~4s, Flyway migrate V1 + V2, GameLoop @ 30Hz
```

### Bước 3 — Mở browser

```
http://localhost:8081/             # Trang chủ Thymeleaf
http://localhost:8081/campaign     # Chọn màn (hiển thị unlock-theo-session)
http://localhost:8081/play?levelId=campaign_1   # Chơi (React iframe)
http://localhost:8081/leaderboard  # Bảng xếp hạng từ PostgreSQL
```

## Desktop client (optional)

LibGDX desktop launcher cho dev parity test:

```bash
mvn -f game-client/pom.xml clean package -DskipTests
java -jar game-client/target/tankzors-game-client-1.0-SNAPSHOT.jar
# Cửa sổ 800x600, kết nối ws://localhost:8081/game
```

## Testing

```bash
# Server tests (141 case)
mvn -f server-spring/pom.xml test

# Client tests (62 case, legacy React)
cd client && npm test

# Legacy server tests (112 case, kept để parity)
mvn -f server/pom.xml test
```

## Networking tóm tắt

**WebSocket `/game`** (text frame JSON):
- C→S: `JOIN_GAME`, `MOVE_INPUT`, `SHOOT_INPUT`, `PING`
- S→C: `CONNECT`, `STATE`, `ACK`, `PONG`, `ERROR`

**REST**:
- `GET /api/health` — health check
- `GET /api/leaderboard/{mapId}` — top 10
- `POST /api/leaderboard` — submit score
- `GET /api/progress` — unlocked levels cho session
- `POST /api/progress/reset` — reset progression

Chi tiết + sample packet: [`docs/networking.md`](docs/networking.md).

## Đồ án môn

- **Chủ đề**: Lập trình mạng.
- **Yêu cầu**: client-server architecture, SOLID, realtime protocol.
- **Author**: lone wolf project.
- **Stack mới**: Spring Boot + WebSocket + PostgreSQL + Thymeleaf + LibGDX.
- **Stack cũ**: Javalin + React Canvas (vẫn kept để parity reference).

## Roadmap còn lại

| Item | Trạng thái |
|---|---|
| LibGDX HTML5/GWT build (Phase 3.5) | defer (khó với Maven, đã có desktop parity) |
| Cutover final (xóa server/+client/) | defer đến Phase 3.5 |
| Multiplayer PvP | backlog (ngoài scope) |
| Procedural map generator | backlog |
| Sound + music | backlog |

Xem `.planning/ROADMAP.md` cho chi tiết đầy đủ.
