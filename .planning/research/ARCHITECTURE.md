# Architecture Research

**Domain:** Real-time 1v1 multiplayer 2D game (client-server), Java backend + React web frontend
**Researched:** 2026-08-13
**Confidence:** MEDIUM (well-established, cross-corroborated patterns; not yet validated against this specific course's grading rubric)

## Standard Architecture

### System Overview

```
┌───────────────────────────────────────────────────────────────────────┐
│                          CLIENT (React Web App)                       │
├───────────────────────────────────────────────────────────────────────┤
│  ┌───────────┐   ┌────────────────┐   ┌───────────┐   ┌────────────┐ │
│  │ Auth/Lobby│   │ Input Capture  │   │  Renderer │   │ Chat Panel │ │
│  │  Screens  │   │ (keydown/up,   │   │ (Canvas + │   │            │ │
│  │  (React   │   │  click-to-fire)│   │  rAF loop)│   │            │ │
│  │  Router)  │   └───────┬────────┘   └─────▲─────┘   └─────▲──────┘ │
│  └─────┬─────┘           │                  │               │        │
│        │                 ▼                  │               │        │
│        │        ┌─────────────────────────────────────────┐          │
│        └───────▶│         Network Client (WebSocket)        │◀────────┘
│                 │  - sends: input, join, chat                │
│                 │  - receives: state snapshot, match events   │
│                 └───────────────────┬─────────────────────┘
└─────────────────────────────────────┼─────────────────────────────────┘
                                       │  WebSocket (JSON or binary frames)
┌─────────────────────────────────────┼─────────────────────────────────┐
│                                      ▼          SERVER (Java)         │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │                  Connection / Session Layer                    │   │
│  │  (WebSocket endpoint, auth token check, per-connection session)│   │
│  └───────────────────────┬─────────────────────────────────────┘   │
│                           │                                          │
│  ┌────────────────────────▼───────────────┐   ┌────────────────────┐│
│  │      Matchmaking / Room Manager         │   │   Chat Router      ││
│  │  (queue, pair 2 players, create Match)  │   │ (relay within room)││
│  └────────────────────────┬───────────────┘   └────────────────────┘│
│                           │                                          │
│  ┌────────────────────────▼───────────────────────────────────────┐ │
│  │              Match / Game Loop (per active match)                │ │
│  │  - fixed-tick simulation (movement, collision, shooting, HP)     │ │
│  │  - validates player input, updates authoritative GameState       │ │
│  │  - broadcasts state snapshot to both clients each tick           │ │
│  └────────────────────────┬───────────────────────────────────────┘ │
│                           │                                          │
│  ┌────────────────────────▼───────────────────────────────────────┐ │
│  │                    Persistence Layer                             │ │
│  │  (accounts, credentials, match results, leaderboard — DB/JPA)    │ │
│  └───────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Connection/Session Layer | Accept WebSocket connections, authenticate (token/session from login), map connection ↔ player identity, detect disconnects | Java-WebSocket `WebSocketServer` subclass or Spring `@ServerEndpoint`/`WebSocketHandler`; one handler instance per connection, session map keyed by connection ID |
| Matchmaking/Room Manager | Queue waiting players, pair exactly 2 into a Match, create/destroy Match instances, route players into their match's message channel | In-memory `ConcurrentLinkedQueue` + a `Map<matchId, Match>`; simple FIFO pairing is enough for v1 (no skill-based matching per PROJECT.md scope) |
| Match / Game Loop | Own the authoritative `GameState` for one match (tank positions, HP, bullets, walls); run a fixed-tick loop (e.g. `ScheduledExecutorService` at ~20-30 Hz); apply queued player inputs; run physics/collision against map obstacles; detect win condition (HP=0); broadcast state each tick | One thread/task per match via `ScheduledExecutorService`; state is a plain Java object mutated only inside the tick — never touched directly by connection threads |
| Persistence Layer | Store accounts (hashed passwords), match history, win/loss records, leaderboard aggregates | Relational DB (PostgreSQL/MySQL/H2) via JPA/Hibernate or plain JDBC; reads/writes happen outside the hot game loop (after match ends, at login) |
| Chat Router | Relay chat messages between the two players in a match (and lobby chat if any) | Piggybacks on the same WebSocket connection/session, routed by matchId — no separate service needed at this scale |
| Network Client (React) | Own the single WebSocket connection; serialize/deserialize messages; expose a small pub/sub or callback API to the rest of the app | Thin class/hook wrapping native `WebSocket`, or `socket.io-client`/`sockjs-client` if using a higher-level protocol on the server |
| Input Capture | Capture keyboard (move) and mouse/click (aim/shoot) events, translate into input messages sent to server at a bounded rate | `keydown`/`keyup` listeners set movement-intent flags sampled on an interval (avoid sending event-per-keystroke); throttle to ~20-30 msgs/sec |
| Renderer | Draw the current authoritative (or interpolated) state each frame; purely reactive to state received from server | HTML5 `<canvas>` + `requestAnimationFrame` loop; state stored in a ref/external store, not React state, to avoid re-render overhead per frame |
| Auth/Lobby Screens | Login/register forms, "find match" button, post-match leaderboard view | Regular React Router pages, REST calls (not WebSocket) for login/register/leaderboard |

## Recommended Project Structure

```
server/                          # Java backend
├── src/main/java/.../
│   ├── connection/               # WebSocket endpoint, session registry
│   │   ├── GameWebSocketHandler.java
│   │   └── SessionRegistry.java
│   ├── matchmaking/               # Queue + room/match lifecycle
│   │   ├── Matchmaker.java
│   │   └── MatchRegistry.java
│   ├── match/                     # Core game simulation (the authoritative part)
│   │   ├── Match.java             # holds GameState, runs tick loop
│   │   ├── GameState.java         # tanks, bullets, map, HP
│   │   ├── GameLoop.java          # ScheduledExecutorService wrapper
│   │   ├── InputHandler.java      # validates & applies player input
│   │   └── CollisionSystem.java   # wall/bullet/tank collision
│   ├── protocol/                  # Message DTOs + (de)serialization
│   │   ├── ClientMessage.java     # input, join, chat
│   │   └── ServerMessage.java     # stateUpdate, matchStart, matchEnd, chat
│   ├── persistence/                # Accounts, match history, leaderboard
│   │   ├── AccountRepository.java
│   │   ├── MatchResultRepository.java
│   │   └── entity/
│   └── auth/                       # Login/register (REST, not WS)
│       └── AuthController.java
└── src/test/java/...

client/                          # React frontend
├── src/
│   ├── network/                   # WebSocket client, message types
│   │   ├── GameSocket.ts          # connect/send/receive wrapper
│   │   └── messages.ts            # shared message shape (mirrors server DTOs)
│   ├── game/                      # Rendering + local game loop
│   │   ├── GameCanvas.tsx         # <canvas> + requestAnimationFrame
│   │   ├── renderState.ts         # external mutable store for latest state
│   │   └── input.ts               # keydown/up capture, throttled send
│   ├── pages/                     # Route-level screens
│   │   ├── LoginPage.tsx
│   │   ├── LobbyPage.tsx          # "find match" / queue status
│   │   ├── MatchPage.tsx          # hosts GameCanvas + chat + HUD
│   │   └── LeaderboardPage.tsx
│   ├── components/                # HUD, HP bars, chat panel, etc.
│   └── api/                       # REST calls for auth/leaderboard
└── ...
```

### Structure Rationale

- **`match/` is the heart of the server:** isolating authoritative simulation from `connection/` and `matchmaking/` means the game-rules code can be unit-tested without spinning up WebSockets — important for a course project where correctness of hit detection/collision is gradeable.
- **`protocol/` as a shared contract:** defining message DTOs explicitly (rather than ad-hoc JSON) keeps client and server message shapes from drifting; mirror the same shape in the TypeScript `messages.ts`.
- **`network/` on the client is a thin seam:** keeping WebSocket logic out of React components lets rendering and input code stay framework-agnostic and testable, and makes swapping raw `WebSocket` for a library (e.g. socket.io) a localized change.
- **State outside React re-render cycle:** `renderState.ts` as a plain mutable object (not `useState`) avoids triggering a React re-render on every network tick (20-30/sec) — the canvas draws directly from it inside `requestAnimationFrame`.

## Architectural Patterns

### Pattern 1: Authoritative Server with Fixed-Tick Game Loop

**What:** The server owns the single source of truth `GameState` per match. It runs a loop at a fixed rate (e.g. every 33-50ms = 20-30 Hz), during each tick: (1) drain queued player inputs, (2) apply movement/physics/collision, (3) check win condition, (4) broadcast the resulting state to both clients.
**When to use:** Any competitive real-time game where cheating/desync must be prevented — client input is a *request*, never directly mutates shared state.
**Trade-offs:** Simpler and safer than distributing authority, but adds latency (client waits for server round-trip to see effects) unless paired with client-side prediction. For a 1v1 LAN/localhost-latency course demo, the added latency is usually imperceptible, so this can be the whole solution — no need for prediction/reconciliation.

**Example:**
```java
// Match.java — one instance per active match
public class Match {
    private final GameState state = new GameState();
    private final Queue<PlayerInput> pendingInputs = new ConcurrentLinkedQueue<>();

    void onPlayerInput(PlayerInput input) { pendingInputs.add(input); }

    void tick() { // called every ~33ms by a ScheduledExecutorService
        drainInputs(pendingInputs, state);
        physicsStep(state);          // move tanks, bullets; resolve wall/tank collisions
        checkWinCondition(state);    // HP <= 0 -> end match
        broadcast(state.snapshot()); // send to both connected sessions
    }
}
```

### Pattern 2: Server-Authoritative State, "Dumb" Client Renderer

**What:** The React client never computes gameplay outcomes locally (no local hit detection deciding truth). It renders whatever state the server last sent, and sends raw input intents (move-left-pressed, fire) rather than computed positions.
**When to use:** Default for any authoritative-server game; essential to prevent client-side cheating and keeps client code simple.
**Trade-offs:** Feels slightly less snappy than a client that predicts its own movement, but is far simpler to build correctly — a good fit for a several-week solo course project. Client-side prediction (Pattern 3) can be added later if time permits, without changing the server.

**Example:**
```ts
// input.ts — client only sends intent, never mutates game state
function sendMoveIntent(socket: GameSocket, keys: KeyState) {
  socket.send({ type: "input", up: keys.up, down: keys.down, left: keys.left, right: keys.right, firing: keys.space });
}
```

### Pattern 3: Client-Side Prediction + Server Reconciliation (optional stretch)

**What:** Client applies its own input locally immediately (predicts movement) while also sending it to the server; when the server's authoritative snapshot arrives, the client reconciles by snapping/interpolating to the corrected position, replaying any inputs the server hasn't yet acknowledged.
**When to use:** Only if local testing reveals movement feels laggy (noticeable over real network, not localhost) and there's time budget left — this is a well-known "nice to have," not required for a working demo.
**Trade-offs:** Meaningfully more complex (need input sequence numbers, replay logic, reconciliation tolerance); for a 1v1 course project on likely low-latency networks (same LAN/localhost for grading), the complexity is usually not worth it for v1. Treat as a stretch goal, not baseline architecture.

## Data Flow

### Real-Time Match Flow (movement/shoot → state update)

```
[Player presses arrow key]
    ↓ (keydown/keyup listener, throttled to ~20-30/sec)
[Input Capture] → [Network Client: send {type:"input", ...}]
    ↓  WebSocket frame
[Server: Connection Layer] → resolves session → [Match.onPlayerInput()]
    ↓ (queued, applied on next tick — not immediately)
[Game Loop tick] → validates input → updates GameState
    (movement bounded by walls, bullets spawned/moved, collisions resolved, HP adjusted)
    ↓
[Match.broadcast(snapshot)] → sent to BOTH player connections
    ↓ WebSocket frame (to both clients, including the one who sent input)
[Client: Network Client] → updates renderState (external store)
    ↓ (next requestAnimationFrame tick)
[Renderer] draws tanks/bullets/HP bars from renderState
```

Key point: **the client that sent the input does not render it optimistically by default** (Pattern 2) — it waits for the server's next broadcast, same as the opponent. This keeps both clients' views trivially consistent.

### Match Lifecycle Flow

```
[Client A] --REST login--> [Auth] --success--> [Client A] --WS connect + join queue--> [Server]
[Client B] --REST login--> [Auth] --success--> [Client B] --WS connect + join queue--> [Server]
[Matchmaker] pairs A+B --> creates [Match] --> sends {type:"matchStart", matchId, mapData} to both
... real-time gameplay loop (see above) ...
[Match] detects HP=0 --> sends {type:"matchEnd", winner} to both
    ↓
[Persistence Layer] writes MatchResult (winner, loser, timestamp)
    ↓
[Client] --REST--> [Leaderboard] reads aggregated win/loss from Persistence
```

### Chat Flow

```
[Client A types message] → [Network Client: send {type:"chat", text}]
    ↓
[Server: Connection Layer] resolves session's matchId
    ↓
[Chat Router] relays to the OTHER connection in the same match
    ↓
[Client B: Network Client] receives {type:"chat", from, text} → appends to chat panel
```
Chat does not touch `GameState` — it is routed independently of the tick loop, so it can be relayed immediately on receipt rather than waiting for the next tick.

## Scaling Considerations

This is a course project graded on a working demo, not a production system — scale is a non-goal. Still, worth noting what's "enough" vs overkill:

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Demo / grading (a handful of concurrent matches) | Single Java process, in-memory `Map<matchId, Match>`, one `ScheduledExecutorService` (or one thread per match) — no need for a message broker, no need for horizontal scaling. This is the target scope. |
| A few dozen concurrent matches | Still fine as a single process; make sure each match's tick loop is lightweight (avoid blocking I/O inside the tick — persistence writes happen only at match end, off the hot path) |
| Hundreds+ concurrent matches (out of scope here) | Would need to shard matches across processes/instances and move matchmaking state to a shared store (Redis) — not relevant for this project |

### Scaling Priorities (informational only, not required for v1)

1. **First thing that would break:** if persistence (DB writes) happens synchronously inside the game tick loop, it would stall ticks under any load — always keep persistence off the tick path.
2. **Second:** unbounded broadcast of full state every tick to many matches could bottleneck a single-threaded network layer — not a concern at 1v1/handful-of-matches scale.

## Anti-Patterns

### Anti-Pattern 1: Trusting Client-Reported State

**What people do:** Let the client compute its own position/HP/hit results and just send the client's "final" state to the server to relay to the opponent.
**Why it's wrong:** Trivially cheatable (a modified client can claim infinite HP or teleport), and the two clients' views can desync since neither is authoritative. Also undermines the "network programming" learning goal — the interesting part is server-side simulation.
**Do this instead:** Client sends only input *intent*; server computes all resulting state (Pattern 1/2 above).

### Anti-Pattern 2: One WebSocket Message = One React State Update via setState on Every Tick

**What people do:** Call `setState`/`useState` on every incoming state-update message (20-30 times/sec) to trigger a re-render for rendering.
**Why it's wrong:** React re-renders the component tree on every state-update message, causing jank and unnecessary CPU/GC pressure — especially noticeable on a 2D canvas game where you want a tight, decoupled render loop.
**Do this instead:** Store the latest server state in a ref or external mutable store (not React state); drive the canvas draw calls from `requestAnimationFrame`, reading from that store. Only use React state for things that change rarely (score, match-end screen, connection status).

### Anti-Pattern 3: Doing Game Logic Inside the WebSocket Message Handler

**What people do:** Put movement/collision/damage logic directly inside the `onMessage` callback of the WebSocket handler, coupling network I/O with game simulation.
**Why it's wrong:** Makes game logic hard to unit test (requires a live socket), and ties simulation timing to whenever messages happen to arrive rather than a controlled tick — leads to race conditions when both players' inputs arrive close together.
**Do this instead:** `onMessage` only enqueues a validated input into the match's pending-input queue; the actual simulation runs on the fixed-tick `GameLoop`, decoupled from message arrival timing (Pattern 1).

### Anti-Pattern 4: Sending Raw Keyboard Events at Full Browser Event Rate

**What people do:** Send a network message on every single `keydown`/`keyup`/`mousemove` event uncapped.
**Why it's wrong:** Floods the WebSocket connection and the server's input queue with far more messages than the tick rate can even use, wasting bandwidth and CPU for no gameplay benefit.
**Do this instead:** Sample input state on an interval (e.g. every 33-50ms, matching or slightly exceeding server tick rate) and send the current intent (which keys are held), not every raw event.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Database (PostgreSQL/MySQL/H2) | JPA/Hibernate repositories called from `persistence/` layer, invoked at login, match-end, and leaderboard queries | Never called from inside the game tick loop; H2 (embedded) is a fine low-friction choice for a course project, PostgreSQL if wanting more "real" ops experience |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Connection Layer ↔ Matchmaker | Direct method call (in-process) — session joins/leaves queue | Both live in the same JVM process; no need for messaging middleware at this scale |
| Matchmaker ↔ Match | Matchmaker constructs `Match` and registers session→match mapping; after creation, Matchmaker's job for that pair is done | Match becomes independently responsible for its own tick loop and broadcasts |
| Connection Layer ↔ Match | `onMessage` looks up the session's current `Match` and forwards validated input via method call | Keeps network parsing separate from simulation (Anti-Pattern 3) |
| Match ↔ Persistence | Match calls persistence only once, at match end, to record the result | Keeps DB I/O off the hot tick path |
| React Network Client ↔ Rendering/Input | Pub/sub or callback registration (`onMessage(type, handler)`) so `GameCanvas` and `ChatPanel` each subscribe to only the message types they care about | Decouples rendering code from raw socket handling |
| React client ↔ Server (Auth/Leaderboard) | Plain REST/HTTP (not WebSocket) for login, register, leaderboard fetch | These are request/response, not real-time — no reason to route them over the game socket; simpler to test and cache |

## Suggested Build Order (dependency-driven)

1. **Basic WebSocket connection** (client connects, server accepts, echoes a message) — proves the network transport works before anything else.
2. **Session/connection layer + simple 2-player room** (no matchmaking queue yet, just a hardcoded pairing or a manual "join room X") — proves two distinct clients can be addressed independently.
3. **Server-side game loop skeleton** (fixed-tick loop running, broadcasting a static/dummy state) — proves the tick/broadcast mechanism before real physics exist.
4. **Input → movement sync** (client sends move input, server updates tank position each tick, broadcasts, client renders) — the core real-time loop end-to-end, still no combat.
5. **Collision with walls/obstacles** — adds map-aware movement validation.
6. **Shooting + HP + win condition** — the core game-rules layer, built once movement/collision are solid.
7. **Matchmaking/queue** (replace hardcoded pairing with real queue-based matching) — safe to defer until the core match loop already works with 2 manually-paired clients.
8. **Accounts + auth (register/login)** — can be built in parallel with the above once basic client/server exist, since it's mostly independent (REST, not WS).
9. **Persistence for match results + leaderboard** — depends on match-end events existing (step 6) and accounts existing (step 8).
10. **Chat** — low-risk, can be added any time after step 2 (session/room exists) since it doesn't depend on game-loop internals.

This order front-loads the riskiest/most novel part (real-time authoritative sync) and defers the more standard, well-understood parts (auth, persistence, leaderboard) to later phases — consistent with tackling technical risk before routine CRUD work.

## Sources

- [Scalable Architecture for Real-Time Multiplayer Games (Differ)](https://differ.blog/p/software-architecture-for-high-load-real-time-multiplayer-games-ff6066) — MEDIUM confidence
- [What are Server-Authoritative Realtime Games? (Metaplay)](https://www.metaplay.io/blog/server-authoritative-games) — MEDIUM confidence
- [Authoritative Multiplayer (Heroic Labs / Nakama docs)](https://heroiclabs.com/docs/nakama/concepts/multiplayer/authoritative/) — MEDIUM confidence
- [Designing Real-Time Game State Sync Services with Spring Boot and WebSockets](https://www.springfuse.com/real-time-game-state-2/) — MEDIUM confidence
- [Java WebSocket Guide: Spring Boot, Virtual Threads (WebSocket.org)](https://websocket.org/guides/languages/java/) — MEDIUM confidence
- [Integrating Java Web Sockets with Spring Framework (Medium)](https://medium.com/@AlexanderObregon/integrating-java-web-sockets-with-spring-framework-b142de738ac2) — MEDIUM confidence
- [Client-Side Prediction and Server Reconciliation — Gabriel Gambetta (canonical reference)](https://www.gabrielgambetta.com/client-side-prediction-server-reconciliation.html) — MEDIUM confidence
- [Fast-Paced Multiplayer: Sample Code and Live Demo — Gabriel Gambetta](https://www.gabrielgambetta.com/client-side-prediction-live-demo.html) — MEDIUM confidence
- [Multiplayer game using React and Websocket](https://www.mikaeleliasson.se/multiplayer-game-using-websocket) — MEDIUM confidence
- [Building Real-time Multiplayer Games With Websockets](https://peerdh.com/blogs/programming-insights/building-real-time-multiplayer-games-with-websockets) — MEDIUM confidence
- [WebSocket Multiplayer Game Tutorial: Node.js & Canvas (codezup)](https://codezup.com/create-multiplayer-game-webssockets-javascript-guide/) — MEDIUM confidence

---
*Architecture research for: real-time 1v1 multiplayer tank battle (Java server + React client)*
*Researched: 2026-08-13*
