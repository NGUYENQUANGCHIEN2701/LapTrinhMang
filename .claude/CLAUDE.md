<!-- GSD:project-start source:PROJECT.md -->

## Project

**Tankzors**

Tankzors là game xe tăng PvE chạy trên website cho đồ án Lập trình mạng. Website dùng Spring Boot và Thymeleaf; gameplay cùng phần render chạy bằng LibGDX HTML5; PostgreSQL lưu tài khoản, kết quả, tiến trình và bảng xếp hạng.

Kiến trúc hiện tại là client-authoritative cho campaign và host-authoritative cho co-op. Spring quản lý web, API, persistence, identity, room và relay; Spring không chạy một gameplay engine thứ hai.

**Core Value:** Người chơi có thể mở game từ website, chơi campaign hoàn chỉnh trong trình duyệt và lưu kết quả bền vững bằng một kiến trúc mạng rõ ràng, dễ chạy và dễ giải thích.

### Constraints

- **Tech stack**: Spring dùng Java 21; GWT build dùng JDK 17 trong Docker — tránh phụ thuộc phiên bản Java trên máy host.
- **Gameplay ownership**: Gameplay mới chỉ được thêm trong `game-client/core` — không tái tạo simulation trong Spring.
- **Server boundary**: Spring chỉ giữ web, API, persistence, identity, room và relay — không chứa entity/AI/collision/game loop gameplay.
- **Static namespace**: `/game/libgdx/**` là tài nguyên HTML5, không phải WebSocket gameplay.
- **Build**: Docker multi-stage là workflow chuẩn — mọi thay đổi tích hợp phải được kiểm tra qua Docker.
- **Generated output**: Không sửa trực tiếp JavaScript sinh ra trong `server-spring/src/main/resources/static/game/libgdx`.

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

| Lớp | Công nghệ |
|---|---|
| Gameplay/render | Java, LibGDX 1.12.1 |
| Browser client | GWT/HTML5 build bằng Gradle 8.10.2 + JDK 17 |
| Desktop dev client | LWJGL3 |
| Web/backend | Spring Boot 3.3.4, Java 21, Maven |
| UI website | Thymeleaf, CSS |
| Auth | Spring Security session + BCrypt |
| Persistence | Spring Data JPA, Flyway, PostgreSQL 16 |
| Co-op | Spring WebSocket `/coop`, JSON envelope v2 |
| Test | JUnit 5, Spring Boot Test, H2 test profile |
| Runtime | Docker Compose, multi-stage Dockerfile |
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

- Java package theo trách nhiệm, không theo “phase migration”.
- REST DTO và persistence entity tách khỏi gameplay entity.
- Thay đổi gameplay phải có test trong `game-client/core/src/test`.
- Thay đổi Spring service/controller phải có test H2/MockMvc phù hợp.
- Không sửa generated bundle trong `server-spring/src/main/resources/static/game/libgdx` bằng tay.
- Asset nguồn đặt trong `game-client/assets`.
- Map JSON nguồn cần đồng bộ giữa client assets và Spring resources khi schema thay đổi.
- Docker build là kiểm tra tích hợp cuối cho thay đổi chạm cả client và server.

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## Luồng campaign

## Luồng co-op mục tiêu

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

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `$gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `$gsd-debug` for investigation and bug fixing
- `$gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
