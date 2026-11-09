# HTML5/GWT: tích hợp game với website

Module biên dịch Java core thành JavaScript và chạy game trong iframe do Spring phục vụ tại `/game/libgdx/index.html`. Website, game và REST dùng cùng origin/cookie phiên.

## Source quan trọng

| File | Trách nhiệm |
|---|---|
| `src/main/java/com/tankzors/client/html/HtmlLauncher.java` | Đọc query params, tạo session campaign, tải bootstrap |
| `GwtAppBridge.java` trong cùng package | Outbox có thứ tự, journal shop/result, revision, timeout và retry qua XMLHttpRequest/JSNI |
| `BrowserWebSocket.java` trong cùng package | Transport WebSocket cho nền tảng relay |
| `TankzorsHtml.gwt.xml` trong cùng package | Entry point và cấu hình GWT/asset |
| `src/main/webapp/index.html` | Trang host canvas của game |
| `build.gradle` | GWT compile, sync assets, dist và copy sang Spring |

Tham số launcher đang đọc: `levelId` (mặc định `campaign_1`), `mode` (mặc định `campaign`), `wsUrl`, `soundVol`, `musicVol`. Âm lượng từ 0–100. `mode=coop` chưa được nối thành host/guest flow có thể chơi; campaign không mở WebSocket.

## Luồng mạng hiện tại

1. Bridge đọc `/api/game/account`, khôi phục outbox của đúng tài khoản trong sessionStorage, gửi hết ghi đang chờ rồi mới bootstrap.
2. Callback chuyển map JSON, `runId`, version và shop state vào `LocalCampaignSession`.
3. Gameplay chạy trong browser. Mua/bán/nghiên cứu, tiêu hao và pickup được ghi thành journal; server kiểm tra từng thao tác trong POST `/api/game/shop/sync`.
4. Kết quả được xếp sau các giao dịch đang chờ. Callback trả xác nhận hoặc lỗi cho session; game hiện trạng thái lưu và chặn chuyển màn/rời kết quả khi chưa xác nhận.

Danh tính lưu dữ liệu do Spring resolve từ session HTTP, không dùng chuỗi `browserPlayerId` của launcher làm khóa tài khoản. Cách sửa chờ xác nhận, xử lý `409` và thứ tự request nằm trong [CODE-REVIEW](../../docs/CODE-REVIEW.md).

## Build

Từ `game-client/`, dùng JDK 17 hoặc Docker workflow ở root:

```powershell
.\gradlew.bat html:dist
# Hoặc build rồi đồng bộ bundle vào Spring để chạy server local
.\gradlew.bat html:copyToServer
```

Output là `html/build/dist/`. Task `syncAssets` lấy nguồn từ `game-client/assets`; `src/main/webapp/assets` là bản đồng bộ phục vụ build. Sửa asset ở nguồn chung, không ở bản copy. GWT tạo manifest và asset hash; giữ pipeline build tạo các file này.

Sau copy, cần đóng gói/chạy lại Spring nếu đang chạy từ JAR, rồi tải lại browser để lấy bundle mới. Nếu dùng Docker, `up --build -d` tự build toàn bộ client và đóng gói server.

## Kiểm tra khi sửa browser bridge

- Network: bootstrap `200`, đúng level/run, POST kết quả và shop trả JSON.
- Session hết hạn phải hiện trạng thái cần đăng nhập; tránh coi trang HTML login là thành công.
- Thử mạng chậm, offline, retry, conflict `409` và chuyển màn ngay sau chiến thắng.
- Kiểm tra input khi focus iframe, resize/fullscreen, WebGL và âm thanh sau tương tác người dùng.
- Không sửa JavaScript sinh ra trong Spring. JVM core test không chạy được JSNI; cần build GWT và kiểm tra browser.

## Kiểm thử browser tùy chọn

Có test tại `src/test/browser/persistence-smoke.cjs`. Dùng database thử riêng: test tự tạo ba tài khoản QA và lưu kết quả của chúng. Test thay map bootstrap bằng fixture nhỏ để kết thúc nhanh; login, cấp lượt, GWT bridge và persistence vẫn chạy thật. Nó không kiểm tra đầy đủ nội dung ba màn thật.

Cần Node.js, Chrome và một server đang phục vụ bundle mới (mặc định `http://127.0.0.1:18081`). Chạy từ root:

```powershell
npm install --prefix game-client/.gradle/browser-check --no-audit --no-fund playwright@1.61.1
$env:TANKZORS_TEST_URL = "http://127.0.0.1:18081"
node game-client/html/src/test/browser/persistence-smoke.cjs
```

Có thể đặt `BROWSER_EXECUTABLE` tới Chrome/Edge riêng. Kết quả JSON và screenshot nằm trong `html/build/browser-smoke`, không commit output. Ba kịch bản: kết quả trả chậm rồi mất phản hồi đã commit (retry idempotent, Next chờ, mua đồ/chuyển màn đúng); 409 không được coi là thành công; reload khôi phục outbox. Chạy `html:copyToServer` và khởi động lại server trước khi test local bằng bundle mới.

Outbox dùng storage của tab: reload có thể khôi phục ghi đang chờ, nhưng không khôi phục world và không bảo đảm tồn tại sau đóng tab/xóa/cấm storage. Các batch 400/409 được giữ riêng để chẩn đoán; nhấn R nạp lại dữ liệu server, không áp lại thao tác bị từ chối.
