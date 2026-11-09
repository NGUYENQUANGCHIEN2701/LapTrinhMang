# Trace các luồng chính của Tankzors

Bộ tài liệu này trả lời câu hỏi: **khi người chơi làm X, code nào chạy, theo thứ tự nào?**

Các luồng 02/04/05/06 đã cập nhật cho issued run, giao dịch server và outbox. Các trích code có số dòng ở luồng còn lại gắn commit lịch sử `543ce0f`; khi đọc source mới hãy tìm theo tên class/hàm.

Để phát triển tiếp, đọc thêm [DEVELOPMENT](../DEVELOPMENT.md) và [CODE-REVIEW](../CODE-REVIEW.md). Các tài liệu này mô tả đường chạy; báo cáo sửa lỗi ghi riêng test, trạng thái xác minh và giới hạn còn lại.

## Bức tranh trong 1 phút

Hệ thống có 3 khối:

```text
┌─ Trình duyệt ────────────────────────┐        ┌─ Spring Boot :8081 ────────┐      ┌─ PostgreSQL ─┐
│ Trang web Thymeleaf (menu, xếp hạng) │ <HTTP> │ Controller trang web       │      │ tài khoản    │
│                                      │        │ REST API /api/**           │ <──> │ kết quả trận │
│ iframe: game LibGDX (biên dịch       │ <REST> │ Service: kết quả, tiến     │      │ tiến trình   │
│ sang JavaScript bằng GWT)            │        │ trình, xếp hạng, cửa hàng  │      │ xếp hạng     │
│  → toàn bộ mô phỏng trận chạy ở đây  │        │ KHÔNG chạy mô phỏng        │      │ cửa hàng     │
└──────────────────────────────────────┘        └────────────────────────────┘      └──────────────┘
```

Điều quan trọng nhất cần nhớ: **trận đấu chạy hoàn toàn trong trình duyệt** (kiến trúc client-authoritative). Server cấp lượt/bản đồ, kiểm tra thay đổi tài khoản trong trận và lưu kết quả cuối trận. Không truyền toàn bộ world liên tục lên server.

## Một lượt chơi đi qua các luồng nào

```text
Đăng nhập → Chọn màn → Mở /play → Tải game → Bootstrap → Chơi (lặp mỗi khung hình) → Thắng/Thua → Gửi kết quả
  [01]        [05]       [02]       [02]        [02]            [03]                    [03]          [04]
                                                                                          │
                              Vào cửa hàng [06] ← Màn hình chiến thắng [05] ←─────────────┘
                                     │
                                     └→ Sang màn kế: bootstrap lại [05] [02]

Xếp hạng được cập nhật trong lúc lưu kết quả [04], xem ở trang /leaderboard [07].
```

## Danh sách tài liệu

| File | Luồng | Đọc khi muốn biết |
|---|---|---|
| [01-dang-nhap-va-dinh-danh.md](01-dang-nhap-va-dinh-danh.md) | Đăng ký, đăng nhập, định danh người chơi | `playerId` đến từ đâu, ai được vào trang nào |
| [02-vao-tran-bootstrap.md](02-vao-tran-bootstrap.md) | Từ `/play` đến khi xe xuất hiện | Game được tải thế nào, `runId` sinh ở đâu |
| [03-vong-lap-game.md](03-vong-lap-game.md) | Vòng lặp mỗi khung hình | Bấm phím thì xe đi thế nào, thắng/thua xác định ở đâu |
| [04-luu-ket-qua-tran.md](04-luu-ket-qua-tran.md) | Gửi và lưu kết quả | Chống ghi trùng, kẹp phần thưởng |
| [05-tien-trinh-va-chuyen-man.md](05-tien-trinh-va-chuyen-man.md) | Mở màn, Continue, chơi lại, màn kế | Mỗi màn chỉ chơi một lần được chặn ở đâu |
| [06-cua-hang-va-tui-do.md](06-cua-hang-va-tui-do.md) | Mua đồ, đồng bộ ví, dùng vật phẩm | Xu/Sao được lưu thế nào |
| [07-bang-xep-hang.md](07-bang-xep-hang.md) | Ghi và đọc bảng xếp hạng | Tên hiển thị, gộp điểm |
| [08-build-va-trien-khai.md](08-build-va-trien-khai.md) | Docker build, khởi động, migration | Client vào trong server thế nào |

## Muốn tìm nhanh

| Tôi muốn sửa... | Mở file |
|---|---|
| Luật gameplay (đạn, va chạm, nhiệm vụ, thắng/thua) | [GameSimulation.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java) |
| Xe người chơi di chuyển | [PlayerTank.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/entity/PlayerTank.java) |
| AI xe địch | [BuildsBotBrain.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/ai/BuildsBotBrain.java) |
| Phím bấm | [ClientInputProcessor.java](../../game-client/core/src/main/java/com/tankzors/client/input/ClientInputProcessor.java) |
| Vòng đời màn chơi phía client | [LocalCampaignSession.java](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java) |
| Vẽ màn hình, HUD | [GameRenderer.java](../../game-client/core/src/main/java/com/tankzors/client/render/GameRenderer.java) |
| Gọi mạng từ game | [HtmlLauncher.java](../../game-client/html/src/main/java/com/tankzors/client/html/HtmlLauncher.java), [GwtAppBridge.java](../../game-client/html/src/main/java/com/tankzors/client/html/GwtAppBridge.java) |
| API của game | [GameApiController.java](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java) |
| Lưu kết quả, kẹp thưởng | [ResultService.java](../../server-spring/src/main/java/com/tankzors/result/ResultService.java) |
| Mở khoá màn | [PlayerProgressService.java](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java) |
| Trang web | [PageController.java](../../server-spring/src/main/java/com/tankzors/web/PageController.java), [templates/](../../server-spring/src/main/resources/templates/) |
| Phân quyền | [SecurityConfig.java](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java) |
| Bản đồ | [game-client/assets/maps/](../../game-client/assets/maps/) và [server-spring/.../maps/](../../server-spring/src/main/resources/maps/), hai bản phải giống nhau |

## Từ khoá hay gặp

| Từ | Nghĩa |
|---|---|
| `playerId` | Khoá định danh người chơi trên server. Đã đăng nhập thì có dạng `user:<tên đăng nhập>` |
| `levelId` | Mã màn: `campaign_1`, `campaign_2`, `campaign_3` |
| `runId` | Mã của một lượt chơi, dạng `run-<UUID>`, sinh mới ở mỗi lần bootstrap |
| bootstrap | Lời gọi `GET /api/game/bootstrap`, lấy dữ liệu để bắt đầu một lượt chơi |
| snapshot | Ảnh chụp trạng thái trận để renderer vẽ. Renderer không sửa dữ liệu game |
| step | Một bước mô phỏng, dài 1/30 giây |
| Workshop | Tên trong code của cửa hàng |
| Sao / Xu | `stars` / `coins`. Xu mua đồ, Sao mua nghiên cứu |

## Lưu ý khi đọc code

- **Phần chưa dùng trong sản phẩm.** Code còn các lớp `/coop`, `RoomRelayHandler`, `HostGameSession`, `GuestGameSession`, `GameSocket`, nhánh `coopMode` trong `GameSimulation`. Sản phẩm chỉ có chế độ một người chơi nên tài liệu này bỏ qua chúng, gặp trong code thì có thể lờ đi. Vì lý do tương tự, `HtmlLauncher` vẫn tạo `BrowserWebSocket` nhưng không kết nối, vì [`LocalCampaignSession` không cần socket](../../game-client/core/src/main/java/com/tankzors/client/session/GameSession.java#L54-L55).
- **Trích code.** Đoạn code trong tài liệu được chép từ source, `// ...` nghĩa là đã lược bớt.
- **Mở link.** Trong VS Code, Ctrl+Click vào link để nhảy tới đúng file và dòng.
