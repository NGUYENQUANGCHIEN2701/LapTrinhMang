# Kiến trúc codebase

## Luồng campaign

`play.html` -> bundle LibGDX -> `HtmlLauncher` -> `LocalCampaignSession` -> REST bootstrap -> `GameSimulation` -> render -> REST result -> PostgreSQL.

## Luồng co-op mục tiêu

Guest gửi PLAYER_INPUT qua `/coop`; relay chuyển tới host; host chạy `GameSimulation` và phát WORLD_STATE/MATCH_RESULT cho guest.

## Ranh giới

- `game-client/core/gameplay`: nguồn sự thật gameplay.
- `game-client/core/session`: vòng đời mode local/host/guest.
- `server-spring/web/application/result/progress/leaderboard`: website, use case và persistence.
- `server-spring/network/room`: room membership và relay; không simulation.
- `server-spring/map`: DTO/loader phục vụ API, không world entity.

## Quy tắc dependency

- Server không phụ thuộc class gameplay client.
- Renderer nhận snapshot và không sửa world.
- Campaign không mở WebSocket.
- Co-op không được dùng lại protocol v1 hoặc endpoint `/game`.
