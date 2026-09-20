# Codebase Structure

**Analysis Date:** 2026-09-20

## Directory Layout

```text
LapTrinhMang/
├── game-client/          # LibGDX source and Gradle modules
│   ├── core/             # Platform-independent gameplay and sessions
│   ├── desktop/          # Desktop launcher for development
│   └── html/             # GWT/HTML5 launcher and web build
├── server-spring/        # Spring Boot web, API, persistence and co-op relay
│   └── src/
│       ├── main/         # Java code, templates, static files and migrations
│       └── test/         # Spring and integration tests
├── infra/                # Docker Compose runtime definition
├── docs/                 # Architecture, networking, protocol and runbook docs
├── .planning/            # OpenGSD project memory and phase state
└── README.md             # Main setup and operating guide
```

## Directory Purposes

**`game-client/`:**
- Purpose: Sở hữu gameplay simulation và rendering.
- Contains: Gradle multi-project với `core`, `desktop`, `html`.
- Key files: `build.gradle`, `settings.gradle`, `gradlew.bat`.
- Subdirectories: `core` cho logic dùng chung; `desktop` và `html` cho launcher.

**`server-spring/`:**
- Purpose: Website, auth, REST, persistence, admin, static hosting và `/coop` relay.
- Contains: Maven Spring Boot application.
- Key files: `pom.xml`, `Dockerfile`, `src/main/resources/application.yml`.
- Subdirectories: `src/main/java`, `src/main/resources`, `src/test`.

**`infra/`:**
- Purpose: Khởi động toàn bộ application và PostgreSQL bằng Docker Compose.
- Key files: `docker-compose.yml`.

**`docs/`:**
- Purpose: Tài liệu kiến trúc và vận hành dành cho người phát triển.
- Key files: `architecture.md`, `networking.md`, `protocol-v2.md`, `runbook.md`.

**`.planning/`:**
- Purpose: Shared memory của OpenGSD.
- Contains: Project, requirements, roadmap, state, codebase map và phase artifacts.

## Key File Locations

**Entry Points:**
- `server-spring/src/main/java`: Spring Boot application entrypoint và server packages.
- `game-client/html/src/main/java`: GWT/HTML5 launcher.
- `game-client/desktop/src/main/java`: Desktop launcher.

**Configuration:**
- `infra/docker-compose.yml`: Runtime services và environment wiring.
- `server-spring/pom.xml`: Spring dependencies và Maven build.
- `game-client/build.gradle`: Shared LibGDX/Gradle configuration.
- `game-client/html/build.gradle`: HTML5 distribution/copy tasks.

**Core Logic:**
- `game-client/core/src/main/java`: Gameplay world, entities, AI, sessions và protocol client.
- `server-spring/src/main/java`: Controllers, services, repositories, security và relay.
- `server-spring/src/main/resources/db/migration`: Flyway schema migrations.

**Testing:**
- `game-client/core/src/test`: Gameplay/unit tests.
- `server-spring/src/test`: Server unit/integration tests.

**Documentation:**
- `README.md`: Cách build/run/test chính.
- `docs/`: Kiến trúc, networking, protocol và runbook.

## Naming Conventions

**Files:**
- Java types dùng PascalCase và package theo domain.
- Tests dùng hậu tố `Test` hoặc `IT` theo phạm vi hiện có.
- Flyway migrations dùng dạng `V<number>__description.sql`.
- OpenGSD root artifacts dùng UPPERCASE Markdown names.

**Directories:**
- Java packages dùng lowercase.
- Phase directories dùng `NN-kebab-case`.
- Generated HTML5 output nằm dưới static `game/libgdx`.

## Where to Add New Code

**Gameplay feature:**
- Primary code: `game-client/core/src/main/java`.
- Tests: `game-client/core/src/test`.
- Platform glue only: `game-client/html` hoặc `game-client/desktop`.

**Web/API/persistence feature:**
- Implementation: `server-spring/src/main/java` theo package controller/service/repository/domain phù hợp.
- Templates/static assets: `server-spring/src/main/resources`.
- Tests: `server-spring/src/test`.

**Database change:**
- Migration: `server-spring/src/main/resources/db/migration`.
- Mapping/repository: `server-spring/src/main/java`.

**Networking change:**
- Shared packet/session behavior: `game-client/core`.
- Room authorization và relay: `server-spring` `/coop` packages.
- Contract docs: `docs/protocol-v2.md`.

## Special Directories

**`server-spring/src/main/resources/static/game/libgdx/`:**
- Purpose: Generated/packaged HTML5 game served by Spring.
- Source: `game-client/html` build output.
- Rule: Không sửa generated JavaScript trực tiếp.

**`game-client/**/build/` và `server-spring/target/`:**
- Purpose: Local build outputs.
- Source: Gradle/Maven.
- Committed: No.

---
*Structure analysis: 2026-09-20*  
*Update when directory structure changes*
