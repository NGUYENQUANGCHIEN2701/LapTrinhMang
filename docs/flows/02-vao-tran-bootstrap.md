# 02. Vào trận: website, khôi phục request và bootstrap

Trang `/play` nhúng game trong iframe tại `/game/libgdx/index.html`. Campaign không mở WebSocket. Mô phỏng chỉ được tạo sau bootstrap hợp lệ.

## Luồng hiện tại

```text
PageController.play: kiểm tra màn → play.html/iframe
  HtmlLauncher: tạo LocalCampaignSession + CampaignPersistence
  GameClient.create: asset/render/input → session.start
  BootstrapMapSource.requestLevel:
    GwtAppBridge.prepare → GET /api/game/account
      đọc playerId/revision, khôi phục outbox của đúng tài khoản
      gửi các ghi đang chờ theo thứ tự
    GET /api/game/bootstrap?levelId=...
      identity → account lock → kiểm tra quyền màn
      GameRunService.issue: lưu runId/owner/level/version/hạn dùng
      PlayerShopService.beginRun: đọc kho/ví, cấp đạn dự phòng nếu đủ điều kiện
    cập nhật revision bridge → onLevelLoadedWithShop
      nạp state server → parse map → tạo/swap simulation
      startCampaignLevel → briefing → chơi
```

## Các ranh giới

- [PageController](../../server-spring/src/main/java/com/tankzors/web/PageController.java) chỉ trả website; [GameApiController](../../server-spring/src/main/java/com/tankzors/web/GameApiController.java) cấp dữ liệu, không dựng world.
- [GameRunService](../../server-spring/src/main/java/com/tankzors/result/GameRunService.java) lưu metadata lượt 24 giờ để kiểm tra lúc submit. Run ID không còn là UUID trả về rồi bỏ qua ở server.
- [HtmlLauncher](../../game-client/html/src/main/java/com/tankzors/client/html/HtmlLauncher.java) chuyển JSON qua callback. [LocalCampaignSession](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java) sở hữu vòng đời simulation.
- Browser re-bootstrap sau khi đã chờ hết ghi dùng lại state authoritative mới, gồm cả số dư 0 và đồ đã mất. Desktop đọc assets local, không dùng REST.
- Một lượt mới không khôi phục world cũ. Outbox chỉ bảo vệ ghi tài khoản/kết quả khi reload cùng tab và storage còn.

## Xử lý lỗi

API chưa đăng nhập trả 401; màn chưa mở/đã thắng trả 403. Bootstrap timeout/lỗi parse gọi `onLoadError`; session hiện lỗi tải và cho thử lại. Nếu khôi phục được kết quả thắng của màn hiện tại trước bootstrap, màn đó đã hoàn thành; browser trở về campaigns thay vì cố tạo lại world màn đã khóa.

Session có `requestToken` để bỏ callback của yêu cầu cũ về muộn. Map được parse vào simulation tạm trước khi swap; lỗi dữ liệu không phá world đang có.

Đọc tiếp [04: lưu kết quả](04-luu-ket-qua-tran.md), [05: chuyển màn](05-tien-trinh-va-chuyen-man.md), [networking](../networking.md).
