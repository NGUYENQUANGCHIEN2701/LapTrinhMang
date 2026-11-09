# 06. Cửa hàng, kho và giao dịch tài khoản

Client dự đoán mua/bán để phản hồi nhanh; server kiểm tra trước khi ghi ví/kho/nghiên cứu. Không còn ghi đè snapshot theo số client gửi.

## Hai PlayerShopState

- [Client](../../game-client/core/src/main/java/com/tankzors/client/shop/PlayerShopState.java): state hiển thị và dự đoán, catalog enum, journal thao tác.
- [Server entity](../../server-spring/src/main/java/com/tankzors/progress/PlayerShopState.java): research/inventory JSON đã được service kiểm tra và lưu.
- [PlayerShopService](../../server-spring/src/main/java/com/tankzors/application/PlayerShopService.java): kiểm tra và áp dụng giao dịch. [EconomyCatalog](../../server-spring/src/main/java/com/tankzors/application/EconomyCatalog.java) chỉ tính metadata kinh tế, không chạy world/gameplay.

## Luồng

```text
B tại căn cứ hoặc sau chiến thắng đã lưu → ShopOverlay.show
  beginShopVisit → journal OPEN, tạm dừng simulation
Mua/bán/nghiên cứu → PlayerShopState dự đoán và ghi operation
  callback session → drainOperations → CampaignPersistence.sync
Dùng đồ/bắn/nhặt đồ → simulation thay inventory và ghi operation
  session flush định kỳ, hoặc ngay khi kết thúc/rời shop
GwtAppBridge:
  xếp batch có requestId, chuẩn bị expectedRevision lúc gửi
  lưu outbox, gửi tuần tự, retry cùng body
Spring:
  lock account → kiểm tra owner/hạn run
  request đã có cùng hash → trả state hiện tại
  request mới: kiểm tra revision → áp dụng cả batch hoặc rollback
  lưu JSON/kho/ví, tăng revision, ghi shop_request → trả state
```

## Các quy tắc được server kiểm tra

- Giá, pack size, giới hạn và tech lấy từ catalog; công thức giá/giới hạn động đối chiếu nghiên cứu và trang bị đã lưu.
- BUY trừ Xu/cộng hàng; partial pack có công thức refund giống client. SELL chỉ hoàn lại hàng mua trong lần mở shop và những loại được phép bán.
- RESEARCH tăng một cấp, trừ Sao đúng giá. Sửa xe trừ tiền theo HP thiếu được báo và research.
- CONSUME chỉ giảm kho hợp lệ; INSTALL chuyển khiên sang trạng thái đã lắp theo dung lượng.
- PICKUP đối chiếu tọa độ ô map và metadata box, không nhận loại/giá trị thưởng tùy chọn. Mỗi ô một lần trong lượt.
- Request cũ khác revision trả 409, không ghi đè state. Cùng request ID/body là retry, không áp dụng hai lần.

Server không biết xe có thực sự ở tọa độ pickup hay HP báo thiếu có đúng không; đó vẫn là phạm vi client-authoritative. Account validation không chứng minh gameplay.

## Catalog và mở rộng

Nguồn giá ở [assets/economy](../../game-client/assets/economy/README.md), bản Spring ở resources; Docker chép nguồn client khi build. `EconomyJournalTest` so enum/catalog. Thêm item/tech phải cập nhật metadata, dự đoán client, validation server, gameplay sử dụng và test; không tái sử dụng ID cũ tùy tiện.

Kết quả thắng được server cộng thưởng trong [luồng 04](04-luu-ket-qua-tran.md), không qua thao tác tự đặt ví. Khi lỗi lưu, UI báo rõ và chặn giao dịch tiếp cho tới retry hoặc nạp lại state. Đóng shop để next sẽ chờ batch cuối, tránh đua bootstrap.
