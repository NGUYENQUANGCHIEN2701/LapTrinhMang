# Khắc phục các vấn đề đã rà soát

Đợt rà soát ban đầu dựa trên commit `4c45094`, ngày 09/11/2026. Tài liệu này ghi trạng thái sau thay đổi code, thay cho danh sách lỗi còn mở trước đó. Các sửa đổi giữ campaign client-authoritative và không thêm simulation vào Spring.

## Những thay đổi đã triển khai

| Mã | Vấn đề ban đầu | Cách khắc phục | Test chính |
|---|---|---|---|
| R1 | Client tự đặt ví/kho/nghiên cứu | Bỏ ghi snapshot; server xử lý journal theo catalog, kiểm tra giá, kho, nghiên cứu, tiền; thưởng do server tính | `PlayerShopServiceTest`, `GameApiControllerTest`, `EconomyJournalTest` |
| R2 | Run ID tự đặt/sai owner vẫn được nhận | Lưu lượt được cấp với owner, level, version, hạn; kiểm tra cả trước nhánh retry | `ResultServiceTest`: invented run, owner, version/level, expiry |
| R3 | Chuyển màn chưa chờ lưu; thưởng và kết quả tách rời | Callback async, trạng thái lưu/lỗi, chặn chuyển màn; thưởng/progress/leaderboard/result cùng transaction; outbox khôi phục qua reload | `LocalCampaignSessionTest`, `ResultServiceTest`; kiểm tra browser |
| R4 | Snapshot cũ/retry ghi đè dữ liệu mới | Gửi thao tác có request ID/revision; khóa account; browser queue tuần tự | `PlayerShopServiceTest`: stale revision, duplicate, concurrent purchase |
| R5 | `409` bị coi là thành công | Chỉ response 2xx đúng contract xác nhận lưu; 400/409 yêu cầu nạp lại state, không tự retry | Kiểm tra browser conflict; API conflict tests |
| R6 | Retry trả ví 0 và unlock rỗng | Response lấy số dư, best score, unlocked levels và revision hiện tại | `ResultServiceTest.retryReturnsCurrentMetadataAndNeverCreditsTwice` |
| R7 | Đua check/insert gây lỗi unique | Khóa DB theo account đến commit trước khi đọc/ghi kết quả; issued run ràng buộc owner | `ResultServiceTest.concurrentSameRunReturnsSavedAndIdempotentWithoutUniqueFailure` |
| R8 | Host không nhận danh sách guest khi start | Relay gửi `members`/host; host lấy run ID, level/version từ packet; chặn start lặp ngoài lobby | `RoomRelayHandlerTest`, `HostGameSessionTest` |
| R9 | Input quá dài/enum sai đi tới DB | Validate chiều dài, mode/status, số lượng; email/display name/password; bắt đăng ký trùng do race | `RegisterTest`, `ResultServiceTest` |
| R10 | Khóa/xóa user chưa thu hồi phiên cũ | Principal giữ ID DB; filter kiểm tra user mỗi request, invalidate session; admin đóng socket | `LoginFailureMessageTest`, `RoomRelayHandlerTest` |

Bổ sung liên quan: load shop state thay đúng cả số dư 0 và đồ bị xóa; GWT sync asset trước compile; sinh hash kết quả bằng SHA-256; WebSocket dùng origin mặc định cùng site. Bootstrap có chính sách cấp đạn thường khi kho hoàn toàn hết đạn và ví không đủ mua, để việc lưu tiêu hao không gây kẹt campaign.

## Dữ liệu và nâng cấp

Migration mới [V8](../server-spring/src/main/resources/db/migration/V8__issued_runs_and_shop_transactions.sql) thêm `game_run`, `shop_request` và `player_totals.revision`. Không sửa V1–V7. Túi đồ rỗng dạng `{}` từ tài khoản chưa khởi tạo được chuyển sang trang bị ban đầu. Không tự thu hồi số dư/vật phẩm hợp lệ đã có trong database cũ.

API shop đã đổi hợp đồng: client cũ gửi `coins/stars/researchJson/inventoryJson` bị từ chối. Phải phát hành bundle HTML5 và server cùng phiên bản bằng Docker multi-stage hoặc build/copy GWT rồi đóng gói Spring; hard reload browser sau nâng cấp. Xem [networking](networking.md) để biết request/response hiện hành.

## Kiểm tra và giới hạn xác minh

Kết quả cuối đợt sửa ngày 09/11/2026:

| Kiểm tra | Kết quả |
|---|---|
| Maven `package`, JDK 21, H2/Flyway V1–V8 | **113 test đạt, 0 bỏ qua**, JAR đóng gói thành công |
| Gradle `core:test`, JDK 17 | **199 test đạt** |
| Gradle `html:copyToServer`, JDK 17 | Build GWT và đồng bộ bundle thành công |
| Chrome headless với JAR đã đóng gói và H2 thử riêng | **3 kịch bản đạt**: response chậm/mất sau commit, conflict 409, reload outbox; có mua Nitro qua UI và chờ batch shop trước chuyển màn |
| Map/campaign và catalog kinh tế hai phía | Các bản nguồn tương ứng có SHA-256 giống nhau |
| Tài liệu | Kiểm tra 24 file Markdown, 216 liên kết nội bộ, không có link/anchor hỏng |

Các lệnh kiểm thử chuẩn ở [DEVELOPMENT](DEVELOPMENT.md#6-kiểm-tra-và-bàn-giao), test browser ở [HTML5 README](../game-client/html/README.md#kiểm-thử-browser-tùy-chọn). Browser test thay map bootstrap bằng fixture nhỏ để điều khiển thời điểm kết thúc; login, cấp lượt, bridge và ghi DB chạy thật. Nó không thay thế chơi đủ ba màn thật.

Test core chạy JVM; build GWT kiểm tra transpilation bằng JDK 17. Test Spring dùng H2 và có auth test dùng `SecurityConfig` thật. Test asset trước đây bị bỏ qua nay đã chạy đạt sau khi build bundle.

Số test server lấy từ summary của lần Maven hiện tại. Thư mục report trên máy phát triển có thể còn XML từ phiên bản cũ; không cộng các file lịch sử đó vào kết quả mới.

Môi trường rà soát có Java/Maven và WSL, nhưng chưa có Docker CLI/daemon hoạt động. Không coi H2, GWT hoặc browser local là xác nhận pipeline Docker/PostgreSQL. Docker build/start và kiểm tra migration trên PostgreSQL vẫn là cổng kiểm tra tích hợp trước khi triển khai.

## Giới hạn còn lại

- Điểm, thắng/thua, tiêu hao và vị trí pickup vẫn do client báo. Server kiểm tra dữ liệu tài khoản/metadata, không replay trận; chưa có chống gian lận gameplay hoàn chỉnh.
- Outbox phụ thuộc `sessionStorage` của tab. Reload có thể khôi phục request; đóng hẳn tab/xóa storage hoặc trình duyệt cấm storage có thể làm mất request chưa gửi. World không được lưu.
- Khi server từ chối 400/409, request được giữ riêng để chẩn đoán và UI yêu cầu nạp lại. Các thao tác bị từ chối không được tuyên bố đã lưu hoặc tự áp lại lên state mới.
- Co-op mới sửa contract nền tảng, chưa nối launcher thành chế độ chơi hoàn chỉnh; cần phase/run/sequence đầy đủ, snapshot và policy persistence.
- Account lock giúp tuần tự hóa mutation do các use case hiện tại thực hiện. API mới phải dùng cùng khóa/transaction, không gọi repository ghi ví riêng.
- Lượt đã hết hạn và lịch sử giao dịch cần chính sách lưu trữ/dọn dẹp khi số người chơi tăng. Chưa có CI Docker/PostgreSQL hoặc hệ thống telemetry riêng.

Hướng mở rộng và tiêu chí bàn giao ở [DEVELOPMENT](DEVELOPMENT.md) và [README](../README.md#12-hướng-phát-triển-và-bàn-giao).
