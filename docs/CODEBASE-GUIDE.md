# Hướng dẫn đọc code

## Bắt đầu từ luồng campaign

Đọc theo thứ tự:

1. `server-spring/.../web/PageController.java`: mở trang `/play`.
2. `templates/play.html`: nhúng bundle LibGDX.
3. `game-client/html/.../HtmlLauncher.java`: tạo `LocalCampaignSession`.
4. `server-spring/.../web/GameApiController.java`: trả bootstrap map.
5. `game-client/core/.../session/LocalCampaignSession.java`: quản lý vòng đời một màn.
6. `game-client/core/.../gameplay/GameSimulation.java`: gameplay chính.
7. `game-client/core/.../render/GameRenderer.java`: vẽ snapshot.
8. `game-client/html/.../GwtAppBridge.java`: gửi kết quả.
9. `server-spring/.../result/ResultService.java`: validate và lưu.

## Khi sửa gameplay

Sửa dưới `game-client/core/src/main/java/com/tankzors/client/gameplay`. Test tương ứng nằm ở `game-client/core/src/test`.

Không thêm AI, collision, tank hoặc game loop vào `server-spring`.

## Khi sửa giao diện website

- Controller/model: `server-spring/src/main/java/com/tankzors/web`
- Template: `server-spring/src/main/resources/templates`
- CSS: `server-spring/src/main/resources/static/css`

## Khi sửa API/database

- REST: `web/GameApiController`
- Kết quả: package `result`
- Tiến trình: package `progress`
- Leaderboard: package `leaderboard`
- Migration: `resources/db/migration`

## Assets và output build

Asset nguồn nằm ở `game-client/assets`. Gradle chuẩn bị asset cho GWT và tạo bundle tại `game-client/html/build/dist`.

`html:copyToServer` đồng bộ bundle tới `server-spring/src/main/resources/static/game/libgdx`. Thư mục đích là generated output phục vụ local Spring; Docker luôn thay nó bằng output client vừa build.

## Các tên dễ nhầm

- `game-client/.../gameplay/entity`: entity mới, chạy ở client; phải giữ.
- `server-spring/.../map/GameMap`: DTO map cho API, không phải world model.
- `/api/game/**`: REST campaign mới.
- `/game/libgdx/**`: static bundle mới.
- `/coop`: WebSocket relay mới.
- WebSocket `/game`, `GameLoop` phía server, React/Javalin: đã xóa.
