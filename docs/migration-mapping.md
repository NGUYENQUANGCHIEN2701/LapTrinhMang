# Migration mapping - Class cũ → vị trí đích

Bảng mapping dựa trên REFACTOR-PLAN.md và AI-IMPLEMENTATION-GUIDE.md,
cập nhật sau khi rà server và client code hiện tại. Phase 1 chỉ lập kế
hoạch, chưa copy. Phase 2/3 sẽ thực thi.

## 1. Server legacy → Spring Boot + domain

| Class / file hiện tại (`server/...`) | Vị trí đích (`server-spring/...`) | Phương án Phase 2 |
|---|---|---|
| `Main.java` | `TankzorsApplication.java` | Replace: Javalin app -> `SpringApplication.run`, graceful shutdown hook dùng Spring Boot built-in |
| `ServerConfig.java` | `config/ServerConfig.java` 或 `application.yml` | Port/TickRate giữ constant, có thể inject qua `@Value` |
| `engine/GameLoop.java` | `engine/GameLoop.java` | **Giữ nguyên 100%**, bỏ dependency `MessageSender` cụ thể, inject interface `StatePublisher` port |
| `engine/GameState.java` | `engine/GameState.java` (hoặc `domain/`) | **Giữ nguyên 100%**, không sửa logic |
| `engine/CollisionManager.java` | `engine/CollisionManager.java` (static helpers) | **Giữ nguyên 100%** |
| `engine/BulletManager.java` | `engine/BulletManager.java` | **Giữ nguyên 100%** (nếu có) |
| `entity/Tank.java`, `PlayerTank`, `AITank`, `Bullet`, `Wall`, `Obstacle`, `Item`, `Direction`, `Position`, `Rectangle` | `domain/entity/...` | **Giữ nguyên 100%**,.Spring annotation KHÔNG được thêm vào class entity |
| `map/GameMap.java`, `MapLoader.java`, `TileType.java` | `map/...` | **Giữ nguyên 100%**, Gson serialize không đổi |
| `resources/maps/*.json` | Copy sang `server-spring/src/main/resources/maps/` | alias classpath resource |
| `ai/AIController.java`, `ChaseBehavior`, `PatrolBehavior`, `ShootBehavior`, `IdleBehavior`, `AINavigator`, `GridPathfinder`, `LineOfSight`, `DifficultyLevel` | `ai/...` | **Giữ nguyên 100%** để parity AI tests pass |
| `interfaces/*` | `domain/valueobject/` hoặc giữ ở `interfaces/` -> sẽ refactor dần | Đầu Phase 2 giữ nguyên package để ít diff nhất |
| `network/WebSocketServer.java` | XÓA (thay bằng `network/WebSocketConfig.java` + `GameWebSocketHandler.java`) | Đã có skeleton Phase 1 |
| `network/ConnectionManager.java` | `network/SessionRegistry.java` hoặc giữ tên | Spring `WebSocketSession` khác `WsContext`, cần adapter |
| `network/MessageHandler.java` | `network/GameWebSocketHandler.handleTextMessage` | Routing logic giữ, dùng shared-protocol packet |
| `network/MessageSender.java` | `network/StatePublisherPort` (interface) + impl `WebSocketStatePublisher` | Tách port để `GameLoop` không biết Spring WebSocket |
| `network/protocol/InputMessage.java`, `StateMessage.java`, `ResultMessage.java`, `ErrorMessage.java`, `GameMessage.java` | `shared-protocol/src/main/java/com/tankzors/protocol/*` | Đã tạo Phase 1 - dùng `BasePacket`/`JoinGamePacket`/`MoveInputPacket`/`ShootInputPacket`/`StatePacket`/`AckPacket`/`PongPacket`/`ErrorPacket` |
| `leaderboard/LeaderboardService.java` | `application/leaderboard/LeaderboardService.java` + `persistence/LeaderboardRepository` interface | Phase 4 - hiện rỗng, chưa mapping |
| `mode/*` (GameMode, CampaignMode, OnlineMode, ProgressManager) | `application/campaign/...` | Phase 4 - ProgressManager chuyển sang server-side PostgreSQL |
| `database/*` (DatabaseConfig, DatabaseManager) | XÓA - Spring Data JPA thay thế | Phase 4 |

## 2. Client legacy → LibGDX

| File hiện tại (`client/src/...`) | Vị trí đích (`game-client/...`) | Phương án Phase 3 |
|---|---|---|
| `core/Renderer.ts` | `core/src/main/java/com/tankzors/client/render/GameRenderer.java` | Dùng `SpriteBatch` thay Canvas2D, giữ sprite sheet & frame mapping. Giữ cache offscreen cho map. |
| `core/GameLoop.ts` | `core/src/main/java/com/tankzors/client/GameClient.java` (ApplicationListener.render) | LibGDX tự render loop, không cần requestAnimationFrame |
| `core/SpriteManager.ts` | `core/src/main/java/com/tankzors/client/AssetLoader.java` | LibGDX `AssetManager` |
| `core/input/InputHandler.ts` | `core/src/main/java/com/tankzors/client/input/ClientInputProcessor.java` | LibGDX `InputProcessor`, giữ map key->Direction và debounce logic |
| `core/network/WebSocketClient.ts` | `html/.../WebSocketBridge.java` | Browser native WebSocket qua JSNI/JsInterop (GWT) |
| `core/network/utils/MessageParser.ts` | `core/.../MessageParser.java` hoặc JsInterop | Hand-coded JSON parse nếu GWT không allow Gson |
| `core/network/utils/MessageBuilder.ts` | `core/.../MessageBuilder.java` | Dùng shared-protocol packet |
| `core/network/types.ts` | `shared-protocol` | Đã tạo DTO tương đương |
| `features/game/GameCanvas.tsx` | `html/.../HtmlLauncher.java` mount vào div | Thay thế React mount bằng GWT host page |
| `features/campaign/` `features/menu/` `features/leaderboard/` | Spring Boot Thymeleaf templates (`server-spring/.../templates/`) | Phase 4 - chỉ giữ game play trong LibGDX, menu chuyển sang server-side render |
| `shared/types/game.ts` | Đổi sang shared-protocol `TileTypeDto`, `DirectionDto`, `GameStatusDto` | DTO Java thay TypeScript interfaces |
| `shared/constants/game.ts` | `shared-protocol/.../ProtocolConstants.java` (server) + hardcoded trong `core` (client) | Đã tạo Phase 1 cho server constants |
| `assets/tanks/bullets/tiles/ui/*.png` | Copy sang `game-client/core/assets/` | Phase 3 |

## 3. Nguyên tắc giữ nguyên khi migrate

1. **Engine + domain = không có Spring/Javalin/JPA trong package**.
   Spring chỉ inject bean từ `application/`, KHÔNG inject vào `engine/`
   hoặc `domain/`.
2. **AI constants (DifficultyLevel, ShootBehavior.SHOOT_RANGE)** giữ giá
   trị nguyên. Bảng so sánh tại protocol-v1.md mục 4.2.
3. **Tick rate 30Hz và single-threaded GameLoop** giữ. Spring Boot chỉ
   làm transport, không đa luồng hoá tick.
4. **JSON format packet** giữ nguyên v1. Thay đổi nào phải có
   `protocol-v2.md` song song.
5. **Map JSON schema** giữ nguyên (`name`/`difficulty`/`width`/`height`/
   `tileSize`/`enemies`/`rewards`/`grid`). MapLoader copy sang server-spring
   không sửa class.
6. **Gson của MessageSender** không đổi format. Server-spring dùng cùng
   `new GsonBuilder().create()` (đã setup ở `GsonConfig`).

## 4. Thứ tự công việc chi tiết theo phase

### Phase 2 (backend migration)

Thứ tự stub:

1. Copy nguyên package `engine/`, `entity/`, `map/`, `ai/`,
   `interfaces/` từ `server/` sang `server-spring/src/main/java/com/tankzors/`.
2. Copy `resources/maps/*.json` sang
   `server-spring/src/main/resources/maps/`.
3. Thêm interface `network/StatePublisherPort` có method `broadcast(Object)`.
4. Tạo impl `network/WebSocketStatePublisher implements StatePublisherPort`
   inject `GameWebSocketHandler`.
5. Refactor `GameLoop` constructor: chấp nhận `StatePublisherPort` thay
   vì `MessageSender`. (đây là refactor nhỏ, không đổi behavior).
6. Implement `GameSessionService` queue input từ `GameWebSocketHandler`
   sang `GameLoop.queueInput`.
7. Boot cả 2 server (Javalin 8080 + Spring 8081), test parity với cùng
   client React cũ: trỏ `DEFAULT_SERVER_URL` tới 8081 và verify parity
   checklist mục 1-7.

### Phase 3 (client migration)

Theo `ROADMAP.md` mục Phase 3 và `game-client/README.md` mục "Phase 3 kế
hoạch".

### Phase 4 (persistence + website)

Theo REFACTOR-PLAN.md mục Phase 4.

## 5. File đã tạo Phase 1 (skeleton, chưa có logic migrate)

```
shared-protocol/                                  (new module)
├── README.md
├── pom.xml
└── src/
    ├── main/java/com/tankzors/protocol/
    │   ├── PacketType.java
    │   ├── DirectionDto.java
    │   ├── GameStatusDto.java
    │   ├── TileTypeDto.java
    │   ├── InputAction.java
    │   ├── ProtocolConstants.java
    │   ├── BasePacket.java
    │   ├── ConnectPacket.java
    │   ├── JoinGamePacket.java
    │   ├── MoveInputPacket.java
    │   ├── ShootInputPacket.java
    │   ├── StatePacket.java
    │   ├── AckPacket.java
    │   ├── PongPacket.java
    │   └── ErrorPacket.java
    └── test/java/com/tankzors/protocol/
        ├── PacketTypeTest.java
        └── PacketSerializationTest.java

server-spring/                                    (new module)
├── README.md
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/tankzors/
    │   │   ├── TankzorsApplication.java
    │   │   ├── config/GsonConfig.java
    │   │   ├── network/
    │   │   │   ├── GameHandlerConfig.java
    │   │   │   ├── GameWebSocketHandler.java
    │   │   │   └── WebSocketConfig.java
    │   │   └── web/HealthController.java
    │   └── resources/application.yml
    └── test/java/com/tankzors/
        └── TankzorsApplicationContextTest.java

game-client/                                      (new Gradle module)
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/gradle-wrapper.properties
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/com/tankzors/client/GameClient.java
├── desktop/
│   ├── build.gradle.kts
│   └── src/main/java/com/tankzors/client/desktop/DesktopLauncher.java
└── html/
    ├── build.gradle.kts
    └── src/main/java/com/tankzors/client/html/HtmlLauncher.java

infra/                                            (new folder)
├── README.md
└── docker-compose.yml                            (PostgreSQL skeleton)

docs/                                             (new folder)
├── migration-mapping.md                          (this file)
├── protocol-v1.md                                (Phase 0)
└── parity-checklist.md                           (Phase 0)
```

Total **30 file mới**, **0 file code cũ bị sửa/xóa**.
