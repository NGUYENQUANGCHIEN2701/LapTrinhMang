# 05. Tiến trình, Continue, retry và chuyển màn

Progress lưu theo `(session_id, unlocked_level)`; `session_id` chứa khóa người chơi `user:<username>`, không phải cookie. Màn có `completed=true` đã thắng và không được bootstrap lại. Thua có thể chơi lại bằng lượt mới.

## Quy tắc

| Thao tác | Nơi xử lý | Hành vi |
|---|---|---|
| Lần đầu vào campaign | `PlayerProgressService.getUnlocked` | Khóa account, tạo màn 1 nếu chưa có progress |
| Continue | `getContinueLevel` và `PageController` | Chọn màn cao nhất còn được chơi |
| Thắng | `ResultService` → `markVictory` | Cùng transaction thưởng/kết quả: khóa màn thắng, mở màn kế |
| Retry thua | `LocalCampaignSession.restart` | Chờ lưu kết quả/giao dịch, bootstrap run mới; browser nạp kho đã lưu |
| Next | `LocalCampaignSession.nextLevel` | Chỉ sau VICTORY và xác nhận lưu; bootstrap màn kế |
| Đóng shop giữa hai màn | `GameClient` | Giữ ý định chuyển màn, chờ giao dịch cuối được xác nhận rồi mới next/thoát |
| Reset | `POST /api/progress/reset` | Cần CSRF; reset progress, không xóa ví, nghiên cứu, leaderboard hoặc lịch sử |

Nguồn: [PlayerProgressService](../../server-spring/src/main/java/com/tankzors/application/PlayerProgressService.java), [LocalCampaignSession](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java), [GameClient](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java).

Browser giữ tiêu hao đã được lưu, không khôi phục tự do checkpoint inventory như đường chạy desktop local. Nếu hết mọi loại đạn và không đủ 5 Xu, bootstrap có chính sách cấp lại 99 đạn thường để tránh kẹt tài khoản. Việc cấp này do server quyết định.

## Ví dụ

Sau khi `user:an` thắng màn 1, progress có `campaign_1 completed=true` và `campaign_2 completed=false`. Bootstrap màn 1 trả 403, màn 2 được cấp run mới. Retry payload kết quả màn 1 vẫn trả kết quả đã lưu, không cộng thưởng lại.

## Khi thêm màn hoặc cho chơi lại màn đã thắng

Thứ tự hiện khai báo trong `PlayerProgressService.CAMPAIGN_ORDER`, `StaticCampaignQueryService` và `LocalCampaignSession.nextCampaignLevel`. Thêm JSON thôi chưa đủ. Nếu cho replay màn thắng, phải thiết kế lại quyền truy cập, thưởng lần đầu, reset và leaderboard cùng lúc. Xem [DEVELOPMENT](../DEVELOPMENT.md#3-thêm-màn-hoặc-nhiệm-vụ).
