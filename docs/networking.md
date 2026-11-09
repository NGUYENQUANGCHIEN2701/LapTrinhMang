# REST campaign và relay co-op

Campaign mô phỏng trong client; Spring xác thực tài khoản, kiểm tra giao dịch, lưu dữ liệu và relay. Mọi API dưới đây cần phiên đăng nhập `JSESSIONID`; `/api/health` là ngoại lệ công khai. API hết phiên trả `401`, không redirect sang HTML login.

## Bootstrap và khôi phục ghi đang chờ

Browser gọi `GET /api/game/account` để lấy `playerId`, số dư và `revision`, khôi phục hàng đợi trong `sessionStorage` của đúng tài khoản, rồi mới gọi:

```http
GET /api/game/bootstrap?levelId=campaign_1
```

Response gồm `protocolVersion`, `playerId`, `levelId`, `nextLevelId`, `mapVersion`, `map`, `settings`, `unlockedLevels`, `runId`, `totalCoins`, `totalStars`, `researchJson`, `inventoryJson`, `revision`. Hai trường JSON của shop là chuỗi. Bootstrap lưu `game_run` gồm owner, level, map version và hạn dùng 24 giờ. Màn chưa mở/đã thắng trả `403`. Tham số `mode` cũ chưa chọn chế độ bootstrap; endpoint này phục vụ campaign.

Nếu không còn loại đạn nào và ví dưới 5 Xu, bootstrap cấp lại 99 đạn thường. Đây là cấp phát được server quyết định để tránh kẹt tài khoản; không nhận số lượng cấp phát từ client.

## Giao dịch cửa hàng và thay đổi kho

```http
POST /api/game/shop/sync
Content-Type: application/json
```

```json
{
  "runId": "<runId đã được cấp>",
  "requestId": "<UUID mới cho batch>",
  "expectedRevision": 0,
  "operations": [
    {"type": "OPEN"},
    {"type": "BUY", "itemId": 17}
  ]
}
```

Endpoint giữ tên `/shop/sync` nhưng không còn nhận snapshot số dư/nghiên cứu/inventory. Body kiểu cũ bị từ chối. Các thao tác:

| Type | Dữ liệu | Server xử lý |
|---|---|---|
| `OPEN` | Không có | Ghi mốc kho lúc mở shop để giới hạn bán lại |
| `BUY`, `SELL` | `itemId` | Kiểm tra catalog, nghiên cứu, kho, giá, tiền và quyền bán lại |
| `RESEARCH` | `techId` | Tăng một cấp theo giá Sao trong catalog |
| `CONSUME` | `itemId`, `amount` | Chỉ giảm số lượng đang sở hữu; cấm tiêu hao trang bị vĩnh viễn |
| `INSTALL` | `itemId=20`, `amount` | Chuyển khiên từ kho sang trạng thái đã lắp, kiểm tra dung lượng |
| `REPAIR`, `REPAIR_ALLIES` | `amount` là HP thiếu | Tính phí sửa theo nghiên cứu đã lưu |
| `PICKUP` | `col`, `row` | Đối chiếu ô map/box metadata; mỗi ô chỉ ghi nhận một lần trong lượt |

Một batch có 1–1000 thao tác và chạy trong một transaction. Server khóa row tài khoản trước khi kiểm tra revision và áp dụng. `requestId` được ràng buộc tài khoản; cùng ID/body trả dữ liệu hiện tại mà không áp dụng lại. Cùng ID/body khác hoặc revision cũ trả `409`. Response `200` gồm `status=OK`, `totalCoins`, `totalStars`, `researchJson`, `inventoryJson`, `revision`.

Client có thể dự đoán giá/kho để giao diện phản hồi ngay, nhưng server là nơi quyết định ghi dữ liệu. Catalog nguồn ở `game-client/assets/economy/catalog.json`; bản Spring được đồng bộ trong Docker build. Core test kiểm tra catalog khớp enum client.

## Kết quả trận

```http
POST /api/game/results
Content-Type: application/json
```

Body chứa `runId`, `mode=CAMPAIGN`, `levelId`, `mapVersion`, `status`, `score`, `hpRemain`, `aiKills`, `elapsedSeconds`, `starsEarned`, `coinsEarned`. Điểm/thống kê vẫn do client báo trong giới hạn; hai trường thưởng không quyết định ví. Lưu kết quả co-op chưa được mở qua endpoint này.

Server kiểm tra lượt được cấp, owner và level/version. Lượt mới hết hạn bị từ chối. Trong cùng transaction và khóa tài khoản: lưu kết quả, tính thưởng từ map cộng nghiên cứu Extra Interest đã lưu, cộng ví, khóa màn thắng/mở màn kế và cập nhật best score. DEFEAT không có thưởng. Retry kết quả đã lưu vẫn được chấp nhận sau hạn lượt nếu cùng owner/payload.

| Response | Ý nghĩa |
|---|---|
| `200`, `saved=true`, `alreadySaved=false` | Đã lưu mới |
| `200`, `saved=true`, `alreadySaved=true` | Cùng lượt, owner và hash SHA-256 của payload; không ghi/cộng thưởng lại |
| `400` | Dữ liệu sai, lượt chưa cấp/hết hạn, sai level/version hoặc màn không được thắng |
| `401` | Phiên đăng nhập không còn hợp lệ |
| `409` | Sai owner hoặc cùng lượt nhưng payload khác |

Response thành công luôn có score/best score, thưởng thực ghi, unlocked levels và số dư/revision hiện tại, kể cả retry. Browser chờ xác nhận trước khi chuyển màn hoặc rời bảng kết quả.

## Thứ tự, retry và lỗi

`GwtAppBridge` dùng một hàng đợi chung cho shop và result. Request sau không vượt request đang retry. Mỗi request có timeout 15 giây; lỗi mạng, `429` và `5xx` được thử lại tối đa 3 lần với thời gian chờ tăng dần. Gửi lại cùng body/request ID, không tạo thao tác mới.

Hàng đợi được ghi trong `sessionStorage` theo tài khoản; reload cùng tab sẽ gửi lại trước bootstrap nếu storage còn và dùng được. Không khôi phục world đang chơi. `400/409` không được coi là thành công và không tự retry: batch bị từ chối cùng các request phụ thuộc được giữ riêng tại khóa `tankzors_outbox:rejected:<playerId>` để chẩn đoán; game yêu cầu nhấn R để tải lại dữ liệu server. `401` cần đăng nhập lại. Không tự tăng revision rồi áp lại dữ liệu cũ.

## Security và giới hạn authority

`/api/game/**` hiện bỏ qua CSRF vì bridge JSON cùng origin; form web và `/api/progress/reset` vẫn cần token. Khi đổi policy phải cập nhật bridge/test cùng lúc. Mỗi request của phiên đăng nhập thật kiểm tra lại tài khoản theo ID DB: khóa/xóa tài khoản làm cookie cũ bị từ chối; admin cũng đóng socket tương ứng.

Server không chạy simulation nên không chứng minh được xe thực sự nhặt pickup, lượng tiêu hao có thật hay chiến thắng là thật. Validation giao dịch bảo vệ tiền, giá, kho và vòng đời lượt trong phạm vi mô hình hiện tại; không thay thế chống gian lận gameplay.

## WebSocket co-op

Endpoint duy nhất là `ws://localhost:8081/coop` (hoặc `wss` khi có HTTPS), dùng [protocol v2](protocol-v2.md), mặc định cùng origin. `/game/libgdx/**` chỉ là tài nguyên tĩnh.

Relay kiểm tra thành viên/quyền host, gắn `senderId`, gửi input guest → host và snapshot/result host → guest. `MATCH_STARTED` có members/host/level/version/run ID; host dùng dữ liệu này tạo đủ player. Relay chưa tự lưu kết quả co-op. Phòng và kết nối nằm trong RAM, mất khi app restart. Launcher browser chưa nối đầy đủ chế độ này; xem [hướng phát triển](DEVELOPMENT.md#lộ-trình-co-op-tối-thiểu).
