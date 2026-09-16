# LibGDX HTML5 Implementation Plan

## 1. Mục tiêu

Đưa gameplay LibGDX hiện tại từ desktop lên browser thông qua HTML5/GWT và nhúng vào trang Thymeleaf `/play`.

Sau phase này, người dùng có thể mở:

```text
http://localhost:8081/play?levelId=campaign_1
```

và chơi game trực tiếp trong browser.

## 2. Phạm vi

### Làm trong phase này

- [ ] Tạo LibGDX HTML5/GWT target.
- [ ] Giữ desktop target để debug.
- [ ] Tách code dùng chung thành `core`.
- [ ] Tạo browser-native WebSocket adapter.
- [ ] Load asset trong browser.
- [ ] Render map, tank, bullet, HP và effect hiện tại.
- [ ] Nhận keyboard input trong browser.
- [ ] Kết nối Spring Boot WebSocket `/game`.
- [ ] Nhận `CONNECT`, `ACK`, `STATE`, `ERROR`, `PONG`.
- [ ] Gửi `JOIN_GAME`, `MOVE_INPUT`, `SHOOT_INPUT`, `STOP`.
- [ ] Nhận `levelId` từ Thymeleaf page.
- [ ] Nhúng output HTML5 vào `play.html`.
- [ ] Test browser end-to-end.

### Không làm trong phase này

- Không thêm vũ khí mới.
- Không thêm loại tank mới.
- Không thêm AI mới.
- Không thêm shop/research/save.
- Không đổi game rule server.
- Không chuyển GameLoop sang client.
- Không xóa React client trước khi parity pass.
- Không xóa desktop launcher.

## 3. Trạng thái hiện tại

### Đã có

```text
game-client/
├── GameClient.java
├── DesktopLauncher.java
├── GameRenderer.java
├── AssetRepository.java
├── ClientInputProcessor.java
├── WebSocketBridge.java
├── StatePacketParser.java
└── PacketBuilder.java
```

Desktop client hiện đã:

- Kết nối Spring Boot server.
- Gửi JOIN/MOVE/SHOOT.
- Nhận STATE.
- Render map, tank, bullet và HP.
- Chạy bằng LWJGL3.

### Chưa có

- HTML5/GWT module.
- `HtmlLauncher`.
- Browser WebSocket bridge.
- HTML5-compatible asset loading.
- Browser build output.
- Script nhúng vào Thymeleaf.

## 4. Kiến trúc đích

```text
game-client/
├── settings.gradle
├── build.gradle
├── core/
│   └── src/main/java/com/tankzors/client/
│       ├── TankzorsGame.java
│       ├── assets/
│       ├── input/
│       ├── model/
│       ├── network/
│       │   ├── GameSocket.java
│       │   ├── PacketBuilder.java
│       │   └── StatePacketParser.java
│       ├── render/
│       └── screen/
│           ├── GameScreen.java
│           ├── LoadingScreen.java
│           └── ErrorScreen.java
│
├── desktop/
│   └── src/main/java/com/tankzors/client/desktop/
│       └── DesktopLauncher.java
│
├── html/
│   ├── src/main/java/com/tankzors/client/html/
│   │   ├── HtmlLauncher.java
│   │   └── BrowserWebSocket.java
│   └── webapp/
│       ├── index.html
│       └── assets/
│
└── assets/
    ├── tanks/
    ├── effects/
    ├── map/
    └── ui/
```

## 5. Dependency boundaries

```text
core    -> LibGDX core, shared protocol, không desktop API
desktop -> core + LWJGL3 + Java-WebSocket
html    -> core + GWT/HTML backend + browser WebSocket bridge
server  -> Spring Boot + WebSocket + game engine
```

### Core không được import

- `org.java_websocket.*`
- `com.badlogic.gdx.backends.lwjgl3.*`
- `java.awt.*`
- `java.net.http.WebSocket`
- `java.io.File` cho asset runtime
- Spring/Javalin/JPA

Core chỉ phụ thuộc vào interface:

```java
public interface GameSocket {
    void connect();
    void disconnect();
    void send(String message);
    void onMessage(Consumer<String> listener);
}
```

Desktop và HTML implement interface này khác nhau.

## 6. Phase A - Chuyển project sang multi-module build

### Mục tiêu

Tách desktop code hiện tại thành `core` và `desktop` trước khi tạo HTML5.

### Việc cần làm

- [ ] Giữ lại code hiện tại làm reference.
- [ ] Tạo Gradle LibGDX project với `core`, `desktop`, `html`.
- [ ] Copy class dùng chung từ `game-client/src/main/java` vào `core`.
- [ ] Chuyển `GameClient` thành `TankzorsGame` hoặc `GameApplication` trong core.
- [ ] Chuyển `DesktopLauncher` vào desktop module.
- [ ] Chuyển asset vào thư mục asset chuẩn LibGDX.
- [ ] Tạo desktop `GameSocket` implementation dùng Java-WebSocket.
- [ ] Chạy desktop client sau khi tách module.

### Nghiệm thu

- Desktop client vẫn mở được.
- Map/tank/bullet/input/network không thay đổi behavior.
- `core` compile không cần LWJGL3.

## 7. Phase B - Tách network adapter

### Mục tiêu

Core không biết transport cụ thể.

### Desktop adapter

```text
desktop/.../DesktopWebSocket.java
  -> org.java_websocket.client.WebSocketClient
```

### HTML adapter

```text
html/.../BrowserWebSocket.java
  -> browser native WebSocket
```

### Browser WebSocket behavior

Phải hỗ trợ:

- `onopen`.
- `onmessage`.
- `onerror`.
- `onclose`.
- `send`.
- reconnect có giới hạn.
- đóng socket khi dispose game.

### Lưu ý

Không dùng `Java-WebSocket` trong HTML target. Thư viện desktop socket không chạy trong browser.

## 8. Phase C - Tạo HTML5/GWT launcher

### Mục tiêu

Build game thành JavaScript/HTML chạy trong browser.

### Việc cần làm

- [ ] Tạo `HtmlLauncher`.
- [ ] Tạo GWT module descriptor.
- [ ] Cấu hình asset path.
- [ ] Tạo `html/webapp/index.html`.
- [ ] Cấu hình canvas id `embed-html` hoặc id thống nhất.
- [ ] Cấu hình viewport/responsive canvas.
- [ ] Tạo loading screen.
- [ ] Tạo browser WebSocket bridge.
- [ ] Đảm bảo code core không dùng API GWT-incompatible.

### Build commands dự kiến

```cmd
gradlew.bat html:superDev
gradlew.bat html:dist
```

Nếu project dùng wrapper khác, ghi chính xác command vào `game-client/README.md`.

### Output yêu cầu

```text
game-client/html/build/dist/
├── index.html
├── html.nocache.js hoặc bootstrap tương đương
├── assets/
└── generated JavaScript files
```

Không hard-code tên file hash nếu build tool tạo hash. Dùng copy task hoặc manifest để đưa đúng output vào server.

## 9. Phase D - Browser input và rendering

### Input

Giữ mapping hiện tại:

```text
W / ArrowUp       -> MOVE UP
A / ArrowLeft     -> MOVE LEFT
S / ArrowDown     -> MOVE DOWN
D / ArrowRight    -> MOVE RIGHT
Space / J         -> SHOOT
Release key       -> STOP
```

Browser phải prevent default cho arrow keys để trang không scroll khi chơi.

### Rendering

Giữ parity với desktop:

- Map 20x15.
- Tile size hiện tại.
- Player tank.
- Enemy tank.
- Bullet.
- HP bar.
- Explosion effect.
- FPS nếu đang có.

Không chuyển collision sang client. Client chỉ render state server.

## 10. Phase E - Nhận level từ Thymeleaf

### `play.html`

Thymeleaf truyền level qua data attribute:

```html
<div id="game-container"
     th:attr="data-level-id=${selectedLevelId}">
</div>
```

Hoặc truyền bằng query string:

```text
/play?levelId=campaign_2
```

### JavaScript bootstrap

Bootstrap page phải lấy:

```javascript
const levelId = document
    .getElementById('game-container')
    .dataset.levelId || 'campaign_1';
```

Sau đó truyền level vào LibGDX launcher/game config.

Không dùng React bundle trong `play.html`.

## 11. Phase F - Nhúng output vào Spring Boot

### Copy output

Copy HTML5 distribution vào:

```text
server-spring/src/main/resources/static/game/libgdx/
```

Có thể dùng Gradle copy task:

```text
game-client/html/build/dist
        ↓
server-spring/src/main/resources/static/game/libgdx
```

### `play.html` cần load

- LibGDX generated bootstrap script.
- HTML5 canvas/container.
- CSS game page.
- Không load React.

### Same-origin

Ưu tiên chạy cùng origin:

```text
Website: http://localhost:8081
WebSocket: ws://localhost:8081/game
```

Không cần CORS riêng nếu page và WebSocket cùng host/port. Khi deploy HTTPS, dùng `wss://`.

## 12. Phase G - WebSocket protocol parity

### Client gửi

- `JOIN_GAME` với `levelId`.
- `MOVE_INPUT`.
- `SHOOT_INPUT`.
- `STOP`.
- `PING`.

### Client nhận

- `CONNECT`.
- `ACK`.
- `STATE`.
- `PONG`.
- `ERROR`.

### Không được thay đổi

- Endpoint `/game`.
- Tên action hiện tại.
- Shape của state nếu chưa update server/client cùng lúc.
- Server tick rate.
- Session ID behavior.

## 13. Phase H - Testing

### Build test

- [x] `core` compile.
- [x] `desktop` compile/package (`gradlew.bat desktop:fatJar`).
- [ ] `html:superDev` chạy. (dev server long-running, test thủ công khi cần debug)
- [x] `html:dist` tạo output.
- [x] Output copy vào Spring Boot static (`gradlew.bat html:copyToServer`).
- [x] Spring Boot package chứa output HTML5 (`mvn clean package -DskipTests`).

### Browser smoke test

1. Start PostgreSQL.
2. Start Spring Boot.
3. Mở `/play?levelId=campaign_1`.
4. Không còn placeholder.
5. Canvas LibGDX hiển thị.
6. Nhận `CONNECT`.
7. Join game.
8. Tank player và enemy xuất hiện.
9. W/A/S/D di chuyển.
10. Space/J bắn.
11. Bullet xuất hiện.
12. Collision server hoạt động.
13. HP thay đổi khi bị bắn.
14. Victory/defeat hiển thị.
15. Back to campaigns hoạt động.

### Browser console test

Không được có lỗi:

- `WebSocket is not defined`.
- `ClassNotFoundException`.
- `NoSuchMethodError`.
- `Unable to load asset`.
- `GWT module not found`.
- `404 html.nocache.js`.
- `404 asset path`.

### Desktop regression

Sau khi thay đổi core, vẫn chạy:

```cmd
java -jar game-client\desktop\build\libs\tankzors-desktop-1.0-SNAPSHOT.jar --ws ws://localhost:8081/game --level campaign_1
```

(lệnh Maven `game-client\target\...jar` cũ đã lỗi thời — client chuyển sang Gradle;
xem `game-client/README.md` mục Build & Run)

Desktop behavior phải không bị phá.

### Kết quả verify Phase G/H (16/09/2026)

Wire-level qua PowerShell ClientWebSocket tới `ws://localhost:8081/game`:

- CONNECT (sessionId) / ACK JOIN_GAME / ACK MOVE_INPUT / ACK STOP / PONG / ERROR 400 / STATE 30Hz: PASS.
- STATE shape khớp protocol-v1.md 3.5 (map 20×15, tileSize 40, PLAYING, tank id == sessionId).
- Server không đổi code.
- Build: core compile, desktop fatJar, html:dist (hash mới `393A407C…cache.js` chứa PING/STOP),
  copyToServer, jar Spring Boot chứa static mới: PASS.
- Desktop regression: Boot → CONNECT sessionId → first STATE PLAYING tanks=2: PASS.
- Chi tiết: `game-client/README.md` mục "Phase G/H — verify".

Còn lại: browser smoke test thủ công (mục trên, bước 1–15) + `html:superDev` debug mode.

## 14. Phase I - Cutover

Chỉ thực hiện sau khi browser parity pass:

- [ ] Đổi `/play` từ placeholder sang LibGDX HTML5.
- [ ] Xóa placeholder text.
- [ ] Giữ React client cho một commit rollback.
- [ ] Đánh dấu React client deprecated.
- [ ] Không xóa React/Javalin trong cùng commit với HTML5 integration.
- [ ] Sau một vòng regression pass mới xóa legacy nếu team xác nhận.

## 15. Definition of Done

- [ ] LibGDX core/desktop/html build được.
- [ ] Browser load game từ `/play`.
- [ ] Browser dùng native WebSocket.
- [ ] Spring Boot nhận input và broadcast state.
- [ ] Map/tank/bullet/effect render đúng.
- [ ] Movement/shoot/collision/HP giữ parity.
- [ ] Victory/defeat giữ parity.
- [ ] Asset path hoạt động sau khi đóng gói JAR.
- [ ] Desktop client vẫn chạy.
- [ ] Không cần React để chơi game trên browser.
- [ ] Không xóa legacy trước parity verification.

## 16. Prompt giao cho AI implement

```text
Đọc trước:
- .planning/PROJECT.md
- .planning/ROADMAP.md
- .planning/REFACTOR-PLAN.md
- .planning/AI-IMPLEMENTATION-GUIDE.md
- .planning/THYMELEAF-UI-IMPLEMENTATION.md
- .planning/LIBGDX-HTML5-IMPLEMENTATION.md
- docs/protocol-v1.md
- docs/parity-checklist.md
- game-client/README.md
- server-spring/README.md

Nhiệm vụ: hoàn thiện LibGDX HTML5 để chơi được trong browser tại
/play?levelId=campaign_1.

Giữ nguyên game behavior hiện tại. Không thêm gameplay mới.
Không xóa React client hoặc Javalin server trước khi parity pass.
Không dùng Java-WebSocket trong HTML5 target.
Tách core/desktop/html và dùng browser-native WebSocket cho html.

Trước khi code:
1. Kiểm tra game-client hiện tại.
2. Kiểm tra build tool hiện tại.
3. Liệt kê file sẽ tạo/sửa.
4. Chốt cách copy HTML5 output vào Spring Boot static.

Sau khi code:
1. Chạy desktop regression.
2. Build HTML5.
3. Start Spring Boot.
4. Test browser /play.
5. Báo cáo console errors, asset errors và WebSocket result.
6. Ghi rõ phần chưa hoàn thành.
```
