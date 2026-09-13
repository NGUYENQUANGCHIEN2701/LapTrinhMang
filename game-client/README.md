# game-client Module - README

LibGDX desktop client cho migration Phase 3. Phase 4 sẽ nhúng bản build HTML5 vào trang Thymeleaf.

## Mục đích

- ✅ Phase 1: skeleton Gradle multi-project.
- ✅ Phase 3 desktop: migrate `Renderer.ts` + `GameLoop.ts` + `InputHandler.ts` + `WebSocketClient.ts` sang LibGDX.
  Boot → kết nối `ws://localhost:8081/game` → JOIN_GAME → render STATE. Verified parity 13/09/2026.
- ⏳ Phase 3 HTML5: build GWT, nhúng vào trang play Thymeleaf (Phase 4).
- ⏳ Phase 5: cutover, xóa React client cũ.

## Cấu trúc package

```
game-client/
├── pom.xml                                # Maven, shade plugin cho fat jar
└── src/main/
    ├── java/com/tankzors/client/
    │   ├── GameClient.java               # ApplicationListener entry
    │   ├── desktop/DesktopLauncher.java  # LWJGL3 main
    │   ├── assets/AssetRepository.java   # load sprites (parity SpriteManager.ts)
    │   ├── render/GameRenderer.java      # SpriteBatch draw (parity Renderer.ts)
    │   ├── input/ClientInputProcessor.java # WASD+space (parity InputHandler.ts)
    │   ├── model/                        # snapshot tank/bullet/map/state
    │   └── network/
    │       ├── WebSocketBridge.java      # Java-WebSocket (parity WebSocketClient.ts)
    │       ├── StatePacketParser.java    # parse STATE bằng LibGDX JsonReader
    │       └── PacketBuilder.java        # build JOIN/MOVE/SHOOT JSON
    └── resources/assets/                 # copied from client/public/assets/
        ├── tanks/                         # player_tank.png, enemy_tank.png
        ├── effects/                       # bullet.png, explosion_*.png
        └── map/                           # ground.png, wall.png, spikes.png, palms.png
```

## Build & Run

```bash
# Build fat jar:
mvn -f game-client/pom.xml clean package

# Run (cần server-spring chạy trên 8081 trước):
java -jar game-client/target/tankzors-game-client-1.0-SNAPSHOT.jar

# Override wsUrl hoặc levelId:
java -jar game-client/target/tankzors-game-client-1.0-SNAPSHOT.jar --ws ws://localhost:8081/game --level campaign_2
```

## Test parity đã verify (13/09/2026)

Client boot log:

```
[main] INFO  GameClient - Boot - wsUrl=ws://localhost:8081/game level=campaign_1
[WebSocketConnectReadThread-30] INFO WebSocketBridge - Đã kết nối tới ws://localhost:8081/game
[main] INFO  GameClient - sessionId=8d7f3fa0-6f6e-9b94-6beb-96a83fc955b1
[main] INFO  GameClient - first STATE received: status=PLAYING tanks=2 bullets=0
```

Sau đó, trong cửa sổ desktop:
- Map campaign_1 (20×15, tileSize 40) render đúng sprite ground/wall.
- 2 tanks (player xanh + AI đỏ) xuất hiện ở vị trí spawn.
- HUD panel HP top-left.
- Bấm WASD/Arrow để di chuyển, Space/J để bắn.

Tương tự React client cũ.

## Parity mapping

| React Canvas | LibGDX |
|---|---|
| `WebSocketClient.ts` | `client.network.WebSocketBridge` (dùng Java-WebSocket) |
| `MessageParser.ts` | `network.StatePacketParser` (dùng `JsonReader` LibGDX) |
| `MessageBuilder.ts` | `network.PacketBuilder` |
| `Renderer.ts` | `render.GameRenderer` (SpriteBatch + ShapeRenderer) |
| `SpriteManager.ts` | `assets.AssetRepository` |
| `InputHandler.ts` | `input.ClientInputProcessor` |
| `GameCanvas.tsx` | `GameClient` (ApplicationListener) |

## Phase 3 kế hoạch còn lại

1. **HTML5 build** (Phase 3.5): GWT plugin, JsInterop bridge cho browser WebSocket,
   host page trong `server-spring/src/main/resources/templates/play.html`.
2. **Smoke test浏览器**: chơi 30s, so sánh với React client.

## Lưu ý

- **Gradle đã bị loại bỏ** ở Phase 3 — Maven shade plugin build fat jar chạy trực tiếp.
- **Gson tránh**: parse JSON dùng LibGDX `JsonReader` (GWT-compatible). Phase 4 khi sang
  HTML5 không phải đổi parser.
- **Assets copy** từ `client/public/assets/` khi Phase 3 setup. Phase 4 hay 5 có thể chuyển
  assets sang folder riêng để chuẩn bị xóa legacy client.
- **Java 17**: vì LibGDX 1.12.1 không build bằng Java 21 mới полностью.
