# Pitfalls Research

**Domain:** Real-time 1v1 multiplayer game — Java WebSocket server + React web client (university Network Programming course project)
**Researched:** 2026-08-13
**Confidence:** HIGH (architecture/networking pitfalls — well-documented pattern, e.g. Valve/Gambetta client-server netcode literature, Java WebSocket concurrency docs); MEDIUM (course-grading-specific claims — inferred from project constraints, not a cited source)

## Critical Pitfalls

### Pitfall 1: Client-authoritative game state ("trust the browser")

**What goes wrong:**
The server just relays whatever the client sends — client A reports "I moved to (x,y)" or "I hit player B for 20 damage" and the server stores it as truth. Since the client is a browser (React), any player can open devtools, edit a WebSocket frame, or call `send()` from the console to teleport, give themselves infinite HP, or one-shot the opponent. In a 1v1 competitive game this isn't a hypothetical — it's the single most likely thing a grader or the opponent will try.

**Why it happens:**
It's the fastest way to get movement/shooting working end-to-end, and early in development "does it move at all" matters more than "can it be cheated." Once messages like `{type: "move", x, y}` or `{type: "hit", damage}` are wired up and working, there's no natural forcing function to go back and add server validation — it already looks done.

**How to avoid:**
Decide the authority model on day one: the server is the single source of truth for position, HP, and match outcome. Client sends *inputs* (`{type:"input", up:true, fire:true}` or key/direction state), never *results* (`{type:"tookDamage"}`, `{type:"moved"}`). The server runs the simulation (movement, collision with walls, bullet-hit detection) and broadcasts authoritative state to both clients. The client only renders what the server says.

**Warning signs:**
- Any WebSocket message from client to server that describes an *outcome* (damage dealt, HP value, "I won") rather than an *intent* (move direction, fire pressed).
- Server code that copies a client-sent field directly into shared game state without validation.

**Phase to address:**
Foundational — must be decided in the architecture/networking-core phase, before movement or combat features are built. Retrofitting authority after client-driven state exists means rewriting the protocol.

---

### Pitfall 2: No server-side game loop — state only updates "when a message arrives"

**What goes wrong:**
The server is written as a pure message reactor: on `move` message, update position; on `fire` message, spawn a bullet and check collision immediately. There's no independent tick that advances bullets, checks wall collisions, or resolves hits between messages. Bullets don't move on their own, tanks with no recent input don't get position updates, and physics/collision only happens at the instant a client happens to send a packet — which is irregular and exploitable (a client can simply stop sending "hit" acknowledgements).

**Why it happens:**
Message-driven code is what WebSocket examples/tutorials teach (`onMessage` handler does the work), and it's the natural mental model coming from HTTP request/response. A continuous simulation loop is a different paradigm most students haven't written before.

**How to avoid:**
Run a fixed-rate game loop per match (e.g. a `ScheduledExecutorService` ticking every 50ms = 20Hz) that: advances bullet positions, checks wall/bullet/tank collisions, applies damage, checks win condition, and broadcasts the resulting state snapshot to both clients. Client input messages just update an "input state" object that the loop reads on its next tick — they don't mutate game state directly. This is the standard authoritative-server tick pattern (see Valve's Source multiplayer networking model).

**Warning signs:**
- Grep the server code for "broadcast" or "send" calls: if they only happen inside `onMessage` handlers and never on a timer, there's no independent loop.
- Bullets that only "exist" as a client-side animation with no corresponding server-tracked position.

**Phase to address:**
Core networking/game-state phase (movement + bullets). This is the architectural backbone everything else (combat, hit detection, win condition) depends on — get it right before building features on top of it.

---

### Pitfall 3: Frame-rate / tick-rate dependent movement (no delta-time)

**What goes wrong:**
Tank position is updated by a fixed increment per tick (`x += 5`) rather than `x += speed * deltaTime`. This looks fine in local testing (steady tick rate, one machine) but becomes inconsistent the moment tick timing jitters under load, or if movement math ever gets duplicated client-side for prediction — the two diverge because they assume different rates.

**Why it happens:**
Fixed-increment movement is simpler to write first and "looks right" in a smooth local demo, so the deltaTime plumbing (tracking elapsed time between ticks, multiplying velocity by it) gets skipped as unnecessary polish.

**How to avoid:**
Always compute movement as `position += velocity * deltaTimeSeconds` using the actual measured time since the last tick, even if the loop targets a fixed interval. Cheap insurance, costs nothing extra to build correctly the first time.

**Warning signs:**
- Movement speed constants used directly as per-tick position deltas with no time term.
- Game "speeds up" or "slows down" when server CPU load changes or when debugging with breakpoints.

**Phase to address:**
Same phase as the game loop (Pitfall 2) — implement together.

---

### Pitfall 4: Shared mutable game state accessed from multiple threads without synchronization

**What goes wrong:**
Java WebSocket servers (Tyrus/Jakarta WebSocket, Spring's `@ServerEndpoint`/STOMP, or a raw `Netty`/`java-websocket` setup) invoke `onMessage` on a thread per connection, while the game loop runs on its own scheduled thread. If both touch the same `Match`/`Tank` objects — one thread reading position while the loop writes it, or two `onMessage` calls for different players racing to update a `HashMap` of bullets — you get intermittent, hard-to-reproduce bugs: a tank frozen for one player but not the other, a bullet that only sometimes registers a hit, or a `ConcurrentModificationException` crash mid-match, often only under the specific timing of a live 2-client match (never reproduces when single-stepping in a debugger).

**Why it happens:**
Single-threaded local testing (or even one browser tab against the server) rarely triggers races. They show up when two real clients send input at truly concurrent times — exactly the demo scenario for grading — which makes this a "worked yesterday, crashes during the live demo" class of bug.

**How to avoid:**
Pick one concurrency strategy up front and apply it everywhere: either (a) funnel all state mutation through a single-threaded executor per match so the game loop and all incoming input updates are serialized on one thread (simplest to reason about, recommended for a solo/course-scale project), or (b) use `java.util.concurrent` structures (`ConcurrentHashMap`, `AtomicReference`, explicit locks) consistently around every read/write of shared match state. Don't mix "mostly single-threaded, except this one map."

**Warning signs:**
- Bugs that only appear with two real browser clients connected simultaneously, never in single-client testing.
- Any plain `HashMap`/`ArrayList`/POJO field shared between the WebSocket `onMessage` callback and the scheduled game-loop task without a lock or executor confinement.

**Phase to address:**
Core networking/game-state phase, alongside Pitfall 2. This is a foundational implementation decision, not something to patch later — retrofitting synchronization into already-written game logic is error-prone.

---

### Pitfall 5: No reconnection / disconnect handling — a dropped WebSocket ends the match badly

**What goes wrong:**
A player's laptop sleeps, WiFi blips, or they refresh the browser tab (common during development/demo). The WebSocket connection drops. With no handling, the server either: leaves the match in limbo forever (the remaining player waits with no feedback), throws an unhandled exception when trying to send to a closed session, or the disconnecting client simply can't rejoin and has to restart the whole flow (re-login, re-queue) losing match state.

**Why it happens:**
Happy-path development (both clients stay connected the whole time) never exercises this, and WebSocket "close" events are easy to ignore since nothing crashes immediately in dev — the failure surfaces later, often during the actual grading demo when something inevitably hiccups.

**How to avoid:**
Explicitly handle the WebSocket `onClose`/`onError` server-side: mark the match as abandoned or paused, notify the remaining player, and decide an explicit rule ("opponent disconnected = you win" is enough for course scope — a full reconnect-and-resume system is out of scope). On the client, detect the closed connection and show a clear "connection lost" state rather than a frozen UI.

**Warning signs:**
- No `onClose` handler in the server WebSocket endpoint, or one that's empty/just logs.
- Manually closing a browser tab mid-match during testing crashes the server or leaves the other client's UI hung with no message.

**Phase to address:**
After core game loop is working, before calling multiplayer "done" — should be an explicit checklist item in the real-time combat phase, since it's easy to skip entirely and still demo fine 9 times out of 10.

---

### Pitfall 6: Over-scoping the network layer — building generic infra instead of the game

**What goes wrong:**
Given "the course wants client-server networking," it's tempting to build a general-purpose message framework, plugin architecture, or reusable "engine" (generic entity-component system, custom serialization protocol, abstracted transport layer supporting both WebSocket and raw sockets "just in case") before a single tank can move on screen. Weeks into a several-week solo timeline, there's an elaborate networking abstraction and no playable game — and worse, it doesn't actually demonstrate client-server concepts better than a simple, readable protocol would; it just adds surface area to explain and debug.

**Why it happens:**
It's a course project, so there's pressure to "show sophistication," and generic/reusable code *feels* like better engineering. But grading and a working demo reward a clear, correct, explainable client-server flow — not architectural generality nobody asked for.

**How to avoid:**
Fix the transport (Java WebSocket server, one connection per player, JSON text frames) and message schema (a small fixed set: `join`, `input`, `state`, `chat`, `matchEnd`) early and treat it as settled, not as something to keep re-abstracting. Build the actual gameplay loop (Pitfall 2) in week one. Any "what if we needed UDP/raw sockets later" abstraction is explicitly out of scope per the project's own constraints (WebSocket is already the accepted answer) — don't build for a requirement that doesn't exist.

**Warning signs:**
- More time spent on message-framing/protocol-abstraction code than on the game loop or combat logic.
- A "network layer" package with interfaces/abstract classes for transports that will only ever have one implementation.

**Phase to address:**
Roadmap-level — the very first phase should produce a minimal but real client-server loop (one tank, one client, moves on screen via a WebSocket round-trip) before anything is generalized. Treat "two tanks moving and shooting" as the north-star early milestone, not "a networking framework."

---

### Pitfall 7: Testing only against yourself (two browser tabs, one machine, localhost)

**What goes wrong:**
Development and "it works" verification happens with two tabs of the same browser on `localhost`, which hides real-network issues: actual latency, packet reordering/jitter, two independent auth sessions, CORS/handshake quirks that only appear cross-origin, and timing bugs that need genuinely concurrent input from two separate processes to trigger (see Pitfall 4). The grading demo, however, likely involves two separate machines/browsers — exactly the scenario never tested.

**Why it happens:**
It's the fastest test loop during solo development, and `localhost`-to-`localhost` has ~0ms latency, so lag-related bugs simply don't exist in that environment — there's nothing pushing the developer to notice they're missing.

**How to avoid:**
At least once before the deadline, test with two separate browser processes (or two machines on the same network / a throttled connection via devtools network throttling) and deliberately introduce artificial latency (e.g. Chrome DevTools network throttling, or a temporary `Thread.sleep` in the send path) to see how the game behaves under lag. Confirm login/session handling actually works for two independent users, not two tabs sharing cookies/localStorage.

**Warning signs:**
- All manual testing notes reference "tab 1 / tab 2" rather than "player A's machine / player B's machine."
- Auth/session code that silently relies on shared browser storage (would break with two real users).

**Phase to address:**
Should be a UAT/verification step attached to the matchmaking and real-time combat phases — not a separate phase, but an explicit test-plan requirement on those phases.

---

### Pitfall 8: Mixing gameplay state and chat/meta messages without a clear message-type discipline

**What goes wrong:**
Chat messages, match state updates, and system events (player joined, match ended) all flow over the same WebSocket connection. Without a consistent `{type, payload}` envelope and a switch/dispatch on the client, handlers end up guessing at message shape (`if (msg.hp !== undefined)`), which breaks the moment a new message type is added — often silently misinterpreting one message type as another (e.g. a chat message with a `text` field colliding with a future field name used by game state).

**Why it happens:**
Chat feels like a small, separate feature bolted on late, so it's easy to reuse ad hoc JSON shapes instead of extending the existing message envelope.

**How to avoid:**
Define one envelope for every WebSocket message from the start: `{ "type": "input" | "state" | "chat" | "join" | "matchEnd" | ..., "payload": {...} }`, and dispatch on `type` on both ends. Adding chat is then just one more `type`, not a special case.

**Warning signs:**
- Client-side message handling with type-sniffing via `if (field in message)` instead of a `type` discriminator.
- Chat feature implemented on a second WebSocket connection or a separate HTTP polling endpoint instead of reusing the match connection.

**Phase to address:**
Define at the same time as the core networking protocol (Pitfall 6), before chat is built as a "late addition" feature.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|-----------------|-----------------|
| Client-authoritative hit detection (client tells server "I hit you") | Combat works in an afternoon | Trivially cheatable, undermines the "real client-server" grading point | Never for this project — server must own hit resolution |
| Single global game loop thread handling all matches serially | Simpler than per-match scheduling | Fine at 1v1/course scale (one match at a time is realistic); would not scale but scale isn't a goal here | Acceptable for this project's scope — don't over-engineer a per-match thread pool |
| Storing match/game state only in memory (no DB persistence of in-progress state) | Simpler, no schema for transient state | Server restart loses all live matches | Acceptable — only persist completed match results (for leaderboard), not live state |
| Polling instead of WebSocket push for state updates | Easier to reason about (just HTTP GET on a timer) | Adds latency, doesn't demonstrate real-time client-server networking (the actual point of the course) | Never — defeats the assignment's purpose |
| Hardcoding two player slots per match instead of a generic N-player room abstraction | Much simpler matchmaking and state model | Can't extend to >2 players later without rework | Acceptable and recommended — >2 players is explicitly out of scope |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|-----------------|-------------------|
| React dev server + Java WebSocket backend | Hardcoding `ws://localhost:PORT` in client code, breaking when deployed or run on another machine/port | Use an environment-configurable WebSocket URL (Vite/CRA env var), and confirm CORS/handshake origin settings on the Java WebSocket endpoint allow the dev server's origin |
| Auth (login) + WebSocket handshake | Building a separate auth mechanism for the WebSocket connection that doesn't share identity with the REST login/leaderboard endpoints | Issue a token (session id/JWT) at login via a normal HTTP endpoint, then pass it during the WebSocket handshake (query param or subprotocol header) and validate server-side before accepting the connection |
| JSON serialization (Jackson/Gson) of game state | Sending the entire domain object graph (including server-internal fields, thread/lock objects, or circular references) directly over the wire | Define explicit DTOs for outbound state snapshots — smaller payload, no accidental leakage of server internals, avoids serialization crashes on non-serializable fields |
| Leaderboard/account persistence (likely a relational DB) | Writing to the DB synchronously inside the hot game loop or WebSocket message handler (e.g. saving every position update) | Only persist on meaningful events (match end, account actions); keep the live game loop entirely in-memory |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|-----------------|
| Broadcasting full game state at high frequency with verbose JSON (field names, nested objects) every tick | Noticeable input lag or choppy rendering despite a fast server, especially over a real (non-localhost) network | Keep tick rate modest (10-20Hz is plenty for a tank game — not a twitch shooter), use compact field names/flat structures in the state payload | Becomes visible as soon as testing moves off `localhost` (Pitfall 7) — not a scale problem, a from-day-one problem for this project |
| Recomputing all collisions (bullets x walls x tanks) with naive nested loops every tick | Server tick time grows if more bullets/obstacles are added | For a 1v1 game with a handful of bullets and a small fixed obstacle map, naive O(n*m) collision checks are completely fine — do not build a spatial-partitioning system | Never breaks at this project's scale (a handful of entities); don't pre-optimize |
| Sending one WebSocket message per keypress/keyup event instead of a periodic "current input state" | Bursty, event-driven network traffic that's harder to reason about than the fixed-tick model in Pitfall 2 | Client sends current held-key state (or the loop samples it) on a fixed interval matching/aligned with the server tick, not on every DOM event | Shows up as jittery movement once real network latency (Pitfall 7) is introduced |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Trusting client-reported damage/HP/position (see Pitfall 1) | Trivial cheating via browser devtools console, undermines the core "fair 1v1 match" premise | Server-authoritative simulation; client sends inputs only |
| Storing passwords in plaintext or with weak hashing for the account system | Credential exposure if the DB is ever inspected/leaked — bad practice even for a course project and an easy, well-known fix | Use a standard password hashing library (e.g. BCrypt via Spring Security or a direct BCrypt library) — don't roll custom hashing |
| No validation that a WebSocket connection belongs to an authenticated, matched player before accepting `input` messages for that match | A stray/malicious connection could inject inputs into someone else's match | Validate identity + match membership at connection time (or on first message) and reject/ignore inputs from unauthorized sessions |
| Broadcasting the same full state object to both clients, including anything one player shouldn't see (rare in a tank game with full visibility, but worth checking if fog-of-war or hidden info is ever added) | Information leakage if scope later expands | Not a concern at current scope (full-visibility 1v1) — flag only if hidden-info mechanics are added later |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-------------------|
| No visual feedback while waiting for matchmaking/queue | Player thinks the app is frozen or broken | Show a clear "searching for opponent..." state with the queue actively polling/listening for a match-found event |
| No indication of connection quality/lag | Player can't tell if their shot missed due to lag or actually missed | Not required for course scope, but a simple "connected/reconnecting" indicator is cheap and prevents confused grading demos (ties to Pitfall 5) |
| Game freezes silently on opponent disconnect (Pitfall 5) instead of showing a clear outcome | Confusing, looks like a bug during a live demo | Explicit "opponent disconnected — you win" screen |
| Chat UI blocking or overlapping the game canvas | Player can't see the battlefield while typing | Keep chat as a clearly separate panel, not an overlay that intercepts game-relevant screen space or keyboard input (careful: chat input focus shouldn't also trigger tank movement keys) |

## "Looks Done But Isn't" Checklist

- [ ] **Movement/combat sync:** Looks done when it works in two tabs on localhost — verify with two separate browser processes/machines and artificial latency (Pitfall 7).
- [ ] **Hit detection:** Looks done when damage numbers change on screen — verify the *server*, not the client, is the one deciding a hit occurred and computing the damage (Pitfall 1).
- [ ] **Win condition:** Looks done when HP hits 0 and a "you win" message shows — verify it's broadcast identically and consistently to *both* clients from one authoritative server decision, not computed independently on each client (which can disagree).
- [ ] **Disconnect handling:** Looks done because it "never happened" in testing — verify by manually closing one client mid-match and observing the other client's and server's behavior (Pitfall 5).
- [ ] **Concurrent input from two players:** Looks done in solo single-player-style testing — verify by having two real inputs arrive at genuinely the same time (e.g. both players moving simultaneously) doesn't corrupt shared state (Pitfall 4).
- [ ] **Leaderboard/account data:** Looks done when one match updates one row — verify with multiple matches and both a win and a loss recorded correctly, and check what happens if a match ends abnormally (disconnect) — does it still record a result?

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|----------------|-----------------|
| Client-authoritative state discovered late (Pitfall 1) | HIGH | Requires redefining the message protocol (inputs vs. state) and moving all game-rule logic (movement validation, collision, damage) into the server loop — effectively a rewrite of the combat/movement phase. Do this before building chat/leaderboard on top, since those depend on match-end being trustworthy. |
| No game loop, purely message-reactive server (Pitfall 2) | MEDIUM-HIGH | Introduce a `ScheduledExecutorService` tick per match, move all "on message, immediately compute result and broadcast" logic into "on message, update input state" + "on tick, simulate and broadcast." Isolated to the server's match-handling class; client protocol can stay the same if the message envelope (Pitfall 8) is already in place. |
| Threading bugs discovered late (Pitfall 4) | MEDIUM | Retrofit by moving all match-state mutation onto a single-threaded executor per match (simplest fix) rather than trying to add fine-grained locks after the fact — confine, don't lock. |
| Missing disconnect handling (Pitfall 5) | LOW | Additive — add `onClose`/`onError` handlers and a client-side "connection lost" UI state without touching existing game logic. |
| Over-built generic network layer (Pitfall 6) | MEDIUM | Delete/inline the unnecessary abstraction layers; collapse down to the concrete WebSocket handler + fixed message schema. Painful mainly in lost time, not in code risk. |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|-------------------|----------------|
| Client-authoritative state (1) | Core networking / real-time movement phase (foundational) | Server rejects/ignores any client message that isn't an input intent; manually attempt to send a forged "state" message from devtools console and confirm the server ignores it |
| No independent game loop (2) | Core networking / real-time movement phase | Server continues advancing bullet positions and broadcasting state even if a client stops sending input messages |
| Missing delta-time (3) | Core networking / real-time movement phase (built alongside #2) | Artificially slow the tick loop (e.g. add temporary delay) and confirm movement speed stays visually consistent, not proportional to tick rate |
| Thread-safety of shared state (4) | Core networking / real-time movement phase | Stress test: both players spam movement/fire simultaneously for an extended period without a crash or desynced HP/position between the two clients' views |
| No reconnect/disconnect handling (5) | Real-time combat phase, before it's considered "done" | Manually close one client mid-match; confirm the other client gets a clear outcome and the server doesn't hang or crash |
| Over-scoped network layer (6) | Roadmap/first-phase framing | First phase produces one moving tank over a real WebSocket round-trip before any abstraction is added; roadmap explicitly sequences "playable loop" before "polish" |
| Untested against real two-client conditions (7) | Matchmaking + real-time combat phases (verification/UAT step) | Explicit UAT task: run with two separate browser processes/machines with throttled network before marking phase complete |
| No message-type discipline (8) | Core networking protocol definition, before chat is added | All WebSocket messages (game and chat) use one `{type, payload}` envelope; adding chat required zero changes to existing message dispatch code |

## Sources

- [Fast-Paced Multiplayer (Gabriel Gambetta) — client-side prediction & server reconciliation](https://www.gabrielgambetta.com/client-side-prediction-server-reconciliation.html) — HIGH confidence, canonical reference for client-server netcode patterns
- [Fast-Paced Multiplayer Part IV: Lag Compensation (Gabriel Gambetta)](https://www.gabrielgambetta.com/lag-compensation.html) — HIGH confidence
- [Valve Developer Community — Latency Compensating Methods in Client/Server In-game Protocol Design](https://developer.valvesoftware.com/wiki/Latency_Compensating_Methods_in_Client/Server_In-game_Protocol_Design_and_Optimization) — HIGH confidence, industry-standard authoritative-server reference
- [Ably — Java and WebSockets: Building dependable realtime apps](https://ably.com/topic/websockets-java) — MEDIUM confidence, general Java WebSocket reliability/reconnection guidance
- [Baeldung — A Guide to the Java API for WebSocket](https://www.baeldung.com/java-websockets) — MEDIUM confidence, Java WebSocket API mechanics
- [Wikipedia — Scope creep](https://en.wikipedia.org/wiki/Scope_creep) — general project-management reference for the over-scoping pitfall
- Course/project constraints (`.planning/PROJECT.md`): WebSocket acceptable in place of raw sockets, 1v1 only, solo/several-week timeline — used to derive course-project-specific pitfalls (over-scoping, testing-in-isolation) — MEDIUM confidence (inferred, not externally sourced)

---
*Pitfalls research for: Real-time 1v1 tank battle game (Java WebSocket server + React client)*
*Researched: 2026-08-13*
