# Planning: chuyển toàn bộ gameplay sang client LibGDX HTML5

Ngày lập: 16/09/2026. Trạng thái: tài liệu triển khai, chưa thay đổi gameplay.

## 1. Yêu cầu bắt buộc và phạm vi

Tài liệu này dành cho AI triển khai trên repository hiện tại. Đọc code thực tế trước khi sửa; các tên lớp mới bên dưới là thiết kế đề xuất, không phải lớp đã tồn tại.

- Giữ Spring Boot + Thymeleaf + LibGDX HTML5/GWT + WebSocket + PostgreSQL. Không đưa React vào.
- Toàn bộ luật gameplay chạy trên client: di chuyển, bắn, cooldown, đạn, va chạm, AI, sát thương, chết, vật phẩm đã có, thắng/thua, thời gian trận và điểm.
- Campaign: client tự chạy trận, không cần WebSocket để điều khiển gameplay.
- Multiplayer: một client làm host và chạy mô phỏng; các client khác gửi input, nhận snapshot qua server relay. Host cũng là client HTML5, không phải tiến trình server.
- Server chỉ phục vụ website/dữ liệu, quản lý phòng/kết nối/quyền gửi gói, và lưu kết quả client báo về. Server không tính lại điểm, không chạy AI hay xác định thắng/thua.
- Giữ hành vi campaign hiện có; không tự thêm màn, vũ khí, kỹ năng, hệ thống tài khoản hay gameplay chưa có.
- Multiplayer tối thiểu của đợt này là phòng 2 người cùng đánh AI trên map campaign. Đây là phạm vi mới được xác định trong plan; không suy diễn rằng code hiện tại đã có phòng multiplayer hoàn chỉnh. PvP, matchmaking và chuyển host nằm ngoài phạm vi.
- Không sửa trực tiếp JavaScript/bundle sinh tự động trong `server-spring/src/main/resources/static/game/libgdx`.
- Chỉ code và cập nhật test trong các bước triển khai. Chạy build/test/runtime verification tập trung ở bước cuối theo mục 10.

Ý nghĩa “xử lý hết ở client”: gameplay thuộc client. Kiểm tra JSON, quyền vào phòng, giới hạn kích thước gói và giao dịch database vẫn thuộc backend.

## 2. Hiện trạng đã đối chiếu với source

| Vị trí hiện tại | Hành vi | Thay đổi cần có |
|---|---|---|
| `game-client/core/.../client/GameClient.java` | Kết nối socket trong `create()`, gửi input, chờ STATE để render | Điều phối session local/host/guest; campaign tự tạo state |
| `client/input/ClientInputProcessor.java` | Callback move, shoot, release, restart, next | Giữ bắt phím, chuyển callback sang session |
| `client/render/GameRenderer.java`, `client/model/*Snapshot` | Render snapshot và UI kết quả | Tái sử dụng qua adapter từ world local; nhận đúng localPlayerId |
| `client/network/PacketBuilder.java`, `StatePacketParser.java` | Protocol server-authoritative cũ | Version hóa protocol relay, tách khỏi simulation |
| `html/.../HtmlLauncher.java` | Luôn tạo BrowserWebSocket | Chọn campaign/host/guest theo bootstrap |
| `server-spring/.../application/GameSessionService.java` | Đưa input vào GameLoop | Thay bằng dịch vụ phòng/relay |
| `engine/GameLoop.java` | Scheduler 30Hz; input, AI, collision, terminal state | Tách logic step sang core; bỏ scheduler server |
| `engine/GameState.java`, `entity/`, `ai/`, `map/`, `interfaces/` | Model và luật gameplay | Port những thành phần đang dùng sang client core |
| `config/EngineConfig.java`, `EngineLifecycle.java` | Khởi tạo và chạy engine server | Gỡ engine beans/lifecycle; giữ bean hạ tầng còn cần |
| `application/MatchEndService.java`, `ScoreCalculator.java` | Nhận callback engine, tính điểm, lưu/unlock | Điểm tính ở client; backend nhận kết quả và lưu |
| `web/ProgressController.java`, `PageController.java` | Danh tính ưu tiên X-Player-Session, fallback HttpSession | Thống nhất identity HTTP cho page/API/WS handshake |
| `PageController.submitScoreFromClient()` | Đã có endpoint submit điểm riêng | Hợp nhất với endpoint kết quả có chống ghi trùng |
| `resources/maps/campaign_1..3.json` | Dữ liệu màn phía server | Giữ làm nguồn chuẩn, cung cấp dữ liệu cho client |
| `game-client/html/build.gradle` | dist, manifest asset và copyToServer bằng Sync | Giữ pipeline, cập nhật bootstrap/assets nếu cần |

`GameLoop` hiện chứa Java concurrency, SLF4J và cú pháp Java mới; không copy nguyên lớp sang GWT. Core Gradle hiện đặt Java 17, backend Java 21; mức Java JVM không chứng minh API/cú pháp đó chạy được trên GWT.

`shared-protocol` có DTO riêng nhưng runtime hiện dùng cả protocol nội bộ server và parser client. Không mặc định đây đã là contract dùng chung hoàn chỉnh.

Trước khi sửa: đọc AGENTS.md nếu có, git status/diff, build files, các file trên, test đang có, map JSON và docs liên quan. Bảo toàn thay đổi sẵn có của người dùng.

## 3. Kiến trúc đích

### Campaign

`Input -> LocalCampaignSession -> GameSimulation.step(dt) -> WorldSnapshot -> GameRenderer`

Khi terminal: client tạo MatchResult, hiện kết quả ngay, gửi REST để lưu. Lưu thất bại không làm biến mất kết quả và không ngừng render.

### Multiplayer

`Guest input -> WebSocket relay -> Host session -> GameSimulation -> snapshot -> relay -> guests`

Host dùng cùng GameSimulation với campaign. Server không tạo GameState và không tiến hành simulation tick. Guest chỉ render state host gửi; dự đoán/reconciliation chưa bắt buộc ở lần chuyển đổi này. Có thể nội suy hình ảnh, nhưng không dùng vị trí nội suy để quyết định va chạm.

Chốt MVP: tối đa 2 người, chỉ vào phòng ở lobby, host bấm bắt đầu khi đủ 2 người sẵn sàng. Dùng map và AI hiện tại; không thêm luật PvP. Đồng đội không gây sát thương cho nhau. Thắng khi không còn AI sống; thua khi tất cả người chơi chết; người chết vẫn xem được đồng đội. Đây là luật co-op mới, phải tách khỏi luật campaign để giữ parity chơi đơn.

Host đóng tab, mất kết nối hoặc quá hạn heartbeat: kết thúc phiên multiplayer với lý do HOST_DISCONNECTED; không ghi là thua gameplay và không lưu điểm. Guest rời trận: MVP kết thúc phiên với PLAYER_DISCONNECTED. Không tự chuyển host hay tự tái tham gia trận đang chạy.

## 4. Cấu trúc thư mục đề xuất

Giữ nguyên Gradle multi-module hiện có, chưa cần tạo thêm module engine riêng.

```text
game-client/core/src/main/java/com/tankzors/client/
  GameClient.java
  gameplay/
    GameSimulation.java
    GameWorld.java
    GameplayConfig.java
    command/             # PlayerCommand, input state
    entity/              # Tank, PlayerTank, AITank, Bullet...
    collision/           # Va chạm và xử lý trúng đạn
    ai/                  # AIController, navigation, behaviors
    map/                 # MapDefinition, loader/parser GWT-compatible
    rules/               # CampaignRules, CoopRules, ScoreCalculator
    result/              # MatchResult, trạng thái kết thúc
  session/
    GameSession.java
    LocalCampaignSession.java
    HostGameSession.java
    GuestGameSession.java
  input/                 # Bắt phím, không chứa luật gameplay
  render/                # Renderer và WorldSnapshotMapper
  model/                 # Snapshot dành cho render
  network/               # GameSocket, relay codec, packet DTO
  persistence/           # ResultGateway, submission state machine
  assets/
game-client/html/src/main/java/com/tankzors/client/html/
  HtmlLauncher.java
  BrowserWebSocket.java
  BrowserResultGateway.java
  BrowserBootstrapLoader.java
game-client/desktop/src/main/java/.../
  DesktopLauncher.java    # Vẫn chạy campaign local để debug
  ...                    # Adapter mạng/HTTP nếu desktop cần
server-spring/src/main/java/com/tankzors/
  web/                   # Thymeleaf, bootstrap API, result API
  room/                  # RoomRegistry, RoomService, metadata
  network/               # WS handler, session registry, relay codec
  application/           # Progress, leaderboard, result persistence
  progress/
  leaderboard/
  result/                # Entity/repository chống ghi trùng kết quả
  config/                # Cấu hình hạ tầng, không có engine lifecycle
```

Tất cả gameplay đặt dưới `com.tankzors.client` để nằm trong source path `client` của `Tankzors.gwt.xml`. Core không import Spring, JPA, servlet, server protocol, SLF4J, java.net hoặc backend LibGDX cụ thể. Map DTO phục vụ HTTP phía server có thể tồn tại riêng, nhưng server không phụ thuộc module simulation.

Không tạo hàng loạt interface rỗng. Chỉ dùng interface cho ranh giới session, transport, tải dữ liệu và lưu kết quả.

## 5. Quy tắc port gameplay và lifecycle

### Simulation

- Trích pipeline từ GameLoop: nhận command -> cập nhật xe/cooldown/AI -> cập nhật đạn -> collision/damage -> cleanup -> xác định terminal -> snapshot.
- World chỉ bị thay đổi trên game thread. Callback HTTP/WS đưa dữ liệu vào queue rồi áp dụng trong update/render loop.
- Không mang ScheduledExecutorService, ConcurrentHashMap, AtomicBoolean hay Thread.sleep vào core simulation. Thay bằng collection thông thường theo mô hình single-thread.
- Fixed step ban đầu 1/30 giây để gần nhịp cũ; render độc lập. Accumulator giới hạn delta nhận vào tối đa 0,25 giây và tối đa 5 step/frame; bỏ backlog dư có chủ đích để không chạy bù vô hạn.
- Cooldown và elapsed time dựa trên simulation time tích lũy. Pause không tăng thời gian trận. Không dùng System.currentTimeMillis cho luật gameplay.
- Giữ tốc độ, HP, kích thước hitbox, độ khó AI, vật cản và spawn của campaign hiện tại. Kiểm tra toàn bộ import sau khi chuyển các entity/interfaces.
- Map loading dùng JSON parser tương thích GWT, đọc field rõ ràng; tránh Gson reflection trong core. Không dùng ClassLoader/InputStream đọc resource kiểu server.
- AI pathfinding giữ hành vi hiện tại; nguồn ngẫu nhiên được đưa vào simulation với seed để test tái lập.
- Tính điểm cuối tại client theo công thức ScoreCalculator hiện có: hpRemain * 10 + aiKills * 200 + max(0, 1000 - elapsedSeconds * 10), với đầu vào không âm.
- Phân biệt điểm tạm trên HUD và điểm cuối; overlay và payload lưu phải lấy cùng MatchResult. Đếm số AI bị tiêu diệt thực tế, không suy từ điểm tạm.
- Terminal chỉ phát một lần; không tiếp tục xử lý bắn/di chuyển/damage sau terminal. Restart tạo world, timer, counters và runId mới.
- Nếu player và AI cuối cùng chết cùng tick: campaign ưu tiên DEFEAT như logic hiện tại; ghi rõ trong test.

### Input, render, trang web

- Local/host input gọi session, không gửi MOVE/SHOOT lên server để server xử lý.
- Guest có input sequence tăng dần; host bỏ input cũ/trùng, không nhân đôi lệnh bắn.
- Clear input khi mất focus, pause, đổi màn, restart và dispose. Host tự thả input guest quá hạn để tránh xe chạy mãi.
- Renderer lấy localPlayerId rõ ràng để hiển thị đúng HP; không chọn player đầu tiên trong danh sách ở co-op.
- Map chuẩn bị xong và có frame render đầu tiên thì báo GAME_READY, ẩn loading; không chờ STATE từ WS ở campaign.
- Bootstrap/map/asset lỗi phải hiện thông báo và nút thử lại, không treo “Đang tải game”.
- Nếu dùng postMessage giữa iframe và trang cha: xác nhận origin và source; không dùng wildcard origin.
- Giữ resize, focus, phím R/N, overlay thắng/thua hiện có; dùng camera/font riêng cho UI khi cần, không tái tạo lỗi chữ lật hoặc mất glyph tiếng Việt.
- Campaign pause khi tab ẩn. Host multiplayer gửi PAUSED khi tab ẩn, resume đặt lại accumulator; quá hạn mạng thì phòng đóng như disconnect. Không chạy bù hàng nghìn tick khi quay lại.
- R/N ở multiplayer chỉ host điều khiển qua trạng thái phòng; không cho guest tự thay map hoặc reset world.

## 6. Map, identity và kết quả REST

### Bootstrap và map

Đề xuất `GET /api/game/bootstrap?levelId=campaign_1` trả `protocolVersion`, `playerId`, `levelId`, `nextLevelId`, `mapVersion`, `map`, `settings`, `unlockedLevels` và `runId` cho lần chơi campaign. Schema map phải bám JSON hiện tại, không viết lại dữ liệu map thủ công.

Server trả dữ liệu, kiểm tra level tồn tại/quyền tải theo progression; không tạo tank hoặc world. `runId` là metadata lần chơi, không phải trạng thái mô phỏng. Restart tạo runId mới; việc tải/lưu ban đầu cần backend, nhưng gameplay sau khi tải xong tiếp tục được khi WS hoặc mạng mất.

Giữ `server-spring/src/main/resources/maps` làm nguồn map chuẩn. Nếu đóng gói map cho desktop/offline debug, dùng task copy từ nguồn chuẩn, không duy trì hai bản sửa tay. Không hứa hỗ trợ reload website hoàn toàn offline trong đợt này.

### Identity

- Tạo một PlayerIdentityResolver dùng chung cho page, progress API, bootstrap, result API và WS handshake.
- Hiện chưa lấy việc có tài khoản làm tiền đề. Dùng identity trong HttpSession hiện tại; không xây hệ thống login mới trong đợt này.
- Không dùng WebSocket sessionId làm khóa progression. WS sessionId chỉ là connectionId, playerId là identity ứng dụng.
- Không tin header X-Player-Session hoặc playerId tự khai để chọn chủ sở hữu dữ liệu. Resolve người gửi từ HTTP session/handshake.
- Phạm vi MVP: tiến độ tồn tại theo phiên trình duyệt; không hứa giữ identity khi cookie/session hết hạn. Nếu đã có dữ liệu WS-session cũ, bảo toàn dữ liệu đó nhưng không tự đoán ánh xạ sang người dùng mới.

### Gửi kết quả

Đề xuất endpoint duy nhất `POST /api/game/results` với body:

```json
{
  "runId": "issued-run-id",
  "mode": "CAMPAIGN",
  "levelId": "campaign_1",
  "mapVersion": "map-version",
  "status": "VICTORY",
  "score": 2300,
  "hpRemain": 70,
  "aiKills": 3,
  "elapsedSeconds": 40
}
```

Con số trên là ví dụ schema, không phải fixture điểm theo công thức.

- Body không quyết định identity. Backend kiểm tra run thuộc người gửi, level/mode khớp metadata, enum hợp lệ và số hữu hạn/không âm trong giới hạn lưu trữ.
- Không tính lại score, không replay để kiểm tra victory. Kết quả là client-reported và có thể bị sửa; tài liệu/UI bảng điểm phải diễn đạt đúng mức tin cậy này.
- Thêm Flyway migration với số tiếp theo thực tế, không sửa migration đã chạy. Lưu run/result và unique key chống ghi trùng.
- Cùng runId + cùng payload retry trả kết quả đã lưu; cùng runId + payload khác trả 409. Xử lý retry song song bằng unique constraint/giao dịch DB, không chỉ synchronized Java.
- Lưu receipt, cập nhật best score và unlock trong một transaction. Nếu lỗi DB, rollback và trả lỗi; không trả ACK thành công trước khi commit.
- Chỉ VICTORY campaign mở màn kế tiếp. Không dùng endpoint cũ làm lối ghi bypass; thay caller rồi bỏ route cũ hoặc đưa qua cùng dịch vụ/idempotency.
- Response trả trạng thái lưu, bestScore và unlockedLevels. Client refresh UI từ response/API, không tự giả rằng DB đã lưu.
- Client có trạng thái NOT_SENT/SENDING/SAVED/FAILED; retry giữ cùng runId/payload, tối đa 3 lần tự động rồi nút “Lưu lại”. Không retry mỗi frame.
- Multiplayer: host gửi MATCH_RESULT qua relay; server kiểm tra host/phòng/runId, lưu bản báo cáo trận co-op và chuyển tiếp kết quả. Không nhập điểm co-op vào bảng campaign hay unlock campaign trong MVP. Result có danh sách participant đã chốt từ lúc bắt đầu; backend không tin danh sách người nhận tự khai.

## 7. Protocol relay và phòng multiplayer

Giữ endpoint WS `/game`, nâng protocol thành v2 rõ ràng. Client cũ nhận lỗi phiên bản không hỗ trợ, không âm thầm ACK gói không hiểu.

Envelope đề xuất: `version`, `type`, `roomId`, `runId`, `seq`, `payload`. Server gắn senderId từ connection, không tin senderId client truyền.

| Gói | Chiều/ý nghĩa |
|---|---|
| CONNECT | Server trả connectionId, playerId, protocolVersion |
| CREATE_ROOM / JOIN_ROOM / LEAVE_ROOM | Client yêu cầu thay đổi membership |
| ROOM_STATE / READY | Metadata host, thành viên, readiness, level, room status |
| START_MATCH | Chỉ host yêu cầu; server cấp runId và chốt roster/map metadata |
| MATCH_STARTED | Cùng runId/mapVersion/roster cho cả hai client |
| PLAYER_INPUT | Guest -> relay -> host; direction, moving, shoot, seq |
| WORLD_STATE | Chỉ host -> relay -> guests; tick, status, entity/map state |
| MATCH_RESULT | Chỉ host -> relay -> guests + persistence; gửi một lần logic |
| PAUSE_MATCH / RESUME_MATCH | Chỉ host; metadata pause, không tick server |
| ROOM_CLOSED / ERROR | Lý do đóng phòng/lỗi có mã rõ ràng |
| PING / PONG | Kiểm tra kết nối; không điều khiển simulation |

- Room lifecycle: LOBBY -> PLAYING -> FINISHED hoặc CLOSED. Rematch tạo runId mới; không reuse result key. RoomRegistry chỉ chứa metadata/connection, không chứa Tank/GameWorld.
- Client tải map/assets trước READY. Host chỉ start khi cả hai ready cùng mapVersion. Chọn vị trí spawn khác nhau ở host, không sinh tank ở server.
- Mỗi phòng độc lập. Cấm gửi input/snapshot/result sang phòng không phải thành viên; chỉ host được gửi WORLD_STATE/MATCH_RESULT.
- Simulation host 30Hz; snapshot khởi điểm 15Hz. Gửi snapshot đầy đủ khi start và khi terminal; snapshot định kỳ có tick tăng dần để guest bỏ state cũ.
- Snapshot chứa HP, hướng, vị trí, đạn, status, điểm, AI và map thay đổi nếu luật hiện có phá tường/vật phẩm. Guest không tự áp damage.
- Giới hạn cấu hình khởi điểm: tối đa 64 KiB/gói, 60 input/s/connection; kiểm tra map thực tế trước khi chốt. Sai giới hạn trả lỗi/throttle; không tạo queue vô hạn.
- Serialize gửi WS trên mỗi connection; slow consumer được giữ snapshot mới nhất, không dồn vô hạn state cũ. Gói kết quả/đóng phòng phải có đường gửi tin cậy riêng, không bị ghi đè bởi snapshot.
- Host bỏ input sai runId, seq cũ và input từ player không có trong roster. Timeout input ví dụ 500 ms => release; heartbeat 5 giây, timeout kết nối 15 giây, cấu hình tập trung.
- Thêm website lobby tối thiểu: tạo phòng, nhập mã tham gia, danh sách 2 người, ready/start, thông báo mất host. Không làm màn lobby đẹp phức tạp.
- Contract wire ghi ở docs riêng. `shared-protocol` chỉ dùng lại nếu tương thích GWT; không kéo Gson/reflection/JVM-only vào core. DTO hai phía có thể riêng nhưng phải có JSON fixtures chung và test contract.

## 8. Các bước triển khai — hoàn tất tất cả trước khi chạy test

### Bước A: Chốt contract và ranh giới

- Lập mapping lớp engine cũ -> gameplay mới; đánh dấu logic thực sự đang dùng.
- Viết GameSession/PlayerCommand/WorldSnapshot/MatchResult và API/WS contract.
- Chốt map schema, identity resolver và persistence receipt.
- Cập nhật test/fixtures trong quá trình code nhưng chưa chạy.

### Bước B: Port simulation vào core

- Port entity, collision, AI, map/interfaces cần thiết và config gameplay.
- Tạo GameSimulation với fixed-step driver bên session; tách mọi side effect network/DB.
- Chuyển công thức điểm và terminal logic vào client.
- Chuyển các test engine/entity/AI sang core, thay scheduler/Spring setup bằng step trực tiếp; thêm test dependency Gradle thích hợp.

### Bước C: Campaign local hoàn chỉnh

- Chuyển GameClient sang LocalCampaignSession mặc định; không connect socket ở campaign.
- Nối map/bootstrap, renderer, input, restart, next level và lifecycle.
- Sửa launcher HTML và desktop theo constructor mới; không để desktop không compile.
- Hiện terminal result ngay cả khi server lưu lỗi; xử lý loading/ready/error và focus.

### Bước D: Lưu kết quả và progression

- Thêm identity resolver, run metadata, REST endpoint, DTO, transaction/idempotency và migration.
- Đổi MatchEndService thành result persistence hoặc thay lớp có tên phù hợp; bỏ dependency MatchEndObserver.
- Nối BrowserResultGateway, retry, trạng thái lưu và refresh campaign/leaderboard.
- Chuyển/xóa route submit điểm cũ sau khi thay caller; cập nhật tests controller/service liên quan.

### Bước E: Host client và server relay

- Thêm RoomService/RoomRegistry và protocol v2; thay GameWebSocketHandler routing.
- Viết HostGameSession dùng GameSimulation; GuestGameSession nhận snapshot.
- Thêm lobby Thymeleaf, ready/start, co-op rules, pause/disconnect/result/rematch.
- Nối identity HTTP vào WS handshake; giới hạn gói, queue và tách phòng.

### Bước F: Tắt engine server, hoàn thiện tài liệu/build

- Gỡ EngineLifecycle, engine beans và mọi caller queueInput/broadcastState cũ.
- Di chuyển hoặc loại bỏ các test server kiểm tra simulation vì trách nhiệm đã sang core; giữ coverage logic trong core, không bỏ test chỉ để pass.
- Bỏ ScoreCalculator server, engine/entity/ai/interfaces chỉ sau khi reference đã chuyển hết. Model map phục vụ dữ liệu được giữ nhưng không chứa simulation.
- Cập nhật docs/CODEBASE-GUIDE.md, architecture, networking/runbook và contract v2; đánh dấu tài liệu server-authoritative cũ đã bị thay thế.
- Kiểm tra import/reference bằng rg và đọc diff. Chưa chạy test, chưa chạy game giữa các bước A-F.
- Chưa xóa hàng loạt thư mục legacy `client/`, `server/` hay `shared-protocol/`; liệt kê ứng viên cleanup riêng nếu chưa chứng minh không dùng. Không xóa dữ liệu PostgreSQL, migration cũ hoặc assets nguồn.

### Bước G: Đợt xác minh cuối và bàn giao

Chỉ bắt đầu khi A-F đã code xong, gồm cả test mới/cập nhật và tài liệu. Thực hiện mục 9-10. Không tuyên bố hoàn thành nếu mới chạy campaign nhưng host/relay còn TODO.

## 9. Ma trận nghiệm thu cho đợt cuối

| Nhóm | Kiểm tra bắt buộc |
|---|---|
| Simulation unit | Di chuyển theo dt, thả phím, tường, cooldown, đạn không gây damage hai lần, AI/pathfinding, spawn hợp lệ |
| Terminal | Victory/defeat một lần, cùng tick chết ưu tiên đúng, điểm cuối đúng, dừng input sau terminal |
| Lifecycle | Restart/next reset world và runId, focus/blur không kẹt phím, pause không nhảy thời gian |
| Campaign HTML5 | Cả 3 map tải được, di chuyển/bắn/AI/thắng/thua; loading biến mất; chữ UI đúng chiều |
| Client ownership | Sau khi tải campaign, chặn WS vẫn chơi; ngắt mạng vẫn mô phỏng/kết thúc, lưu báo lỗi đúng |
| Isolation | Hai tab campaign không reset hoặc ghi đè world của nhau |
| Result API | Idempotency retry và request song song, payload khác cùng key => 409, DB rollback, sai owner/level/status => lỗi |
| Progress | Trang campaign và result API cùng identity, thắng unlock đúng, defeat không unlock, reload trang thấy dữ liệu đã lưu |
| Relay | Hai client cùng trận, host nhận input guest, guest nhận snapshot, HP/HUD đúng người, kết quả giống nhau |
| Quyền relay | Guest giả WORLD_STATE/MATCH_RESULT bị từ chối, sai room/run bị chặn, JSON lỗi không làm rớt handler |
| Disconnect | Mất host/guest được thông báo, không treo; không lưu trận bị hủy; rematch không lẫn snapshot cũ |
| Packaging | Bundle GWT mới nằm trong JAR, manifest assets đúng, không Invalid assets description file/404 |
| Backend | Khởi động không có game-loop thread/tick, endpoint và DB còn hoạt động |

Đối với co-op, dùng hai cửa sổ/profiles độc lập để có identity khác nhau. Hai tab cùng cookie dùng cho test campaign isolation, không dùng để giả hai người chơi khác nhau.

## 10. Quy trình “code hết rồi test lần cuối”

Yêu cầu này có nghĩa là một đợt xác minh tập trung sau triển khai, không phải chỉ được chạy đúng một lệnh hoặc bỏ qua lỗi của lần đầu.

Trong A-F được đọc file, tìm reference, sửa source/test/docs và rà diff. Không chạy test suite, GWT build, package hay mở game thử lặp lại sau từng bước. Viết test cùng code để đến cuối có thể chạy toàn bộ.

Cuối đợt, thực hiện theo thứ tự dưới đây từ PowerShell. Kiểm tra công cụ Maven thực tế trước khi chọn lệnh: repository đã thấy `mvnw`, chưa xác nhận `mvnw.cmd`; không dùng lệnh sai `..\mvnw\.cmd`. Ví dụ sau dùng `mvn` khi đã có trong PATH; nếu thiếu, dùng Maven đã cấu hình của IDE hoặc wrapper Windows hợp lệ, không tự giả định file tồn tại.

```powershell
Set-Location D:\BTLLapTrinhMang\LapTrinhMang\game-client
.\gradlew.bat core:test desktop:classes html:dist html:copyToServer

Set-Location D:\BTLLapTrinhMang\LapTrinhMang
mvn -f server-spring\pom.xml verify

# Chỉ cần khi shared-protocol được sử dụng hoặc thay đổi trong đợt này:
mvn -f shared-protocol\pom.xml test
```

Các lệnh ở trên là kế hoạch xác minh, chưa được chạy khi viết tài liệu. Chọn test profile theo cấu hình test thực tế; không tự ép profile production hoặc bỏ test. Thêm core test framework/task nếu hiện chưa có. Kiểm tra exit code sau từng lệnh; lỗi thì dừng pipeline phụ thuộc để sửa.

Sau build thành công, khởi động PostgreSQL theo infra/runbook hiện tại nếu cần rồi chạy JAR mới:

```powershell
java -jar .\server-spring\target\tankzors-server-spring-1.0-SNAPSHOT.jar
```

- Cổng mặc định hiện tại 8081. Nếu trùng cổng, xác định đúng PID/server cũ trước khi dừng; không kill tất cả java.exe.
- Chạy browser smoke một đợt theo ma trận mục 9, bao gồm hai trình duyệt multiplayer.
- Kiểm tra Console/Network, WS frames, kết quả REST và dữ liệu progression. Clear cache/reload để không dùng bundle cũ.
- Nếu phát hiện lỗi: sửa, chạy lại test phần bị ảnh hưởng và rebuild/retest HTML5 nếu sửa client. Không bỏ xác minh sau sửa chỉ để giữ nghĩa đen “test một lần”.
- Không chạy lại toàn bộ suite nhiều lần nếu không có thay đổi hoặc lỗi mới.
- Không được báo “đã test” khi chỉ đọc code. Nếu môi trường thiếu DB/browser/dependencies, ghi rõ kiểm tra nào chưa chạy và nguyên nhân.

## 11. Definition of Done và bàn giao

- [ ] Campaign và host đều chạy simulation trong client core.
- [ ] Backend không có scheduled game loop, AI, collision, damage hoặc tính điểm.
- [ ] Campaign chơi được sau bootstrap khi không có WS.
- [ ] Website/iframe có loading/error/ready/result/save status đúng.
- [ ] Lưu kết quả chống trùng, progression cùng HTTP identity, không mất dữ liệu DB cũ.
- [ ] Phòng co-op 2 người chạy qua relay với quyền host rõ ràng, disconnect được xử lý.
- [ ] Client cũ/protocol cũ bị từ chối rõ ràng; không còn hai đường ghi kết quả độc lập.
- [ ] Test gameplay đã chuyển sang core; test backend phản ánh trách nhiệm mới.
- [ ] Đợt xác minh cuối hoàn tất, ghi lệnh và kết quả thật.
- [ ] Tài liệu source/runbook/protocol được cập nhật; generated bundle được tạo bằng pipeline.

Bàn giao kèm danh sách file đổi, mapping logic đã chuyển, lệnh chạy, kết quả test, mục chưa kiểm tra và giới hạn đã chấp nhận. Điểm client-reported và host rời trận làm hủy phòng là giới hạn thiết kế, không được che giấu.

Không tự commit/push nếu chưa có yêu cầu. Có thể đề xuất nhóm commit: gameplay core; campaign integration; persistence; relay/co-op; cleanup/docs/generated build.

## 12. Prompt giao cho AI triển khai

> Đọc `docs/CLIENT-AUTHORITATIVE-IMPLEMENTATION-PLAN.md`, hướng dẫn repository và source hiện tại. Triển khai đầy đủ A-F: toàn bộ gameplay ở LibGDX client, campaign local, multiplayer host client qua Spring WebSocket relay, lưu kết quả và progression qua backend. Giữ stack và hành vi campaign hiện có; không thêm React. Bảo toàn thay đổi người dùng. Code cả test và docs trong quá trình triển khai nhưng không chạy test/build/game giữa các bước. Chỉ khi toàn bộ A-F xong mới chạy một đợt build, automated tests và browser verification theo mục 9-10. Nếu đợt cuối phát hiện lỗi, sửa và kiểm tra lại phần bị ảnh hưởng trước khi bàn giao. Không coi chỉ chạy desktop hoặc chỉ chạy campaign là hoàn tất. Báo chính xác những gì đã kiểm tra, không tự commit/push.
