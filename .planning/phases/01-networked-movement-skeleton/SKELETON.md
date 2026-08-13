# Walking Skeleton — Tank Battle Online

**Phase:** 1
**Generated:** 2026-08-13

## Capability Proven End-to-End

Two players in separate browser processes connect to the running Java server, each sees both tanks in a shared arena, and a movement key pressed in one window moves that tank live in the other — with every position computed and owned by the server.

The thinnest slice of that capability — a browser opening a real WebSocket to Javalin and being told which player it is — is delivered by the `tracer` task in plan `01-01` before anything else is built. Plans `01-02` through `01-04` expand outward from that proven path; they do not re-litigate its architecture.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Backend runtime | Java 21 LTS (OpenJDK 21.0.11, verified installed) | Clears Javalin 7's Java 17+ floor with headroom and is already on the machine — zero setup cost. |
| Backend framework | Javalin 7.2.3, registered via `Javalin.create(config -> config.routes.ws("/ws", ...))` | One process serves both the real-time WebSocket and the later REST routes for auth/leaderboard, so there is only ever one network stack to maintain. Its `onConnect`/`onMessage`/`onClose` surface maps almost 1:1 onto the raw socket lifecycle, which keeps the networking mechanics visible — the point of the course. The `config.routes.ws` form is mandatory: the pre-7 `app.ws(...)` shape every tutorial shows does not compile against 7.x. |
| JSON layer | `jackson-databind` 2.22.1, declared **explicitly** in `pom.xml` | Javalin's own POM marks this dependency optional, so Maven does not pull it in transitively. Omitting it compiles and starts cleanly, then fails at the first `ctx.send(object)` call. Pinning it explicitly is not optional. |
| Wire protocol | JSON text frames, one envelope: `{ "type": ..., ...payload }` | Three types exist this phase — `joined`, `state` (server→client) and `input` (client→server). Every future message kind (`chat`, `matchEnd`, `fire`) is a new `type` value dispatched by the same switch, never a protocol redesign. |
| Authority model | Server-authoritative. Clients send intent (`{"type":"input","direction"}`) only; the server broadcasts positions | The project's core value is a genuinely networked game, not a client mockup. `ClientMessage` deliberately has no positional or identity field, so there is nothing for a browser to lie about. Retrofitting authority after client-driven state exists means rewriting the protocol and all combat logic. |
| Game loop | One fixed-rate `ScheduledExecutorService` tick per `Match` at ~30 Hz (33ms), advancing positions by `speed * deltaTimeSeconds` | An independent loop is what makes bullets travel and idle tanks stop in later phases; delta-time is what keeps movement speed constant when tick timing jitters. Both are cheap to build correctly now and expensive to retrofit. |
| Concurrency model | Single-threaded executor **per match**. Both the tick task and every inbound frame submit their work to that same executor; `GameState` is touched by no other thread | Confinement rather than locking. A partly-locked, partly-confined design is how races survive local testing and appear only with two real simultaneous players. No lock primitives or concurrent collections exist anywhere under the match package, and a source gate keeps it that way. |
| Match identity | `Match` is an instantiable object owning its own executor and its own `GameState`, injected into the connection handler. No static mutable game state anywhere | Phase 4 will ask whether the server supports simultaneous matches. With `Match` already a real object, that becomes "hold a map of instances" — additive. As a static singleton it would be a rewrite. Costs nothing extra today. |
| Frontend | React 19.2.8 + TypeScript on Vite 8.2.1 (`react-ts` template), Canvas 2D API for rendering, no game engine | Two rectangles on a flat background do not justify Phaser's physics/scene machinery or PixiJS's dependency. TypeScript earns its keep on the client/server message contract specifically. |
| Client render loop | Server state lands in a `useRef`; a `requestAnimationFrame` loop reads that ref and draws the canvas. React never re-renders per network tick | A 30 Hz React re-render is the client-side anti-pattern this architecture is built to avoid. Connection status *does* use React state — it changes about four times per match, not thirty times per second. |
| Matchmaking | None. The first WebSocket connection becomes P1, the second P2, a third is closed with code 1013 | MATCH-01/02 is Phase 4. Building a queue now is the "generic infra instead of the game" trap; the roadmap deliberately front-loads the networking risk instead. |
| Persistence | None. All match state is in memory | Only completed match results need durability, and that arrives with the leaderboard in Phase 4. A server restart losing a live match is acceptable and expected. |
| Auth | None | AUTH-01/02/03 is Phase 4. The WebSocket handshake accepts any connection this phase, which is recorded as an accepted risk in the plans' threat models. |
| CORS | Not configured, deliberately | The WebSocket handshake is not subject to browser CORS — no preflight, no `Access-Control-Allow-Origin`. Vite on 5173 talking to Javalin on 7000 needs nothing. CORS becomes relevant only for the Phase 4 REST routes. Written into the README because the instinct to reach for a CORS fix on a failed socket wastes an afternoon. |
| Directory layout | `server/` (Maven module) and `client/` (Vite app) as siblings under a root Maven aggregator `pom.xml` | Sibling projects keep the two toolchains cleanly separated; the aggregator exists so `mvn test` and `mvn -pl server test` — the commands 01-VALIDATION.md contracts for — work from the repo root. |
| Local run | Two processes, documented in `README.md`: `mvn -pl server exec:java` and `npm --prefix client run dev` | There is no deployment target for a course project. The documented full-stack local run is the deliverable equivalent, and the README's acceptance recipe requires two *independent browser processes* with artificial latency — not two tabs on localhost. |

## Stack Touched in Phase 1

- [x] Project scaffold — Maven (Java 21, Javalin 7.2.3, Jackson 2.22.1, JUnit 5) and Vite (React 19.2.8, TypeScript)
- [x] Routing — one real route, the `/ws` WebSocket endpoint, registered the Javalin 7 way
- [ ] Database — deliberately none this phase; all state is in memory. First real read/write arrives with accounts and the leaderboard in Phase 4
- [x] Real read AND real write of live state — `GameState` is written by the tick loop and by every inbound input frame, and read by every broadcast, all serialized on one executor per match
- [x] UI — an interactive Canvas arena wired to the server: keyboard input goes out as intent, positions come back and are drawn
- [x] Deployment — no hosting target; a documented local full-stack run command plus a reproducible two-browser acceptance recipe stands in for it
- [x] Test harness — JUnit 5 plus a real headless WebSocket client, exercising the same socket path a browser uses

## Out of Scope (Deferred to Later Slices)

Explicit, so no later phase re-litigates Phase 1's minimalism:

- Walls, obstacles and any collision beyond arena-bounds clamping (COMBAT-05, Phase 2)
- Bullets, firing and hit detection (COMBAT-02, Phase 2); ricochet (COMBAT-06, Phase 2)
- HP, damage and a win condition (COMBAT-03, COMBAT-04, Phase 3)
- Disconnect resolution beyond "the slot is freed and the tick loop keeps running", and any result screen (LIFE-01, LIFE-02, Phase 3)
- Registration, login, session persistence (AUTH-01/02/03, Phase 4)
- A matchmaking queue, waiting/matched status beyond the local status line, real usernames (MATCH-01/02, Phase 4)
- Leaderboard and any persistence at all (LEADER-01, Phase 4)
- Practice bot (BOT-01, Phase 5) and in-match chat (CHAT-01, Phase 5)
- Client-side prediction, interpolation, reconciliation or lag compensation — explicitly out of scope project-wide per REQUIREMENTS.md
- Reconnecting into an in-progress match (REL-01, v2)
- Sound, particles, sprites, animation polish, multiple maps or tank types (POLISH-01..04, v2)
- Any "match already full" UX beyond closing the third connection

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering the architectural decisions above. In particular, none of them changes the message envelope, the authority model, the per-match executor confinement, or the ref-based render loop.

- **Phase 2** — Bullets fire, travel and ricochet off walls; tanks are blocked by walls. Adds `fire` input and bullet entries to the state payload; collision resolution runs inside the same tick.
- **Phase 3** — Hits cost HP, zero HP ends the match, disconnects resolve cleanly. Adds HP to the state payload and a `matchEnd` message type; `onClose` grows from freeing a slot to deciding an outcome.
- **Phase 4** — Accounts, matchmaking and a leaderboard. Introduces the first database work, token validation at the WebSocket handshake, and the singular-to-plural `Match` transition the identity model was already promoted for.
- **Phase 5** — Practice bot and in-match chat. The bot drives a `Match` slot through the same intent path a browser uses; chat is one more `type` on the existing envelope, routed around the tick loop so it cannot affect game timing.
