# Stack Research

**Domain:** Real-time 2D multiplayer game — Java backend, React web frontend, WebSocket transport, solo course project
**Researched:** 2026-08-13
**Confidence:** MEDIUM (web-search-based, cross-verified across official npm/Maven/release-note sources for versions; architecture pattern choices corroborated by multiple independent sources but not project-specific benchmarked)

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Java (JDK) | 21 LTS (25 LTS also fine) | Backend runtime | 21 is the most broadly installed/tooled LTS in university lab environments as of 2026 and has premier support through Sept 2028; 25 LTS (Sept 2025) is the more future-proof choice with virtual-thread fixes and better memory/startup numbers if your dev machine and IDE already have it — either is safe, don't chase a non-LTS version. |
| Javalin | 7.0.x (requires Java 17+, built on Jetty 12) | Backend web framework — unifies REST endpoints (auth, leaderboard) and the real-time WebSocket game connection in one lightweight process | A "thin layer over Jetty, no annotations, no magic." One framework serves both `post("/auth/login", ...)` REST routes and `ws("/game", ...)` for the tank match connection, so you don't maintain two separate network stacks. Its WebSocket API exposes `onConnect/onMessage/onClose` almost 1:1 with the underlying socket lifecycle — this keeps the client-server networking mechanics visible instead of hidden behind DI/annotation magic, which matters for a networking course. Minimal boilerplate = more of the "several weeks" budget goes to actual game logic. |
| React | 19.2.x | Frontend UI framework | User's explicit choice; current major version, actively maintained, standard for 2025/2026 web frontends. |
| Vite | 8.x | Frontend build tool / dev server | `create-react-app` was officially deprecated by the React team (Feb 2025); Vite is the community-recommended replacement — fast HMR, minimal config, first-class React + TypeScript template. |
| TypeScript | 5.x (via Vite's `react-ts` template) | Typed client code | The client/server message protocol (move, shoot, hit, leaderboard payloads) is exactly the kind of contract that benefits from compile-time checking; catches "field renamed on server, client still sends old shape" bugs before runtime. Modest learning overhead, worth it for a networking-protocol-heavy project. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jackson (`jackson-databind`) | bundled with Javalin 7 (2.18.x line) | JSON (de)serialization for both REST bodies and WebSocket messages | Default already wired into Javalin — don't add Gson on top, just reuse it for encoding game-state snapshots and REST payloads with one consistent library. |
| H2 Database | 2.4.240 | Embedded SQL database for accounts + match/leaderboard history | Pure-Java, file-mode embedded DB — zero install, zero separate server process to run/manage during grading/demo. Perfect fit for "a few tables, solo dev, several weeks." |
| JDBI | 3.54.0 | Thin SQL access layer over JDBC | You have ~2-3 tables (`users`, `matches`/`leaderboard`). JDBI gives you clean parameterized SQL with object mapping in a few lines, without the ORM machinery (entity lifecycle, lazy loading, session management) that Hibernate/JPA would force you to learn for no real benefit at this scale. |
| jBCrypt | 0.4 | Password hashing for registration/login | Industry-standard bcrypt hashing without pulling in all of Spring Security just to hash a password. |
| JJWT (`jjwt-api` + `jjwt-impl` + `jjwt-jackson`) | 0.13.0 | Stateless session tokens for login persistence across REST calls and to authenticate the WebSocket handshake | Modern JJWT split into 3 artifacts (the old single `jjwt` dependency is deprecated). Use a JWT in an `Authorization` header for REST calls, and pass the same token as a query param or first WS message during the WebSocket handshake to identify the player before joining a match. |
| `java.util.concurrent.ScheduledExecutorService` | JDK built-in | Server-side authoritative game tick loop (e.g. ~20–30 Hz) | No external library needed — schedule a fixed-rate task per active match that advances tank/bullet positions, resolves collisions against walls, and broadcasts the updated state to both players' WebSocket sessions. This is the "server owns game state" mechanism described in PROJECT.md. |
| native browser `WebSocket` API | built into all evergreen browsers | Client-side real-time connection | No client library needed or wanted — pairs directly with Javalin's raw `ws()` endpoint (both speak plain RFC 6455 WebSocket + JSON text frames). |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Maven | Backend build/dependency management | More common in university course settings than Gradle; simpler declarative XML for a project this size; Javalin's own docs default to Maven quickstarts. |
| Vite dev server | Frontend hot-reload dev loop | `npm create vite@latest client -- --template react-ts` |
| IntelliJ IDEA (Community) or VS Code | IDE | Either works fine; IntelliJ has stronger out-of-the-box Java/Maven support, VS Code is lighter and shares the same editor for the React side. |

## Installation

```bash
# Frontend (React + TypeScript via Vite)
npm create vite@latest client -- --template react-ts
cd client
npm install

# Backend (Maven pom.xml dependencies)
# <dependency>org.javalin:javalin:7.0.x</dependency>
# <dependency>com.h2database:h2:2.4.240</dependency>
# <dependency>org.jdbi:jdbi3-core:3.54.0</dependency>
# <dependency>org.jdbi:jdbi3-sqlobject:3.54.0</dependency>  (optional, declarative queries)
# <dependency>org.mindrot:jbcrypt:0.4</dependency>
# <dependency>io.jsonwebtoken:jjwt-api:0.13.0</dependency>
# <dependency>io.jsonwebtoken:jjwt-impl:0.13.0</dependency> (runtime scope)
# <dependency>io.jsonwebtoken:jjwt-jackson:0.13.0</dependency> (runtime scope)
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|--------------------------|
| Javalin (REST + WebSocket in one process) | Spring Boot 4.1 + `spring-boot-starter-websocket` + Spring Data JPA + Spring Security | If you already know Spring from other coursework and want the "industry standard enterprise Java" experience, or your instructor specifically wants to see Spring. Trade-off: real learning-curve tax (DI, annotations, entity mapping, Spring Security config) inside a several-week solo timeline — budget extra time if you go this route. |
| Javalin `ws()` endpoint | Raw `org.java-websocket:Java-WebSocket:1.6.0` (barebones WebSocket-only server, no HTTP routing) | If the instructor specifically wants to see a framework-free WebSocket server with nothing hiding the handshake/frame handling, and you're willing to hand-build a tiny separate HTTP layer (e.g. `com.sun.net.httpserver.HttpServer`) for auth/leaderboard REST endpoints. More "networking transparent" but doubles your network-stack surface area. |
| Canvas 2D API (no library) for rendering | PixiJS 8.19.0 | If you want smoother sprite animation, particle effects (explosions, muzzle flash), or camera/viewport handling and don't mind an added dependency + its API to learn. Good "polish" upgrade after the core game loop works. |
| Canvas 2D API / PixiJS | Phaser (full game framework) | Only if you want a built-in physics engine, tilemap system, and scene manager. For a simple top-down 2-tank arena with axis-aligned wall collisions, Phaser's scope (physics engines, asset pipeline, scene graph) is more machinery than the game needs and adds a learning curve disproportionate to the payoff. |
| H2 (embedded, file-mode) | PostgreSQL / MySQL | If you specifically want to demonstrate running a separate database server process as part of the "networking" story, or your course environment already provisions one. Otherwise it's pure setup overhead (install, run, credentials) for 2-3 tables. |
| JDBI (thin SQL layer) | Hibernate / JPA | If your course has already taught JPA and you want to reuse that knowledge, or the schema is expected to grow substantially. For `users` + `matches` it's not worth the entity-mapping ceremony. |
| Maven | Gradle | If you're more comfortable with Gradle from prior coursework — functionally equivalent for a project this size, pick whichever you already know. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|--------------|
| Socket.IO (`socket.io-client` on the React side) | Socket.IO is not plain WebSocket — it layers its own handshake/protocol (polling fallback, packet framing, room semantics) that requires a matching Socket.IO server implementation. There is no first-class, actively maintained Socket.IO server for plain Java; you'd be fighting protocol mismatch instead of building the game. | Native browser `WebSocket` API on the client, talking plain RFC 6455 WebSocket to Javalin's `ws()` endpoint on the server. |
| Full Spring Boot + Spring Security + JPA/Hibernate as the *default* choice | Front-loads weeks of framework learning (DI container, annotation-driven config, entity lifecycle, Spring Security filter chains) that competes directly with the actual deliverable — a working real-time client-server tank game — inside a solo, several-week timeline. Fine as a deliberate choice if you already know Spring; risky as a default. | Javalin + JDBI + jBCrypt/JJWT (see Recommended Stack) — same underlying concepts (routing, auth, persistence) with far less ceremony. |
| Hand-rolled raw TCP/UDP byte-level protocol | PROJECT.md confirms this is explicitly not required for grading; designing your own binary framing/parsing adds real complexity (message boundaries, partial reads, endianness) without a corresponding grading requirement. Worth doing later only if you specifically want the extra learning value once the WebSocket version already works end-to-end. | Java WebSocket server (Javalin `ws()` or `Java-WebSocket`) + JSON text frames. |
| A full game engine (Phaser) as the default renderer | Physics engine, scene graph, tilemap loader, and asset pipeline are overkill for two tanks, bullets, and static wall rectangles on a top-down 2D map — you'd spend project time learning Phaser's abstractions instead of your own game/network logic. | Plain `<canvas>` + `requestAnimationFrame` render loop in a React component; upgrade to PixiJS only if you want animation/particle polish later. |
| A full ORM (Hibernate/JPA) for a 2-3 table schema | Entity lifecycle management, lazy-loading pitfalls, and session/transaction boundaries are real complexity that pays off at scale, not for `users` + `matches`. | JDBI (or even plain JDBC) with straightforward parameterized SQL. |
| Client-side prediction + server reconciliation (rollback netcode) | This is the deep end of multiplayer game networking (used for competitive shooters over unreliable/high-latency connections). For a 1v1 course project likely tested on localhost/LAN with low latency, it adds significant complexity for no visible benefit and risks becoming the whole project instead of a feature of it. | Authoritative server tick loop broadcasting the latest state (optionally with simple client-side interpolation between the last two snapshots for visual smoothness) — see Architecture research for detail. |

## Stack Patterns by Variant

**If you want maximum simplicity and the smallest new-framework surface area (recommended default):**
- Javalin + Canvas 2D + H2 + JDBI + jBCrypt/JJWT
- Because it's one backend framework for both REST and WebSocket, zero external DB server, and no game-engine dependency — the fastest path to a working end-to-end demo within a few weeks solo.

**If you already know Spring from coursework and want the "enterprise Java" experience:**
- Spring Boot 4.1 + `spring-boot-starter-websocket` (raw handler, skip STOMP — STOMP's session-iteration locking is unnecessary overhead for a 1v1 match broadcast) + Spring Data JPA + H2 or PostgreSQL + Spring Security with JWT
- Because you reuse existing Spring knowledge; budget extra time for the WebSocket handler config and security filter chain setup.

**If you want visual polish beyond flat shapes (explosions, smooth tank rotation, particle trails):**
- Add PixiJS 8.19.0 on top of the Canvas-API baseline once core gameplay works
- Because PixiJS is a rendering layer only (no physics/scene assumptions), so it drops into an existing React + Canvas structure without forcing an architecture rewrite.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|------------------|-------|
| Javalin 7.0.x | Java 17+, Jetty 12 | Jetty 12 moved from `javax.servlet.*` to `jakarta.servlet.*` — irrelevant if you don't touch servlet APIs directly through Javalin's routing DSL. |
| JJWT 0.13.0 | Java 8+ | Use the 3-artifact split (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) — the old single `jjwt` artifact is deprecated and will pull stale transitive deps. |
| React 19.2.x | Vite 8.x via `@vitejs/plugin-react` | Use the official `react-ts` Vite template; no manual wiring needed. |
| Vite 8.x | Node.js 20 LTS+ | Verify your Node version is current before scaffolding; Vite's Rolldown-based bundler (new in v8) drops support for old Node ranges. |
| PixiJS 8.19.0 (if adopted) | Modern evergreen browsers (WebGL/WebGPU) | No IE/legacy browser support needed for a course demo — non-issue here. |

## Sources

- [Javalin — official site](https://javalin.io/) — framework overview, WebSocket + REST unification, "no annotations, no magic" positioning (MEDIUM confidence, cross-verified across javalin.io, GitHub, and third-party write-ups)
- [Javalin 7.0 release notes](https://javalin.io/news/javalin-7.0.0-stable.html) and [migration guide v6→v7](https://javalin.io/migration-guide-javalin-6-to-7) — Java 17+/Jetty 12 requirement (HIGH confidence, official source)
- [Javalin WebSocket chat tutorial](https://javalin.io/tutorials/websocket-example) — WebSocket handler API shape
- [Java-WebSocket GitHub / Maven Central](https://github.com/TooTallNate/Java-WebSocket), [Maven Central listing](https://central.sonatype.com/artifact/org.java-websocket/Java-WebSocket) — version 1.6.0 (HIGH confidence, official package registry)
- [WebSocket.org Java guide](https://websocket.org/guides/languages/java/) — Java WebSocket ecosystem comparison, Spring Boot/JSR-356/Undertow notes (MEDIUM confidence)
- [React sunsetting Create React App (react.dev blog, Feb 2025)](https://react.dev/blog/2025/02/14/sunsetting-create-react-app) — official CRA deprecation, Vite recommended (HIGH confidence, official source)
- [React npm package page](https://www.npmjs.com/package/react) — version 19.2.x (HIGH confidence, official registry)
- [PixiJS npm package page](https://www.npmjs.com/package/pixi.js) and [PixiJS 8.19.0 blog](https://pixijs.com/blog/8.16.0) — version 8.19.0 (HIGH confidence, official registry/blog)
- [Konva "Best Canvas Library" guide](https://konvajs.org/docs/guides/best-canvas-library.html) and [Phaser vs PixiJS comparison (2025)](https://generalistprogrammer.com/tutorials/phaser-vs-pixijs-renderer-comparison) — Canvas/PixiJS/Phaser/Konva positioning (MEDIUM confidence, cross-verified across multiple independent write-ups)
- [H2 Database GitHub / Wikipedia](https://github.com/h2database/h2database) — version 2.4.240, embedded file-mode DB (MEDIUM-HIGH confidence)
- [JDBI official site](https://jdbi.org/) and [release notes](https://github.com/jdbi/jdbi/blob/master/RELEASE_NOTES.md) — version 3.54.0 (HIGH confidence, official source)
- [jBCrypt Maven Central](https://mvnrepository.com/artifact/org.mindrot/jbcrypt) — version 0.4 (MEDIUM confidence)
- [JJWT GitHub / Maven Central](https://github.com/jwtk/jjwt) — version 0.13.0, 3-artifact split (HIGH confidence, official source)
- [Spring Boot version history / HeroDevs EOL tracker](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026) — Spring Boot 4.1, Java 17+ requirement, used only for the alternative-stack path (MEDIUM confidence)
- [Java 21 vs Java 25 LTS analysis (Java Code Geeks, 2026)](https://www.javacodegeeks.com/2026/04/java-21-vs-java-25-lts-the-migration-decision-framework-teams-are-avoiding.html) — LTS adoption/support timeline (MEDIUM confidence)
- [Real-time game state sync patterns (springfuse.com)](https://www.springfuse.com/real-time-game-state/) and [dinogame.gg multiplayer WebSocket guide](https://dinogame.gg/blog/how-to-add-multiplayer-to-browser-game/) — tick-rate/delta-encoding/JSON-vs-binary patterns informing the "authoritative server, JSON snapshot broadcast" recommendation (MEDIUM confidence — general industry pattern, not project-benchmarked)
- Vite 8 release note ("Vite 8, released March 12, 2026, ships with Rolldown") via web search — used for build-tool version and Node baseline guidance (MEDIUM confidence; verify exact patch version at project start since Vite ships frequently)

---
*Stack research for: real-time 2D multiplayer tank battle game (Java + React + WebSocket)*
*Researched: 2026-08-13*
