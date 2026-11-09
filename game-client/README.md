# Tankzors LibGDX client

Đây là nơi duy nhất chạy gameplay simulation.

## Module

- `core`: entity, AI, collision, rules, simulation, session, input và renderer dùng chung.
- `html`: GWT launcher, tải bootstrap từ Spring và gửi kết quả qua REST.
- `desktop`: launcher phát triển local, đọc map trực tiếp từ assets.
- `assets`: asset nguồn dùng chung cho desktop và HTML5.

`GameClient` chỉ điều phối input/render và luôn nhận một `GameSession`:

- `LocalCampaignSession`: campaign client-authoritative.
- `HostGameSession`: nền tảng host cho co-op relay.
- `GuestGameSession`: nền tảng guest chỉ render snapshot host.

Không còn nhánh kết nối WebSocket `/game` hay parse state của engine server cũ.

## Luồng cần biết

`GameClient` điều phối input/render. Campaign dùng `LocalCampaignSession` để bootstrap map, giữ `PlayerShopState`, tạo `GameSimulation` và chuyển màn. Simulation chạy 30 bước/giây; `WorldSnapshotMapper` tạo dữ liệu cho renderer. Browser gửi kết quả và đồng bộ shop qua `GwtAppBridge`; desktop chỉ in kết quả, không lưu tài khoản lên Spring.

| Module | Đọc tiếp | Sửa khi |
|---|---|---|
| `core` | [core/README.md](core/README.md) | Thêm luật gameplay, AI, session, input, HUD, shop |
| `html` | [html/README.md](html/README.md) | Tích hợp trình duyệt, REST, GWT và iframe |
| `desktop` | [desktop/README.md](desktop/README.md) | Chạy gameplay local để debug |
| `assets` | [assets/README.md](assets/README.md) | Thêm map, ảnh và âm thanh nguồn |

## Build và test

Các lệnh sau chạy trong `game-client/`, với JDK 17 cho workflow client chuẩn:

```powershell
.\gradlew.bat core:test
.\gradlew.bat html:dist
.\gradlew.bat html:copyToServer
.\gradlew.bat desktop:run --args="--level campaign_1"
```

- HTML5 output: `html/build/dist/`
- Bundle được copy tới: `../server-spring/src/main/resources/static/game/libgdx/`

Docker ở root repository tự build client bằng JDK 17, nên máy host không cần cài đúng phiên bản Java/Gradle.

`html:copyToServer` đã phụ thuộc `html:dist`, không cần chạy cả hai khi chỉ muốn copy bản mới. Task này đồng bộ output generated; không chỉnh trực tiếp bundle trong Spring. Trên Linux/macOS dùng `./gradlew` thay `gradlew.bat`.

Test report: `core/build/reports/tests/test/index.html`. Core test chạy Java trên JVM; compile GWT và thử browser vẫn cần để phát hiện API Java không được GWT hỗ trợ, lỗi JSNI, asset, WebGL, focus và âm thanh.

## Phát triển tiếp

- Giữ logic dùng chung tương thích GWT; tránh đưa API filesystem, thread hoặc dependency chỉ chạy JVM vào core.
- Luật mới phải có test trong `core/src/test`; renderer nhận snapshot và không sửa world.
- Map cần đồng bộ với `server-spring/src/main/resources/maps`. Khi thêm màn, cập nhật cả catalog server và thứ tự chuyển màn client.
- `HostGameSession` và `GuestGameSession` còn là nền tảng; launcher browser chưa nối co-op. Không xem `mode=coop` là chế độ chơi hoàn chỉnh.
- Luồng browser dùng `CampaignPersistence` và journal giao dịch; chờ xác nhận lưu, retry theo outbox và giữ revision. Khi sửa cần giữ test callback/thứ tự và parity catalog; xem [báo cáo](../docs/CODE-REVIEW.md) và [hướng dẫn](../docs/DEVELOPMENT.md).
