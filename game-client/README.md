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

## Build và test

```powershell
.\gradlew.bat core:test
.\gradlew.bat html:dist
.\gradlew.bat html:copyToServer
```

- HTML5 output: `html/build/dist/`
- Bundle được copy tới: `../server-spring/src/main/resources/static/game/libgdx/`

Docker ở root repository tự build client bằng JDK 17, nên máy host không cần cài đúng phiên bản Java/Gradle.
