# server-spring Module - README

Spring Boot server cho migration Phase 2+. Phase 0/1 đã hoàn thành skeleton,
Phase 2 đã migrate backend xong (engine/AI/collision/map giữ nguyên behavior
với legacy Javalin).

## Mục đích

- ✅ Phase 1: skeleton Spring Boot + health endpoint + WebSocket placeholder.
- ✅ Phase 2: migrate engine/domain/AI/collision/map từ `server/` (Javalin)
  sang Spring WebSocket. GameLoop 30Hz đã hoạt động, chấp nhận CONNECT/
  JOIN_GAME/MOVE_INPUT/SHOOT_INPUT theo protocol-v1. Legacy server (Javalin)
  chưa bị xóa, dùng làm parity reference và rollback.
- ⏳ Phase 4: thêm JPA (PostgreSQL) + Thymeleaf.
- ⏳ Phase 5: cutover, xóa `server/` Javalin.

## Cấu trúc package

```
server-spring/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/tankzors/
    │   │   ├── TankzorsApplication.java      # @SpringBootApplication entry
    │   │   ├── ServerConfig.java             # TICK_RATE, PORT constants
    │   │   ├── config/
    │   │   │   ├── EngineConfig.java         # Bean GameState + GameLoop + parser
    │   │   │   ├── EngineLifecycle.java      # @PostConstruct/@PreDestroy
    │   │   │   └── GsonConfig.java
    │   │   ├── application/
    │   │   │   └── GameSessionService.java   # route input -> GameLoop.queueInput
    │   │   ├── network/
    │   │   │   ├── GameWebSocketHandler.java # route packet -> service
    │   │   │   ├── WebSocketConfig.java      # đăng ký /game
    │   │   │   ├── WebSocketStatePublisher.java # impl StatePublisherPort
    │   │   │   ├── SessionRegistry.java      # quản lý WebSocketSession
    │   │   │   └── InputPacketParser.java    # flat hoá nested payload
    │   │   ├── engine/                       # copy từ server/, giữ nguyên
    │   │   │   ├── GameLoop.java             # refactor: dùng StatePublisherPort
    │   │   │   ├── GameState.java
    │   │   │   ├── CollisionManager.java
    │   │   │   ├── BulletManager.java
    │   │   │   └── StatePublisherPort.java   # NEW: interface tách coupling
    │   │   ├── entity/                       # copy nguyên
    │   │   ├── map/                          # copy nguyên
    │   │   ├── ai/                           # copy nguyên
    │   │   ├── interfaces/                   # copy nguyên
    │   │   ├── network/protocol/             # copy nguyên (InputMessage etc.)
    │   │   └── web/
    │   │       └── HealthController.java
    │   └── resources/
    │       ├── application.yml
    │       └── maps/                          # campaign_1/2/3.json
    └── test/java/com/tankzors/               # 115 tests (112 legacy copy + 3 new context)
```

## Port

| Module | Port | Ghi chú |
|---|---|---|
| `server/` Javalin cũ | 8080 | Legacy, vẫn build/test được (parity reference) |
| `server-spring/` (this) | 8081 | Phase 2: đầy đủ game logic |

Phase 5 cutover: đổi port `server-spring` về 8080 sau khi xóa legacy.

## Build & Run

```bash
# Cần cài shared-protocol jar nếu có dependency (Phase 2 tạm bỏ, không cần)
mvn -f server-spring/pom.xml clean package

# Run (port 8081) qua Maven:
mvn -f server-spring/pom.xml spring-boot:run

# Hoặc jar (build trước đó):
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar
```

## Smoke test parity với protocol-v1

```bash
# Health
curl http://localhost:8081/
# Tankzors Server (Spring) is running

curl http://localhost:8081/api/health
# {"port":8081,"status":"UP"}

# WebSocket test bằng Node + ws:
# 1) Sửa DEFAULT_SERVER_URL trong client/src/shared/constants/game.ts
#    thành ws://localhost:8081/game
# 2) cd client && npm run dev
# 3) Mở browser, JOIN_GAME, tanks xuất hiện, bấm WASD/Space để kiểm tra
#    move/shoot/collision/AI (parity với protocol-v1 mục 4)
```

Log mẫu đã được verify manual parity (13/09/2026):

```
[CLIENT] << CONNECT { sessionId, type:'CONNECT', message, timestamp }
[CLIENT] << ACK { action:'JOIN_GAME', status:'RECEIVED', timestamp }
[CLIENT] << first STATE { gameStatus:'PLAYING', tanks:2, bullets:0, hasMap:true }
```

## Refactor chính của Phase 2 (vs Phase 1)

1. **Copy nguyên packages**: `engine/`, `entity/`, `map/`, `ai/`, `interfaces/`,
   `network/protocol/`, `resources/maps/` từ `server/`.
2. **Tách port**: `StatePublisherPort` interface trong `engine/` → `GameLoop`
   chỉ biết interface, không biết Spring WebSocket.
3. **Adapter**: `WebSocketStatePublisher implements StatePublisherPort`.
4. **Service**: `GameSessionService` route input từ WS handler vào
   `GameLoop.queueInput`.
5. **Lifecycle**: `EngineLifecycle @PostConstruct start / @PreDestroy stop`.

`GameLoop` chỉ thay 2 dòng (constructor + `broadcastState()`), tất cả 112
legacy tests vẫn pass nguyên → parity behavior với Javalin cũ.

## Phase 3 kế hoạch

Phase 3 sẽ migrate client React Canvas sang LibGDX HTML5 trong `game-client/`,
không động vào `server-spring` (trừ khi cần protocol-v2 song song).

