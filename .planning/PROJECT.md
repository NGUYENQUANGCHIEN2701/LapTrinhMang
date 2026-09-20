# Tankzors

## What This Is

Tankzors là game xe tăng PvE chạy trên website cho đồ án Lập trình mạng. Website dùng Spring Boot và Thymeleaf; gameplay cùng phần render chạy bằng LibGDX HTML5; PostgreSQL lưu tài khoản, kết quả, tiến trình và bảng xếp hạng.

Kiến trúc hiện tại là client-authoritative cho campaign và host-authoritative cho co-op. Spring quản lý web, API, persistence, identity, room và relay; Spring không chạy một gameplay engine thứ hai.

## Core Value

Người chơi có thể mở game từ website, chơi campaign hoàn chỉnh trong trình duyệt và lưu kết quả bền vững bằng một kiến trúc mạng rõ ràng, dễ chạy và dễ giải thích.

## Requirements

### Validated

- ✓ Gameplay PvE, AI, collision và render chạy trong `game-client/core`.
- ✓ HTML5 bundle được Spring phục vụ tại `/game/libgdx/**`.
- ✓ Campaign bootstrap và submit kết quả qua REST.
- ✓ PostgreSQL, Flyway và JPA lưu user, result, progress và leaderboard.
- ✓ Website có login/register, campaign, settings, leaderboard và admin.
- ✓ Docker build đồng thời LibGDX HTML5, Spring JAR và chạy PostgreSQL.
- ✓ Server không còn gameplay entity, AI, collision, game loop, React hoặc Javalin cũ.

### Active

- [ ] Ghép lobby `/coop` với `HostGameSession` và `GuestGameSession` end-to-end.
- [ ] Kiểm thử hai browser cho create, join, ready, start, input, state và result.
- [ ] Hoàn thiện UX co-op khi disconnect, room close và reconnect.
- [ ] Hoàn tất security/release verification cho milestone v2.0.

### Out of Scope

- Server-authoritative gameplay và WebSocket `/game` — kiến trúc này đã bị thay bằng client/host-authoritative.
- React, Vite và Javalin — không thuộc stack hiện tại.
- PvP cạnh tranh, rollback netcode và matchmaking theo trình độ — vượt phạm vi đồ án hiện tại.
- Mobile app và voice/video chat — không phục vụ core value của milestone.

## Context

- Đây là đồ án môn Lập trình mạng, hiện được phát triển bởi một người.
- Campaign có ba map, AI, collision, scoring, victory/defeat và progression.
- Spring cung cấp auth, website, REST API, persistence, admin và WebSocket relay `/coop`.
- Relay, room và client session co-op đã có nền tảng nhưng chưa được nối end-to-end từ lobby vào game frame.
- Repository là brownfield; tài liệu OpenGSD phải mô tả code hiện tại, không giữ migration plan cũ như active work.

## Constraints

- **Tech stack**: Spring dùng Java 21; GWT build dùng JDK 17 trong Docker — tránh phụ thuộc phiên bản Java trên máy host.
- **Gameplay ownership**: Gameplay mới chỉ được thêm trong `game-client/core` — không tái tạo simulation trong Spring.
- **Server boundary**: Spring chỉ giữ web, API, persistence, identity, room và relay — không chứa entity/AI/collision/game loop gameplay.
- **Static namespace**: `/game/libgdx/**` là tài nguyên HTML5, không phải WebSocket gameplay.
- **Build**: Docker multi-stage là workflow chuẩn — mọi thay đổi tích hợp phải được kiểm tra qua Docker.
- **Generated output**: Không sửa trực tiếp JavaScript sinh ra trong `server-spring/src/main/resources/static/game/libgdx`.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Client-authoritative campaign | Chỉ duy trì một gameplay engine trong LibGDX | ✓ Good |
| REST cho campaign bootstrap/result | Hai thao tác không cần kết nối realtime | ✓ Good |
| Host-authoritative co-op qua `/coop` relay | Có networking nhưng Spring không giữ world simulation | — Pending end-to-end validation |
| Spring Boot + Thymeleaf + PostgreSQL | Một backend cho web, auth, API và persistence | ✓ Good |
| Docker multi-stage là workflow chuẩn | Cố định JDK, Gradle và Maven giữa các máy | ✓ Good |

## Evolution

- Sau mỗi phase, chuyển requirement đã xác minh từ Active sang Validated và cập nhật quyết định mới.
- Khi kiến trúc hoặc phạm vi đổi, cập nhật đồng thời `PROJECT.md`, `REQUIREMENTS.md`, `ROADMAP.md` và `STATE.md`.
- Không đưa lại engine server-authoritative, React hoặc Javalin nếu chưa có quyết định kiến trúc mới được ghi rõ.

---
*Last updated: 2026-09-20 after client-authoritative cleanup and OpenGSD recovery*
