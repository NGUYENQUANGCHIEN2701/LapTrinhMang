# game-client Module - README

LibGDX client đa nền tảng cho desktop và HTML5/GWT, được nhúng vào trang Thymeleaf `/play`.

## Mục đích

- ✅ Phase 1: skeleton Gradle multi-project.
- ✅ Phase 3 desktop: migrate `Renderer.ts` + `GameLoop.ts` + `InputHandler.ts` + `WebSocketClient.ts` sang LibGDX.
  Boot → kết nối `ws://localhost:8081/game` → JOIN_GAME → render STATE. Verified parity 13/09/2026.
- ✅ Phase 3 HTML5: build GWT và nhúng vào trang play Thymeleaf.
- ✅ Phase G: WebSocket protocol parity — client gửi JOIN_GAME/MOVE_INPUT/SHOOT_INPUT/STOP/PING,
  nhận CONNECT/ACK/STATE/PONG/ERROR. Verified wire-level 16/09/2026.
- ✅ Phase H: build test (core/desktop/html:dist/copyToServer/jar) + wire smoke test. 16/09/2026.
- ⏳ Browser smoke test thủ công (mở `/play?levelId=campaign_1`, chơi 30s) — xem mục cuối.
- ⏳ Phase 5: cutover, xóa React client cũ.

## Cấu trúc package

```
game-client/
├── core/       # gameplay, render, input và protocol dùng chung
├── desktop/    # LWJGL3 launcher và Java-WebSocket adapter
├── html/       # GWT launcher, browser WebSocket và webapp
└── assets/     # assets dùng chung cho desktop/HTML5
```

## Build & Run

```cmd
cd game-client

rem Build desktop fat jar (desktop\build\libs\tankzors-desktop-1.0-SNAPSHOT.jar):
gradlew.bat desktop:fatJar

rem Chay desktop client doc lap (can server-spring chay tren 8081):
java -jar desktop\build\libs\tankzors-desktop-1.0-SNAPSHOT.jar --ws ws://localhost:8081/game --level campaign_1

rem Hoac chay qua Gradle:
gradlew.bat desktop:run --args="--ws ws://localhost:8081/game --level campaign_1"

rem Build HTML5 distribution tai html\build\dist:
gradlew.bat html:dist

rem Build va dong bo HTML5 vao Spring Boot static resources:
gradlew.bat html:copyToServer
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

## Phase G/H — verify 16/09/2026

### Wire-level protocol parity (PowerShell ClientWebSocket → ws://localhost:8081/game)

| Bước | Gửi | Nhận | Kết quả |
|---|---|---|---|
| 1 | (mở kết nối) | `CONNECT` + `sessionId` | PASS |
| 2 | `JOIN_GAME campaign_1` (nested payload) | `ACK status=RECEIVED action=JOIN_GAME` + `STATE` ngay | PASS |
| 3 | `MOVE_INPUT {direction:UP, moving:true}` | `ACK action=MOVE_INPUT` | PASS |
| 4 | `STOP` | `ACK action=STOP` | PASS |
| 5 | `PING` | `PONG` | PASS |
| 6 | JSON sai | `ERROR code=400` | PASS |
| 7 | — | `STATE` ticks 30Hz, tank id == sessionId | PASS |

STATE shape khớp `docs/protocol-v1.md` 3.5: map 20×15 tileSize 40, `gameStatus=PLAYING`,
player spawn (100,100), AI `ai-16-11` (660,460). Server code KHÔNG đổi.

### Thay đổi Phase G (chỉ client)

- `PacketBuilder.stop()` / `PacketBuilder.ping()`.
- `ClientInputProcessor.Callbacks.onAllReleased()` — thả phím cuối cùng / pause / dispose → gửi `STOP`.
- `GameClient`: heartbeat `PING` mỗi 10s khi connected; xử lý `CONNECT` (log sessionId),
  `ACK`/`PONG` (debug log, PONG đo RTT), `ERROR` như trước.

### Build test (Phase H)

- `:core:compileJava` PASS.
- `:desktop:fatJar` PASS → `desktop/build/libs/tankzors-desktop-1.0-SNAPSHOT.jar` (14.1 MB).
- `:html:dist` PASS → hash mới `393A407C91EC3C967BCE351655849E58.cache.js` (chứa PING/STOP).
  Task `dist` đổi từ `Copy` sang `Sync` để không tích tụ cache.js hash cũ.
- `:html:copyToServer` PASS → `server-spring/src/main/resources/static/game/libgdx` chỉ còn hash mới.
- `mvn clean package -DskipTests` PASS → jar chứa `BOOT-INF/classes/static/game/libgdx/**` mới.

### Desktop regression (16/09/2026)

```text
[GameClient] Boot - wsUrl=ws://localhost:8081/game level=campaign_1
[GameClient] CONNECT received sessionId=ee47a143-6eb1-642f-1df0-3dcea791eb77
[GameClient] first STATE received: status=PLAYING tanks=2 bullets=0
```

### HTTP smoke test

- `GET /play?levelId=campaign_1` → 200, iframe `src=/game/libgdx/index.html?levelId=campaign_1`,
  `data-level-id=campaign_1`, không còn placeholder (chỉ `<noscript>` fallback).
- `/game/libgdx/index.html`, `tankzorshtml.nocache.js`, cache.js mới, `assets/assets.txt`,
  asset PNG → đều 200.

### Kiểm thử còn lại (thủ công, cần browser thật)

1. Mở `http://localhost:8081/play?levelId=campaign_1`, chơi 30s: canvas hiển thị,
   WASD/mũi tên di chuyển, Space/J bắn, HP đổi khi trúng đạn, victory/defeat.
2. Console không được có: `WebSocket is not defined`, `ClassNotFoundException`,
   `NoSuchMethodError`, `Unable to load asset`, `GWT module not found`, `404 html.nocache.js`,
   `404 asset path`.
3. Sau đó mới qua Phase I (cutover).

## Lưu ý

- **Gradle multi-project** quản lý cả `core`, `desktop` và `html`; Java 17 được dùng cho GWT/LibGDX.
- **Gson tránh**: parse JSON dùng LibGDX `JsonReader` (GWT-compatible). Phase 4 khi sang
  HTML5 không phải đổi parser.
- **Assets copy** từ `client/public/assets/` khi Phase 3 setup. Phase 4 hay 5 có thể chuyển
  assets sang folder riêng để chuẩn bị xóa legacy client.
- **Java 17**: vì LibGDX 1.12.1 không build bằng Java 21 mới полностью.
