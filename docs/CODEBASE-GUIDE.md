# Tankzors Codebase Guide

Tai lieu nay giup nguoi moi doc source hien tai cua Tankzors sau khi da chuyen sang stack:

- Spring Boot + Thymeleaf cho website.
- LibGDX core + LibGDX HTML5/GWT cho game chay tren web.
- WebSocket cho realtime gameplay.
- PostgreSQL + Flyway cho luu tru.

Pham vi tai lieu chi gom nhung folder dang duoc dung trong luong hien tai. Cac folder khong nam trong danh sach ben duoi duoc xem la legacy/ngoai pham vi doc source moi.

## 1. Nhin nhanh kien truc hien tai

```text
Browser
  |
  | HTTP /play, /campaigns, /leaderboard
  v
server-spring
  |-- Thymeleaf render website
  |-- Static serve LibGDX HTML5 bundle
  |-- WebSocket /game
  |-- Game loop + AI + collision + match result
  v
PostgreSQL

Trong trang /play:
  Thymeleaf page -> iframe/static bundle -> LibGDX HTML5 client
                                      -> WebSocket ws://localhost:8081/game
```

Y tuong chinh:

- Website/menu/campaign/leaderboard/settings nam o `server-spring`.
- Gameplay realtime ve bang LibGDX nam o `game-client`.
- Server la nguon su that cho game state: AI, va cham, mau, dan, win/lose.
- Client chi render, nhan input nguoi choi, gui input len WebSocket, nhan state tu server.
- Bundle HTML5 sinh ra tu `game-client` duoc copy vao `server-spring/src/main/resources/static/game/libgdx`.

## 2. Folder dang dung

```text
LapTrinhMang/
  server-spring/          Spring Boot app, website, websocket, game engine, DB
  game-client/            LibGDX client: core, desktop launcher, HTML5 launcher
  shared-protocol/        Java DTO/packet contract dung lam protocol target
  infra/                  Docker Compose PostgreSQL dev
  docs/                   Tai lieu kien truc, protocol, runbook, guide nay
  README.md               Huong dan tong quan o root
```

Khong sua truc tiep cac output build/generated:

- `game-client/**/build/`
- `game-client/.gradle/`
- `shared-protocol/target/`
- `server-spring/target/`
- `server-spring/src/main/resources/static/game/libgdx/` neu muc tieu la sua source game. Thu muc nay la bundle HTML5 da build ra.

Neu sua gameplay LibGDX, sua trong `game-client/core` roi build lai `html:copyToServer`.

## 3. Server Spring Boot

Folder chinh: `server-spring/src/main/java/com/tankzors`

```text
com.tankzors
  TankzorsApplication.java     Entry point Spring Boot
  ServerConfig.java            Cau hinh server chung

  web/                         Controller website + settings + exception pages
  network/                     WebSocket endpoint, session, parse input, publish state
  network/protocol/            Message classes server dang dung de gui/nhan JSON
  application/                 Use case/service layer: campaign, progress, leaderboard, match end
  engine/                      Game loop, game state, collision, bullet, match observer
  entity/                      Tank, bullet, wall, item, position, rectangle
  ai/                          Hanh vi AI tank va pathfinding
  map/                         Load map JSON va tile map
  progress/                    Entity/repository tien do nguoi choi
  leaderboard/                 Entity/repository leaderboard
  config/                      Engine lifecycle, Gson, config bean
  interfaces/                  Interface domain/gameplay cu the tach layer
```

Nhung class nen doc theo thu tu:

1. `TankzorsApplication.java`
2. `web/PageController.java`
3. `network/GameWebSocketHandler.java`
4. `application/GameSessionService.java`
5. `engine/GameLoop.java`
6. `engine/GameState.java`
7. `engine/CollisionManager.java`
8. `engine/BulletManager.java`
9. `application/MatchEndService.java`

Vai tro tung nhom:

| Nhom | Sua khi nao |
|---|---|
| `web/` | Sua route website, page Thymeleaf, settings, leaderboard page |
| `network/` | Sua ket noi WebSocket, parse input client, publish state realtime |
| `application/` | Sua use case: load campaign, luu progress, tinh diem, ket thuc tran |
| `engine/` | Sua logic gameplay: tick loop, collision, bullet, win/lose |
| `entity/` | Sua model game: tank, bullet, wall, position |
| `ai/` | Sua tank dich, patrol/chase/shoot, difficulty |
| `map/` | Sua load map, tile type, map JSON |
| `progress/`, `leaderboard/` | Sua persistence PostgreSQL |

## 4. Server resources

Folder: `server-spring/src/main/resources`

```text
application.yml                 Port, datasource, spring config
db/migration/                   Flyway SQL migration
maps/campaign_*.json            Du lieu map campaign
templates/                      Thymeleaf page
templates/fragments/            Navbar, head, footer, flash message
templates/error/                Trang loi
static/css/                     CSS website
static/game/libgdx/             LibGDX HTML5 bundle da copy tu game-client
```

Page quan trong:

| File | Chuc nang |
|---|---|
| `templates/layout.html` | Layout chung |
| `templates/menu.html` | Home/menu |
| `templates/campaigns.html` | Chon campaign |
| `templates/play.html` | Trang nhung game LibGDX HTML5 |
| `templates/leaderboard.html` | Bang xep hang |
| `templates/settings.html` | Cai dat |

Map campaign hien tai nam o:

- `maps/campaign_1.json`
- `maps/campaign_2.json`
- `maps/campaign_3.json`

Them man moi thi uu tien them file map JSON, sau do cap nhat service/query campaign tuong ung.

## 5. Game Client LibGDX

Folder chinh: `game-client`

```text
game-client/
  settings.gradle
  build.gradle
  gradlew.bat

  assets/                       Sprite/texture dung chung cho desktop va html

  core/                         Source gameplay client cross-platform
    src/main/java/com/tankzors/client

  desktop/                      Launcher test nhanh tren desktop
    src/main/java/com/tankzors/client/desktop

  html/                         Launcher GWT/HTML5 de chay trong browser
    src/main/java/com/tankzors/client/html
```

Trong `game-client/core`:

| Package/class | Vai tro |
|---|---|
| `GameClient.java` | App LibGDX chinh, noi input, socket, render, overlay win/lose |
| `assets/AssetRepository.java` | Load texture/sprite |
| `input/ClientInputProcessor.java` | WASD/mui ten, Space/J ban, R restart, N next level |
| `model/*Snapshot.java` | Client-side snapshot cua state server gui ve |
| `network/GameSocket.java` | Interface socket de core khong phu thuoc desktop/html |
| `network/PacketBuilder.java` | Tao JSON packet client gui len server |
| `network/StatePacketParser.java` | Parse state/result tu server |
| `render/GameRenderer.java` | Ve map, tank, bullet, HP, overlay |

Trong `game-client/desktop`:

- `DesktopLauncher.java`: chay client bang JVM de test nhanh.
- `DesktopWebSocket.java`: WebSocket adapter cho desktop.

Trong `game-client/html`:

- `HtmlLauncher.java`: entry point cho GWT/HTML5.
- `BrowserWebSocket.java`: WebSocket adapter tren browser.
- `TankzorsHtml.gwt.xml`: GWT module config.

Quy tac quan trong:

- Logic render/input chung dat trong `core`.
- Code phu thuoc browser chi dat trong `html`.
- Code phu thuoc desktop chi dat trong `desktop`.
- Khong sua generated JS trong `server-spring/static/game/libgdx` bang tay.

## 6. WebSocket gameplay flow

Luong vao trang choi:

```text
User mo /play?levelId=campaign_1
  -> PageController render templates/play.html
  -> play.html nhung static/game/libgdx/index.html
  -> HtmlLauncher tao GameClient
  -> BrowserWebSocket ket noi /game
  -> Client gui JOIN_GAME
  -> Server tao/lay session game
  -> GameLoop tick
  -> WebSocketStatePublisher gui STATE lien tuc
  -> Client parse STATE va render
```

Input flow:

```text
Nguoi choi bam WASD/Arrow/Space/J
  -> ClientInputProcessor
  -> PacketBuilder tao MOVE_INPUT/SHOOT_INPUT
  -> BrowserWebSocket gui JSON
  -> GameWebSocketHandler nhan
  -> InputPacketParser parse
  -> GameSessionService cap nhat input vao GameState
```

Match end flow:

```text
GameState phat hien win/lose
  -> MatchEndObserver
  -> MatchEndService tinh score/progress
  -> WebSocketStatePublisher gui RESULT/STATE co status
  -> GameClient khoa input gameplay va hien overlay
```

## 7. Protocol

Tai lieu chi tiet nam o `docs/protocol-v1.md`.

Hien tai co 2 noi can chu y:

- `server-spring/src/main/java/com/tankzors/network/protocol`: message server dang dung runtime.
- `shared-protocol/src/main/java/com/tankzors/protocol`: packet contract target de thong nhat DTO ve sau.

Khi sua protocol, can check ca 3 cho:

1. Server parse/publish trong `server-spring/network`.
2. Client build/parse trong `game-client/core/network`.
3. Tai lieu `docs/protocol-v1.md`.

Nguyen tac: server va client phai cung hieu `type`, field toa do, HP, status, bullet, map.

## 8. Database va infra

PostgreSQL dev nam o `infra/docker-compose.yml`.

Thong tin mac dinh:

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `tankzors` |
| User | `tankzors` |
| Password | `tankzors_dev_password` |

Migration nam o:

- `server-spring/src/main/resources/db/migration/V1__init.sql`
- `server-spring/src/main/resources/db/migration/V2__player_progress.sql`

Chay PostgreSQL:

```powershell
docker compose -f infra/docker-compose.yml up -d
```

Dung PostgreSQL:

```powershell
docker compose -f infra/docker-compose.yml down
```

## 9. Lenh chay local

Tu root repo:

```powershell
cd /d D:\BTLLapTrinhMang\LapTrinhMang
```

Neu dang dung PowerShell thi `cd /d` khong hop le. Dung:

```powershell
Set-Location D:\BTLLapTrinhMang\LapTrinhMang
```

Build server:

```powershell
.\mvnw.cmd -pl server-spring -am clean package
```

Run server:

```powershell
java -jar server-spring\target\tankzors-server-spring-1.0-SNAPSHOT.jar
```

Mo web:

```text
http://localhost:8081
http://localhost:8081/play?levelId=campaign_1
```

Build lai LibGDX HTML5 sau khi sua `game-client`:

```powershell
Set-Location D:\BTLLapTrinhMang\LapTrinhMang\game-client
.\gradlew.bat html:dist html:copyToServer
```

Test client desktop:

```powershell
Set-Location D:\BTLLapTrinhMang\LapTrinhMang\game-client
.\gradlew.bat desktop:run --args="--ws ws://localhost:8081/game --level campaign_1"
```

Neu loi port 8081 dang duoc dung:

```powershell
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

## 10. Sua tinh nang thi vao dau

| Muon sua | Folder/file nen doc truoc |
|---|---|
| UI website menu/campaign/play | `server-spring/src/main/resources/templates`, `static/css` |
| Nhung game vao web | `templates/play.html`, `static/game/libgdx`, `game-client/html` |
| Ve tank/map/bullet | `game-client/core/render/GameRenderer.java`, `game-client/assets` |
| Phim dieu khien | `game-client/core/input/ClientInputProcessor.java` |
| Packet client gui server | `game-client/core/network/PacketBuilder.java` |
| Parse state server | `game-client/core/network/StatePacketParser.java` |
| WebSocket server | `server-spring/src/main/java/com/tankzors/network` |
| AI tank dich | `server-spring/src/main/java/com/tankzors/ai` |
| Va cham/dan/mau | `server-spring/src/main/java/com/tankzors/engine` |
| Map/man choi | `server-spring/src/main/resources/maps`, `server-spring/src/main/java/com/tankzors/map` |
| Win/lose/score/progress | `application/MatchEndService.java`, `application/ScoreCalculator.java`, `progress/` |
| Leaderboard | `application/LeaderboardService.java`, `leaderboard/`, `templates/leaderboard.html` |
| Database schema | `server-spring/src/main/resources/db/migration` |

## 11. Debug nhanh

Game khong hien trong web:

- Check server da run port `8081`.
- Mo DevTools Console xem loi JS/GWT.
- Check `server-spring/src/main/resources/static/game/libgdx/index.html` co ton tai.
- Neu vua sua client, chay lai `.\gradlew.bat html:dist html:copyToServer`.

Game hien nhung khong di chuyen:

- Check WebSocket `/game` co connect khong.
- Check Console co loi parse state/protocol khong.
- Check server log co nhan input khong.

Ban chet dich ma UI khong doi:

- Check server state co gui `status`/result match end khong.
- Check `StatePacketParser` parse du field status/result khong.
- Check `GameClient` overlay va input lock da nhan dung status khong.

Chu trong overlay bi nguoc/loi font:

- Check `BitmapFont` trong `GameRenderer`.
- GWT/LibGDX HTML5 co khac desktop ve font/camera; uu tien test lai bang browser sau khi build.

## 12. Nguyen tac refactor tiep theo

- Game rule nam tren server, khong day logic win/lose/AI sang client.
- Client chi render state va gui input.
- Protocol thay doi thi update server, client, docs cung luc.
- Them page website thi dung Thymeleaf trong `server-spring`, khong them React.
- Them asset thi dat vao `game-client/assets`, build lai HTML5 bundle.
- Them database table thi tao Flyway migration moi, khong sua migration cu da apply.
- Khong sua generated bundle trong `static/game/libgdx`; source goc nam o `game-client`.

## 13. Thu tu doc cho nguoi moi

1. Doc file nay.
2. Doc `README.md` o root de nam cach chay tong quan.
3. Doc `server-spring/README.md`.
4. Doc `game-client/README.md`.
5. Doc `docs/protocol-v1.md`.
6. Chay server va vao `/play?levelId=campaign_1`.
7. Sua mot thay doi nho o `GameRenderer` hoac CSS, build lai, quan sat thay doi.

Sau khi nam luong nay, moi nen di vao cac phan sau: AI nang cao, them map, score/progress, leaderboard, multiplayer that.
