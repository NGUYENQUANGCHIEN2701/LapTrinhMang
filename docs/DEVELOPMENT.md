# Hướng dẫn phát triển Tankzors

Cách chạy nằm ở [README gốc](../README.md), hợp đồng mạng ở [networking](networking.md), các sửa lỗi và giới hạn xác minh ở [CODE-REVIEW](CODE-REVIEW.md). Toolchain chuẩn: Docker build client bằng Gradle 8.10.2/JDK 17, server bằng Maven/JDK 21, runtime PostgreSQL 16.

## 1. Bắt đầu một thay đổi

1. Build/chạy Docker, tạo tài khoản thử và chơi một màn.
2. Đọc [kiến trúc](architecture.md) rồi chọn luồng trong [flows](flows/README.md).
3. Ghi tình huống đầu vào, hành động và kết quả mong muốn trước khi sửa.
4. Sửa tại lớp sở hữu trách nhiệm, thêm test hồi quy, kiểm tra browser/GWT và Docker nếu chạm ranh giới module.

Desktop giúp debug mô phỏng; H2 giúp kiểm tra persistence/controller. Chúng chưa thay thế browser, GWT hoặc PostgreSQL.

## 2. Chọn nơi sửa

| Yêu cầu | Điểm bắt đầu | Kiểm tra |
|---|---|---|
| Đạn, sát thương, nhiệm vụ, thắng/thua | Core `gameplay/GameSimulation`, `entity`, `collision`, `SpecialAmmoRules` | Map nhỏ/seed cố định, JUnit core, desktop, HTML5 |
| AI, spawn, wave | Core `gameplay/ai`, `MissionSpawner`, `gameplay/map` | Vật cản, đường đi và số mục tiêu |
| Input, pause, retry/next | `ClientInputProcessor`, `LocalCampaignSession`, `GameClient` | Async callback, lỗi bootstrap/lưu, thao tác trong iframe |
| HUD, radar, hiệu ứng | `model`, `WorldSnapshotMapper`, `render`, `ui` | Snapshot và kiểm tra nhiều kích thước canvas |
| Browser I/O | `HtmlLauncher`, `GwtAppBridge`, `CampaignPersistence` | Thứ tự request, timeout, retry, conflict, reload |
| Kinh tế tài khoản | `PlayerShopState` journal; Spring `PlayerShopService`, `EconomyCatalog`, `PlayerStateLock` | Giá/giới hạn, thiếu tiền, replay, stale revision và đồng thời |
| API, tiến trình, kết quả | Spring `web`, `application`, `result` | MockMvc, H2, PostgreSQL và transaction |
| Security | `security`, `identity`, `AccountStatusFilter` | Login thật, session cũ sau khóa/xóa, CSRF, socket |
| Co-op | `RoomRelayHandler`, `room`, host/guest session | Contract packet, hai browser, rời phòng, sai quyền |

Client package nằm dưới `game-client/core/src/main/java/com/tankzors/client`; server package dưới `server-spring/src/main/java/com/tankzors`. [CODEBASE-GUIDE](CODEBASE-GUIDE.md) giúp tra cứu thêm.

## 3. Thêm màn hoặc nhiệm vụ

### Map dùng schema hiện tại

1. Tạo `game-client/assets/maps/campaign_N.json` dựa trên kiểu nhiệm vụ tương tự. Kiểm tra width/height, grid/visualGrid, spawn, đường đi, nhiệm vụ, wave/allies, boxes và rewards.
2. Đồng bộ sang `server-spring/src/main/resources/maps`. Desktop đọc assets; browser lấy từ server. Hai bản phải giống nhau.
3. Cập nhật `PlayerProgressService.CAMPAIGN_ORDER`, `StaticCampaignQueryService` và `LocalCampaignSession.nextCampaignLevel`. Tìm thêm `campaign_3` để kiểm tra các giả định ba màn ở UI/test.
4. Test loader hai phía, bootstrap, quyền truy cập, unlock sau chiến thắng và điểm dừng ở màn cuối; thử desktop/browser.

Chỉ thêm JSON chưa đủ để Continue hoặc chuyển màn biết màn mới. Hướng dài hạn là một catalog campaign chung được kiểm tra trong build.

### Đổi schema hoặc kiểu nhiệm vụ

Đổi DTO client `MapDefinition`/`MissionConfig`, DTO Spring `GameMap`, loader và phần trả map trong bootstrap. Luật thực thi nằm ở core, thường tại simulation/spawner; Spring chỉ xử lý metadata. Chốt default cho map cũ, cập nhật hai bản JSON, map version và test thiếu trường/sai kích thước. Bootstrap hiện dùng version `v1`; khi thay hợp đồng phải cập nhật và kiểm tra version cả hai phía.

Pickup tài khoản đi qua operation `PICKUP(col,row)`; server đối chiếu ô map/box, mỗi ô một lần trong lượt. Khi thêm loại pickup bền vững, cập nhật diễn giải metadata ở `PlayerShopService` cùng hành vi core; không thêm va chạm hoặc world vào Spring.

## 4. Thêm vật phẩm hoặc nghiên cứu

1. Khai báo `ShopItem` hoặc `ResearchTech`, không tái sử dụng ID đã lưu với ý nghĩa khác.
2. Cập nhật catalog giá/pack/limit/tech ở `game-client/assets/economy/catalog.json` và bản tương ứng trong Spring resources. Docker chép catalog nguồn client vào server; core test kiểm tra khớp enum.
3. Nếu có công thức giá/giới hạn mới, cập nhật phần dự đoán trong client và `EconomyCatalog` trên server; test cả hai. Client không được quyết định giá ghi vào DB.
4. Thêm luật sử dụng trong simulation, inventory/input, mapper/snapshot và render tương ứng. Kiểm tra các vòng lặp dải ID trong session.
5. Ghi đúng operation: mua/bán/nghiên cứu trong `PlayerShopState`, tiêu hao khi thay kho, pickup tại ô map, chuyển khiên bằng `INSTALL`. Không gửi số dư hoặc JSON kho để ghi đè.
6. Test thiếu tiền, chưa mở tech, giới hạn, partial pack, refund trong lần mở shop, tiêu hao, chết/hồi sinh, restart và tải lại. Cập nhật [hướng dẫn vật phẩm](CLIENT-SHOP-ITEMS-GUIDE.md).

Khi browser có persistence, thưởng chỉ được cộng sau xác nhận của server; desktop local có thể tính thưởng trong bộ nhớ. Không cộng thêm thưởng ở cả hai nơi. Kho browser dùng state đã lưu qua retry; nếu hết toàn bộ đạn và không đủ 5 Xu, server có chính sách cấp lại đạn thường khi bắt đầu lượt mới.

## 5. API, transaction và co-op

### API và dữ liệu

- Resolve danh tính từ session, không từ body. DTO request/response tách entity và validate theo schema DB.
- Bootstrap cấp `game_run`: owner, level/version và hạn. Submit kiểm tra metadata và owner trước nhánh retry; score/thắng-thua vẫn client-reported.
- Mọi use case thay đổi ví/kho/progress phải acquire `PlayerStateLock` trong cùng transaction trước khi đọc và ghi. Khóa DB được giữ đến commit; `synchronized` ở service không thay thế được.
- Giao dịch cần request ID, hash và expected revision. Cùng ID/body là retry, khác body/revision cũ là conflict. Không dùng transaction đã rollback-only sau exception.
- Gửi mọi ghi browser qua cùng outbox; giữ nguyên body khi retry, cập nhật revision từ response/bootstrap. Không cho request mới vượt request đang retry.
- Không tự rebase thao tác bị từ chối. 400/409 hiển thị lỗi và nạp lại state server; 401 yêu cầu đăng nhập. Pending trong cùng tab có thể khôi phục từ sessionStorage, world thì không.
- Migration mới dùng version kế tiếp, hiện là V9 sau V8; không sửa SQL đã áp dụng. Xem [database README](../server-spring/src/main/resources/db/README.md).
- Khi đổi CSRF, cập nhật token cho iframe/bridge và test cùng lúc. Không tắt CSRF toàn app để tránh lỗi test.

### Lộ trình co-op tối thiểu

1. Contract `MATCH_STARTED` hiện có members/host/level/version/run ID; giữ test relay và host tương thích khi thay đổi.
2. Thiết kế chuyển lobby sang game mà không mất định danh/connection. Nếu cần rejoin, định nghĩa quy trình thay vì dựa vào bộ nhớ của trang đã rời.
3. Nối `HtmlLauncher` chọn host/guest từ dữ liệu phòng. Host chạy đủ player; guest chỉ gửi input và render snapshot.
4. Hoàn thiện kiểm tra phase/run ID/seq, packet cũ/lặp, giới hạn lưu lượng, timeout, disconnect/reconnect và rematch. Bảo đảm snapshot đủ cho render.
5. Chốt ai lưu kết quả, lưu cho ai, thưởng và leaderboard co-op có tách campaign không. REST result hiện chỉ nhận campaign; relay không tự lưu `MATCH_RESULT`.
6. Test hai browser, host/guest rời, sai quyền và mạng chậm. Không dùng `/game` hoặc packet v1.

## 6. Kiểm tra và bàn giao

Các lệnh chạy từ root trừ block chuyển thư mục. Dockerfile không chạy unit test.

```powershell
# JDK 21; H2, không cần PostgreSQL
mvn -f server-spring/pom.xml test

# JDK 17
Set-Location game-client
.\gradlew.bat core:test html:copyToServer
Set-Location ..

# Pipeline tích hợp chuẩn
docker compose -f infra/docker-compose.yml build app
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
curl.exe http://localhost:8081/api/health
```

Core test không thực thi JSNI. Các Spring test profile `test` dùng security permit-all; test auth phải dùng cấu hình thật như `LoginFailureMessageTest`. H2 không đảm bảo SQL/lock đồng thời giống PostgreSQL. Browser regression bổ sung nằm trong [hướng dẫn HTML5](../game-client/html/README.md#kiểm-thử-browser-tùy-chọn).

Checklist PR:

- Có tình huống trước/sau và test tương tác thực tế liên quan, không chỉ getter/setter.
- Lượt, ví, kho, progress và reward vẫn nhất quán khi retry, đồng thời, request cũ, mất mạng hoặc hết phiên.
- Map/catalog đồng bộ; migration áp dụng được cho database mới và database ở version trước.
- Gameplay/render đã thử desktop và browser; input, focus, resize và kết thúc trận phù hợp.
- Test đạt, GWT build đạt và kiểm tra Docker/PostgreSQL hoàn tất; nếu thiếu môi trường, ghi rõ phần chưa xác minh.
- README module, flow/protocol và các giới hạn được cập nhật; không sửa generated JavaScript bằng tay.

## 7. Kiểm tra nên mở rộng tiếp

Các test hồi quy hiện có bao phủ owner/run giả, version/expiry, credit một lần, metadata retry, stale shop revision, duplicate/concurrent request, lỗi độ dài, phiên cũ sau khóa/xóa và contract host/relay. Các phần tiếp theo nên bổ sung:

- PostgreSQL thật: upgrade từ V7 và lock/concurrency giữa hai transaction/instance.
- Browser: storage bị cấm/hết quota, session hết hạn khi outbox đang chờ, hai tab cùng tài khoản và request nhiều batch.
- Co-op: packet sai thứ tự/run, disconnect/reconnect và policy lưu kết quả khi hoàn thiện launcher.
- Triển khai: CI Docker, backup/restore và quan sát lỗi lưu; chính sách dọn game_run/shop_request theo hạn lưu trữ.
