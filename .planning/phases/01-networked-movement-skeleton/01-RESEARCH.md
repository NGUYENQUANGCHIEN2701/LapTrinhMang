# Phase 1: Networked Movement Skeleton - Research

**Researched:** 2026-08-13
**Domain:** Java WebSocket server (Javalin 7) + React/Vite Canvas client — thin end-to-end movement-sync slice
**Confidence:** MEDIUM-HIGH (Javalin API and package versions verified directly against Maven Central and official docs this session; concurrency/render-loop patterns are well-established industry patterns cross-checked but not project-benchmarked)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Classic Battle City-style controls — pressing a direction (WASD/arrow keys) both rotates the tank body to face that direction AND moves it that way. No independent turret aim; firing (future phase) will always go in the direction the tank body currently faces. — **Reversibility:** one-way — switching to twin-stick later means adding a second synced angle (turret) to the network payload and rewriting client input handling and rendering; treat this as locked for the whole project, not just this phase.
- **D-02:** 4-directional movement/facing only (up/down/left/right) — no diagonal/8-direction movement.
- **D-03:** Free, continuous pixel-by-pixel movement (not grid-snapped) — the tank slides smoothly along continuous coordinates. This is also the simpler fit for continuous-position network sync (vs. discrete tile-index sync).
- **D-04:** Tank speed and bullet speed values are Claude's discretion — pick numbers that feel like a classic tank game (bullets travel noticeably faster than tanks), tune after playtesting. Not user-specified.
- **D-05:** Small-to-medium arena that fits entirely on one screen (~800x600px scale) — no camera scrolling/following. The whole arena is visible at all times.
- **D-06:** The two tanks spawn at opposite corners of the arena (diagonal from each other) at match start.

### Claude's Discretion

- Exact tank/bullet speed numbers (D-04).
- Exact arena pixel dimensions within the "fits on one screen, no scrolling" constraint (D-05).
- Visual placeholder style for tanks/arena at this skeleton stage (simple shapes are expected — no shooting/walls/art requirements yet).
- Message protocol envelope shape, tick rate (~20-30Hz per research), and concurrency model (single-threaded executor per match, per research) — these are already recommended in research/STACK.md and research/ARCHITECTURE.md; treat as the default unless research-phase during planning surfaces a reason to deviate.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope. Walls/collision, shooting/ricochet, HP, and accounts are already scoped to Phases 2-4 per ROADMAP.md; not re-discussed here.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|--------------------|
| COMBAT-01 | Player can move their tank in real time; movement is visible live to the opponent | Verified Javalin 7 `config.routes.ws(...)` WS endpoint syntax (Pattern 1), per-tick broadcast to both sessions (Pattern 2), server-authoritative delta-time movement in the tick loop (Pattern 3), and the client ref+`requestAnimationFrame` render loop (Pattern 4) together implement all four of this phase's success criteria: live cross-browser movement sync, server-owned position (rejecting forged client state), and drift/crash-free behavior under concurrent input (see Concurrency Stress-Test Approach). |
</phase_requirements>

## Summary

This phase's job is narrow but foundational: get one real `ws()` round trip working end-to-end (client sends movement intent, server owns position, both browsers render the same state), with the three "expensive to retrofit" decisions locked correctly the first time — message envelope, server authority, and per-match concurrency confinement.

Two things discovered this session materially change what was assumed at the project level. First, **Javalin 7's WebSocket registration syntax differs from the `app.ws()` shape used in Javalin 5/6 tutorials** — Javalin 7 requires `config.routes.ws(...)` declared inside the `Javalin.create(config -> ...)` block, not `app.ws(...)` after creation. Second, **`jackson-databind` is an `optional=true` dependency of `io.javalin:javalin` in the published POM** — Javalin does not transitively pull it in; the project's own `pom.xml` must declare `jackson-databind` explicitly or `ctx.send(obj)` / `ctx.messageAsClass(...)` will throw at runtime with no JSON mapper found. Both are concrete, verified corrections to the project-level STACK.md.

Also verified: **WebSocket connections are not subject to browser CORS.** There is no preflight, no `Access-Control-Allow-Origin` requirement, and no SOP enforcement on the WS handshake itself — so a Vite dev server on port 5173 opening `new WebSocket('ws://localhost:7000/ws')` against the Javalin server works with zero CORS configuration. This simplifies the dev-run setup for this phase considerably; CORS only matters later for REST endpoints (auth, leaderboard — out of scope this phase).

**Primary recommendation:** Scaffold Maven (Java 21, Javalin 7.2.3 + explicit `jackson-databind` 2.22.1) and Vite (`react-ts` template, React 19.2.8) as sibling directories; wire one `config.routes.ws("/ws", ...)` endpoint that assigns the first two connections to a single hardcoded in-memory match, runs a `Executors.newSingleThreadScheduledExecutor()` tick loop at 30Hz per match, and funnels all `onMessage` input through `executor.execute(...)` onto that same thread — no explicit locks needed. Client is a thin `WebSocket` wrapper writing into a ref, and a `<canvas>` component driven by `requestAnimationFrame` reading that ref, never `useState` per tick.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Tank position truth (x, y, facing) | API/Backend (Match tick loop) | — | Server-authoritative per locked decision; client never computes its own position |
| Movement input capture (keydown/keyup) | Browser/Client | — | Only the browser can read keyboard events; translated to intent, not position |
| Position rendering | Browser/Client (Canvas + rAF) | — | Purely reactive draw from last-received server state; no game logic here |
| WebSocket connection lifecycle | API/Backend (Connection layer) | Browser/Client (native `WebSocket`) | Server owns session↔player mapping; client owns reconnect-free single connection for this phase |
| Match/tick concurrency confinement | API/Backend (single-threaded executor per match) | — | Must be enforced entirely server-side; client has no visibility into it |
| Dev environment wiring (ports, process startup) | N/A (tooling) | — | Not an application tier; documented separately below |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|---------------|
| Java (JDK) | 21 LTS (confirmed installed: OpenJDK 21.0.11) [VERIFIED: local `java --version`] | Backend runtime | Meets Javalin 7's Java 17+ minimum with headroom; already present on this machine. |
| Javalin | **7.2.3** [VERIFIED: Maven Central `maven-metadata.xml`, `lastUpdated 20260811193747`] | WebSocket (`config.routes.ws`) + future REST routes in one process | Latest stable as of 2026-08-13; supersedes the `7.0.x` line named in project STACK.md — use 7.2.3, not 7.0.x, when scaffolding `pom.xml`. |
| `jackson-databind` | **2.22.1** [VERIFIED: Maven Central `maven-metadata.xml`] | JSON (de)serialization for WS messages | **Must be added explicitly to `pom.xml`** — Javalin 7's own POM marks this dependency `<optional>true</optional>`, so it is NOT pulled in transitively. Omitting it causes a runtime failure the first time `ctx.send(someObject)` or `ctx.messageAsClass(...)` is called. [VERIFIED: `javalin-7.2.3.pom` inspected directly via `curl` this session] |
| React | **19.2.8** [VERIFIED: `npm view react version`] | Frontend UI | Matches project-level STACK.md recommendation, exact patch confirmed current. |
| Vite | **8.2.1** [VERIFIED: `npm view vite version`] | Dev server / build | Requires Node `^20.19.0 \|\| >=22.12.0` [VERIFIED: `npm view vite engines`] — local Node 20.20.2 satisfies this. |
| `create-vite` | 9.1.2 [VERIFIED: `npm view create-vite version`] | Scaffolding CLI | `npm create vite@latest client -- --template react-ts` |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `jetty-websocket-jetty-client` (test scope) | matches Javalin's bundled Jetty 12 line [CITED: javalin-7.2.3.pom, `org.eclipse.jetty.websocket:jetty-websocket-jetty-client`, scope=test] | Headless WS client for automated concurrency/stress testing | Add as a test-only Maven dependency to script a "two clients hammering input" test without needing a real browser (see Concurrency Stress-Test section). |
| Playwright (Node, optional) | current | Two-real-browser-process style automated UAT | Two independent `BrowserContext`s (not two tabs) give isolated storage close to "two independent browser processes" and can be scripted to mash keys concurrently — useful for repeatable success-criterion-4 verification. [CITED: playwright.dev/docs/browser-contexts] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `config.routes.ws(...)` (Javalin 7 native) | Raw `org.java-websocket:Java-WebSocket` | Already rejected at project level (STACK.md) — Javalin's own WS API is close enough to the raw socket lifecycle for the course's networking-transparency goal; no reason to deviate for this thin slice. |
| Single hardcoded 2-player match (this phase) | A real matchmaking queue | Explicitly deferred — MATCH-01/02 is Phase 4. Phase 1 should NOT build a queue; the first two WS connections simply become "the match." Building matchmaking now is scope creep per PITFALLS.md Pitfall 6. |
| `Executors.newSingleThreadScheduledExecutor()` per match | `ConcurrentHashMap` + fine-grained locks | Rejected — project ARCHITECTURE.md/PITFALLS.md already settled on single-threaded-executor-per-match as the simpler, safer default for a solo/course-scale project; locks are more error-prone to retrofit correctly under deadline pressure. |

**Installation:**
```bash
# Frontend
npm create vite@latest client -- --template react-ts
cd client && npm install

# Backend pom.xml additions (Java 21 target)
# <dependency><groupId>io.javalin</groupId><artifactId>javalin</artifactId><version>7.2.3</version></dependency>
# <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.22.1</version></dependency>
```

**Version verification performed this session:**
- `curl https://repo1.maven.org/maven2/io/javalin/javalin/maven-metadata.xml` → latest release `7.2.3`, updated 2026-08-11.
- `curl https://repo1.maven.org/maven2/io/javalin/javalin/7.2.3/javalin-7.2.3.pom` → confirmed `jackson-databind` and `jackson-module-kotlin` both `<optional>true</optional>`; confirmed Jetty 12 (`jetty-ee10-websocket-jetty-server`) as the WS transport.
- `curl https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/maven-metadata.xml` → latest `2.22.1`.
- `npm view react version` → `19.2.8`; `npm view vite version` → `8.2.1`; `npm view vite engines` → Node `^20.19.0 || >=22.12.0`.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `react` | npm | published 2026-07-21 (recent patch of a long-established package) | 163M/wk | github.com/facebook/react | SUS (`too-new` — flags the recent *patch release date*, not the package) | Approved — false-positive on a well-known, already project-locked package; no `checkpoint:human-verify` needed given 163M weekly downloads and official Meta/React org repo. |
| `vite` | npm | published 2026-08-06 | 164M/wk | github.com/vitejs/vite | SUS (`too-new`, same false-positive pattern) | Approved — same reasoning as `react`. |
| `io.javalin:javalin` | Maven Central | long-established package (152+ versions), 7.2.3 published 2026-08-11 | N/A (Maven doesn't expose npm-style download counts) | github.com/javalin/javalin | Not run through the npm-ecosystem legitimacy seam (Maven not supported by that tool) — manually verified via direct Maven Central `maven-metadata.xml`/POM inspection | Approved — verified via direct registry inspection, official org, matches project-level STACK.md choice. |
| `com.fasterxml.jackson.core:jackson-databind` | Maven Central | long-established (FasterXML org) | N/A | github.com/FasterXML/jackson-databind | Manually verified via Maven Central metadata | Approved. |

**Packages removed due to [SLOP] verdict:** none.
**Packages flagged as suspicious [SUS]:** `react`, `vite` — both approved as false positives (recency-only signal on already-adopted, extremely high-download packages); no additional checkpoint required.

## Architecture Patterns

### System Architecture Diagram (this phase's thin slice only)

```
┌─────────────────────────────┐        ┌─────────────────────────────┐
│   Browser A (React + Vite)  │        │   Browser B (React + Vite)  │
│                              │        │                              │
│  keydown/keyup ──▶ input.ts │        │  keydown/keyup ──▶ input.ts │
│        │  (throttled ~33ms) │        │        │  (throttled ~33ms) │
│        ▼                    │        │        ▼                    │
│  WebSocket.send({type:      │        │  WebSocket.send({type:      │
│   "input", direction})      │        │   "input", direction})      │
└────────────┬─────────────────┘        └────────────┬─────────────────┘
             │  ws://host:7000/ws                     │  ws://host:7000/ws
             ▼                                         ▼
        ┌────────────────────────────────────────────────────┐
        │         Javalin config.routes.ws("/ws", ...)         │
        │  onConnect: assign PlayerSlot 1 or 2 to this session │
        │  onMessage: executor.execute(() -> match.applyInput) │──┐
        └────────────────────────────────────────────────────┘  │
                             │  (single-threaded executor per match, serializes)
                             ▼                                    │
        ┌────────────────────────────────────────────────────┐  │
        │   Match (in-memory, one hardcoded instance)          │  │
        │   - GameState { tankA{x,y,facing}, tankB{x,y,facing}}│◀─┘
        │   - tick(): apply last-known input * speed * dt,     │
        │             clamp to 800x600 arena bounds             │
        │   - runs on Executors.newSingleThreadScheduledExecutor│
        │     scheduleAtFixedRate(tick, 0, 33ms)                │
        └───────────────────────┬────────────────────────────┘
                                 │  broadcast {type:"state", tankA, tankB} to BOTH sessions, every tick
                    ┌────────────┴────────────┐
                    ▼                         ▼
        ┌───────────────────────┐  ┌───────────────────────┐
        │ Browser A: onmessage  │  │ Browser B: onmessage  │
        │  → writes to stateRef │  │  → writes to stateRef │
        │  (NOT useState)       │  │  (NOT useState)       │
        └───────────┬───────────┘  └───────────┬───────────┘
                    ▼                         ▼
        ┌───────────────────────┐  ┌───────────────────────┐
        │ requestAnimationFrame │  │ requestAnimationFrame │
        │  reads stateRef,      │  │  reads stateRef,      │
        │  draws <canvas>       │  │  draws <canvas>       │
        └───────────────────────┘  └───────────────────────┘
```

A hand-crafted `{type:"state",...}` message sent from browser devtools has no effect: the server never reads `state` messages from clients — `onMessage` only recognizes `{type:"input"}` and ignores/logs anything else (success criterion 3).

### Recommended Project Structure (this phase's slice)
```
server/
├── src/main/java/.../
│   ├── connection/
│   │   └── GameWebSocketHandler.java   # config.routes.ws("/ws", ...) registration + onConnect/onMessage/onClose
│   ├── match/
│   │   ├── Match.java                  # single hardcoded 2-player match, owns GameState + single-thread executor
│   │   ├── GameState.java              # tankA/tankB {x, y, facing}
│   │   └── protocol/
│   │       ├── ClientMessage.java      # {type:"input", direction}
│   │       └── ServerMessage.java      # {type:"joined"|"state", ...}
│   └── Main.java                       # Javalin.create(config -> { config.routes.ws(...) }).start(7000)
└── pom.xml

client/
├── src/
│   ├── network/
│   │   └── useGameSocket.ts            # thin WebSocket wrapper, writes into a ref via callback
│   ├── game/
│   │   ├── GameCanvas.tsx              # <canvas> + requestAnimationFrame reading the ref
│   │   └── input.ts                    # keydown/keyup → held-direction tracking, throttled send
│   └── App.tsx
└── vite.config.ts
```

### Pattern 1: Javalin 7 WebSocket endpoint declaration (verified syntax)

**What:** Javalin 7 moved WS route registration inside the `Javalin.create(config -> ...)` block via `config.routes.ws(...)` — the `app.ws(...)`-after-creation form from Javalin 5/6 tutorials no longer applies.
**When to use:** All WebSocket endpoints in this project.
**Example:**
```java
// Source: javalin.io/migration-guide-javalin-6-to-7 [CITED] — verified this session via WebFetch
var app = Javalin.create(config -> {
    config.routes.ws("/ws", ws -> {
        ws.onConnect(ctx -> {
            // assign this ctx to PlayerSlot 1 or 2
        });
        ws.onMessage(ctx -> {
            ClientMessage msg = ctx.messageAsClass(ClientMessage.class);
            match.getExecutor().execute(() -> match.applyInput(playerId, msg));
        });
        ws.onClose(ctx -> {
            // mark player disconnected (full handling deferred to Phase 3 / LIFE-01)
        });
        ws.onError(ctx -> System.out.println("Errored"));
    });
}).start(7000);
```
Note: `ctx.matchedPath()` from Javalin 6 became `ctx.endpoint().path()` in Javalin 7 — irrelevant for this phase's single fixed `/ws` path but worth knowing if path params are added later.

### Pattern 2: Broadcasting to both sessions in a match

**What:** Track each match's two `WsContext` objects (or a small wrapper), and call `ctx.send(payload)` on each from inside the tick, after filtering for `ctx.session.isOpen()`.
**When to use:** Every tick broadcast (this is what makes success criterion 2 — "other window shows movement live" — actually work).
**Example:**
```java
// Source: javalin.io/tutorials/websocket-example [CITED] — verified this session via WebFetch
void broadcast(ServerMessage state) {
    for (WsContext ctx : List.of(playerASession, playerBSession)) {
        if (ctx != null && ctx.session.isOpen()) {
            ctx.send(state); // Jackson serializes via ctx.send(Object) — requires jackson-databind on classpath
        }
    }
}
```

### Pattern 3: Single-threaded-executor-per-match concurrency confinement

**What:** One `ScheduledExecutorService` (created with `Executors.newSingleThreadScheduledExecutor()`, NOT a pooled executor) per match. The recurring tick task AND every inbound WS message handler both submit their work to this same single-thread executor. Because only one thread ever touches `GameState`, there is no need for `synchronized`, locks, or concurrent collections — the executor itself is the serialization point.
**When to use:** All match-state mutation, per PITFALLS.md Pitfall 4 and ARCHITECTURE.md's recommended default.
**Example:**
```java
// Pattern synthesized from java.util.concurrent.ScheduledExecutorService docs + project ARCHITECTURE.md Pattern 1 [ASSUMED: exact code not copied from a single official source; standard JDK idiom]
public class Match {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final GameState state = new GameState();
    private long lastTickNanos = System.nanoTime();

    public void start() {
        executor.scheduleAtFixedRate(this::tick, 0, 33, TimeUnit.MILLISECONDS); // ~30Hz
    }

    // Called from the WS onMessage callback thread — MUST go through executor.execute,
    // never mutate `state` directly on the calling thread.
    public void onPlayerInput(PlayerId id, Direction dir) {
        executor.execute(() -> state.setIntent(id, dir));
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        state.advance(dt); // position += speed * dt, clamped to arena bounds — delta-time, not fixed increment (PITFALLS.md Pitfall 3)
        broadcast(state.snapshot());
    }
}
```

### Pattern 4: React ref-based render loop (not `useState` per tick)

**What:** Store the latest server-pushed state in a `useRef`, updated from the WebSocket `onmessage` callback; drive `<canvas>` drawing from a `requestAnimationFrame` loop that reads the ref directly. React `useState`/re-render is never invoked per network tick.
**When to use:** All gameplay rendering this phase (per project ARCHITECTURE.md Anti-Pattern 2 and PITFALLS.md).
**Example:**
```ts
// Source: general React+Canvas game-loop pattern, cross-checked across multiple community sources [CITED: dev.to/childrentime, css-tricks.com/using-requestanimationframe-with-react-hooks — MEDIUM confidence, not a single canonical doc]
function useGameSocket(url: string) {
  const stateRef = useRef<GameState | null>(null);
  useEffect(() => {
    const ws = new WebSocket(url);
    ws.onmessage = (e) => { stateRef.current = JSON.parse(e.data); };
    return () => ws.close();
  }, [url]);
  return stateRef;
}

function GameCanvas({ stateRef }: { stateRef: React.RefObject<GameState | null> }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    let raf: number;
    const draw = () => {
      const ctx = canvasRef.current?.getContext('2d');
      if (ctx && stateRef.current) { /* draw tanks from stateRef.current */ }
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, [stateRef]);
  return <canvas ref={canvasRef} width={800} height={600} />;
}
```

### Anti-Patterns to Avoid
- **`app.ws(...)` after `Javalin.create()` with no args:** This is the Javalin 5/6 shape and will not compile against Javalin 7's config-based routing (`config.routes.ws(...)`). Copy-pasting an older tutorial verbatim is the most likely first mistake.
- **Forgetting `jackson-databind` in `pom.xml`:** Compiles fine, fails at first `ctx.send(object)` call with a missing-JSON-mapper error, since Javalin marks it optional.
- **Processing input directly in `onMessage` without routing through the match's executor:** Reintroduces the exact race PITFALLS.md Pitfall 4 warns about — `onMessage` runs on a Jetty I/O thread, not the tick thread.
- **`x += speed` per tick instead of `x += speed * deltaTimeSeconds`:** Looks correct with a steady local tick rate, breaks (PITFALLS.md Pitfall 3) the moment tick timing jitters — implement delta-time from the first tick, not as a later fix.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|--------------|-----|
| WebSocket frame parsing/handshake | A custom `Upgrade: websocket` HTTP handler | Javalin's `config.routes.ws(...)` (built on Jetty 12) | Handshake/framing correctness (masking, fragmentation, ping/pong) is exactly the kind of protocol-detail bug that's invisible until a real (non-Chrome-devtools) client hits it. |
| JSON (de)serialization of messages | Hand-written string concatenation/parsing | `jackson-databind` via `ctx.send(obj)` / `ctx.messageAsClass(Class)` | One typo in manual JSON string-building silently breaks the opponent's view; Jackson's typed mapping fails loudly at compile/deserialize time instead. |
| Per-match thread safety | Ad hoc `synchronized` blocks sprinkled around `GameState` field accesses | `Executors.newSingleThreadScheduledExecutor()` confinement (Pattern 3) | Partial locking (`synchronized` on some accessors, not others) is the classic way races survive local testing and appear only under real concurrent load — exactly success criterion 4's scenario. |

**Key insight:** every "don't hand-roll" item above maps directly to one of this phase's four success criteria — get the framework/library doing the mechanical parts right, and success criteria 1-4 fall out of getting the four Patterns above correctly wired, not out of writing more code.

## Common Pitfalls

(See project-level `.planning/research/PITFALLS.md` for the full catalogue — Pitfalls 1, 2, 3, 4, 6, 7, 8 all apply directly to this phase's scope. Phase-specific sharpening below.)

### Pitfall A: Copying Javalin 5/6 tutorial syntax verbatim
**What goes wrong:** `app.ws("/chat", ws -> {...})` called directly on the `Javalin` instance — compiles against Javalin 5/6 but not 7.
**Why it happens:** Most search results and tutorials (including the official `javalin.io/tutorials/websocket-example` walkthrough) predate the 7.0 config-based routing change; only the dedicated migration guide documents the new form.
**How to avoid:** Use `config.routes.ws(...)` inside `Javalin.create(config -> ...)` — see Pattern 1.
**Warning signs:** A compile error on `app.ws(...)` with no matching method, or (worse) an old Javalin 6.x dependency accidentally left in `pom.xml` alongside 7.x code samples.

### Pitfall B: `jackson-databind` missing from classpath
**What goes wrong:** `pom.xml` lists only `io.javalin:javalin`; the app compiles, starts, and even accepts WS connections — then throws at the first `ctx.send(someDto)` call because Javalin has no JSON mapper.
**Why it happens:** `jackson-databind` is declared `<optional>true</optional>` in Javalin's own POM (verified this session), so Maven does not pull it transitively; nothing in `pom.xml` or the IDE flags this until runtime.
**How to avoid:** Add `com.fasterxml.jackson.core:jackson-databind:2.22.1` explicitly to `pom.xml` before writing the first `onMessage`/`send` code.
**Warning signs:** `IllegalStateException`/mapper-not-found style error the first time a typed object (not a raw String) is sent or received over the WS endpoint.

### Pitfall C: Assuming a CORS fix is needed for the WebSocket connection
**What goes wrong:** Time spent adding `config.bundledPlugins.enableCors(...)` to "fix" a WS connection failure between the Vite dev server (5173) and Javalin (7000) — CORS is not the actual mechanism governing WS cross-origin behavior.
**Why it happens:** The instinct to reach for CORS config is strong because it's the standard fix for cross-origin HTTP/fetch failures, and Vite-to-backend is genuinely cross-origin.
**How to avoid:** Know that WS handshakes bypass SOP/CORS entirely (no preflight, no `Access-Control-Allow-Origin` needed) — verified this session via WebSearch cross-checked against websocket.org's CORS troubleshooting guide. If a WS connection actually fails cross-port in dev, the real culprit is almost always a reverse proxy/middleware stripping the `Upgrade` header, a `wss://` vs `ws://` mismatch (mixed content on an `https` page), or the server simply not running — not CORS.
**Warning signs:** Browser console shows a WS connection failure with no CORS-specific error text (CORS errors have a distinct, recognizable browser console message; a generic WS `1006` close code or "connection failed" is not that).

## Runtime State Inventory

Not applicable — this is a greenfield phase (first phase of a new project, no prior code, no rename/refactor/migration in scope).

## Code Examples

### Client → Server input message
```json
{ "type": "input", "direction": "UP" }
```
`direction` is one of `"UP" | "DOWN" | "LEFT" | "RIGHT" | "NONE"` — matches the locked Battle City-style 4-directional model (D-01, D-02); sent on key-state change (edge-triggered), not on every `keydown` repeat event (per project PITFALLS.md Performance Trap: "sending one WS message per keypress event").

### Server → Client state broadcast (every tick, ~30Hz)
```json
{
  "type": "state",
  "tick": 4821,
  "players": {
    "P1": { "x": 40.0, "y": 40.0, "facing": "DOWN" },
    "P2": { "x": 740.0, "y": 540.0, "facing": "UP" }
  }
}
```
Sent to **both** connected sessions identically every tick — including the player who sent the input (no client-side optimistic rendering this phase, per project ARCHITECTURE.md Pattern 2). This is what makes success criterion 4 ("both windows still show identical tank positions") trivially true: both clients render the exact same server-broadcast object.

### Join/handshake message (sent once per player, on match assignment)
```json
{
  "type": "joined",
  "playerId": "P1",
  "arena": { "width": 800, "height": 600 },
  "spawn": { "x": 40.0, "y": 40.0, "facing": "DOWN" },
  "opponentSpawn": { "x": 740.0, "y": 540.0, "facing": "UP" }
}
```
Since matchmaking (MATCH-01/02) is out of scope until Phase 4, this phase should hardcode: the first WS connection becomes `P1` (top-left spawn), the second becomes `P2` (bottom-right spawn), and a third connection is rejected or queued with no UI (not a success criterion this phase — don't build handling for it beyond "doesn't crash").

**Envelope discipline:** every message on the wire uses `{ "type": "...", ...payload }` — one `type` string, dispatched via switch/discriminated union on both ends — per project PITFALLS.md Pitfall 8. Adding `chat` or `matchStart` later is then a new `type` value, not a protocol redesign.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|-------------------|---------------|--------|
| `app.ws("/path", ws -> {...})` (Javalin 5/6) | `config.routes.ws("/path", ws -> {...})` inside `Javalin.create(config -> ...)` | Javalin 7.0.0 (2026) | Any tutorial/StackOverflow answer predating 7.0 needs this one syntactic translation — logic (onConnect/onMessage/onClose bodies) is otherwise unchanged. |
| `config.enableCorsForAllOrigins()` (Javalin ≤4) | `config.bundledPlugins.enableCors(cors -> cors.addRule(r -> r.anyHost()))` | Javalin 5+ | Not needed for the WS endpoint this phase (WS bypasses CORS); relevant later for REST auth/leaderboard routes (Phase 4). |

**Deprecated/outdated:** none directly relevant beyond the above — this is a young, actively-developed stack.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|-----------------|
| A1 | The exact `Match`/`Executors.newSingleThreadScheduledExecutor()` code shown in Pattern 3 is a synthesized idiom (standard JDK usage), not copied verbatim from one official tutorial | Architecture Patterns → Pattern 3 | Low — `ScheduledExecutorService`/`Executors.newSingleThreadScheduledExecutor()` are core JDK APIs with stable, well-documented contracts; the risk is only in exact method-chaining style, not correctness. |
| A2 | React ref+rAF example code (Pattern 4) is a synthesized pattern cross-checked across several community write-ups, not a single canonical/official source | Architecture Patterns → Pattern 4 | Low-Medium — the underlying principle (avoid `useState` per network tick) is corroborated by the project's own ARCHITECTURE.md Anti-Pattern 2; exact hook shape may need adjustment during implementation. |
| A3 | Recommended tick rate of 30Hz (33ms) and tank speed numbers are not sourced from any external doc — Claude's discretion per CONTEXT.md D-04 | Standard Stack / Code Examples | Low — explicitly delegated to Claude's discretion in CONTEXT.md; tune-after-playtesting is expected, not a defect. |
| A4 | `jetty-websocket-jetty-client` is suitable and sufficient for a headless Java-side stress test client (not hands-on tested this session, only confirmed it exists as a Javalin test-scope dependency) | Standard Stack → Supporting; Concurrency Stress-Test | Medium — if this dependency proves awkward to wire up standalone, fall back to the Playwright dual-BrowserContext approach (also documented) as the primary stress-test method. |

## Open Questions

1. **Exact spawn coordinates and tank/bullet speed constants**
   - What we know: Arena is ~800x600 (D-05, Claude's discretion on exact px), spawn at opposite corners (D-06).
   - What's unclear: Precise pixel values for tank size, spawn offset from the corner, and movement speed (px/sec) — all explicitly Claude's discretion (D-04) per CONTEXT.md.
   - Recommendation: Planner should pick concrete numbers (e.g., tank 32x32px, spawn at (40,40) and (728,528) so a 32px tank stays fully on-screen inset from each corner, speed ~150px/sec) and treat them as tunable constants isolated in one config location, not scattered magic numbers.

2. **What exactly happens on a third WebSocket connection attempt**
   - What we know: Only 2 players are in scope for v1 (REQUIREMENTS.md Out of Scope: "More than 2 players per match").
   - What's unclear: Whether Phase 1 needs explicit rejection logic for a 3rd connection, or whether "doesn't crash" is sufficient given no success criterion covers this.
   - Recommendation: Minimal handling only — e.g., `onConnect` for a 3rd session either closes the connection immediately with a close code, or silently spectates with no input processed. Full "match already full" UX is not a success criterion this phase; don't over-build it (PITFALLS.md Pitfall 6).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|--------------|-----------|---------|----------|
| Java (JDK) | Backend runtime | ✓ | OpenJDK 21.0.11 [VERIFIED: `java --version`] | — |
| Maven | Backend build | ✗ | — [VERIFIED: `command -v mvn` returned nothing] | Install via system package manager (`sudo apt install maven` on this Ubuntu 24.04 host) before Phase 1 execution begins, OR use an IDE (IntelliJ IDEA) that bundles its own Maven and can generate/build the project without a system-wide install. No maven-wrapper (`mvnw`) files can be bootstrapped without either a local Maven or manually vendoring wrapper scripts — flag this as a **blocking setup step**, not something the plan can silently work around. |
| Node.js | Frontend build/dev server | ✓ | v20.20.2 [VERIFIED: `node --version`] | — satisfies Vite 8's `^20.19.0` requirement. |
| npm | Package management | ✓ | 10.8.2 [VERIFIED: `npm --version`] | — |
| Network access to Maven Central / npm registry | Dependency resolution during scaffolding | ✓ [VERIFIED: `curl` to `repo1.maven.org` returned HTTP 200] | — | — |

**Missing dependencies with no fallback:**
- None — Maven's absence has a viable fallback (install before execution, or use an IDE that bundles it).

**Missing dependencies with fallback:**
- Maven — install via `apt install maven` or use IntelliJ IDEA's bundled Maven. The planner should surface this as an explicit first task/checkpoint in Wave 0 of the plan, since scaffolding cannot proceed without it.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None yet — greenfield project, no test infra exists. Recommend JUnit 5 (standard with Maven/Javalin ecosystem) for server-side `Match`/`GameState` unit tests. |
| Config file | none — see Wave 0 Gaps |
| Quick run command | `mvn -q -pl server test` (once scaffolded) |
| Full suite command | `mvn test` (server) — no client test framework needed this phase (no client-side logic complex enough to unit test; rendering is visually verified via UAT) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|---------------------|---------------|
| COMBAT-01 (movement visible to opponent) | Server broadcasts identical state to both sessions every tick | integration (headless WS client) | `mvn -pl server test -Dtest=MatchBroadcastTest` | ❌ Wave 0 |
| COMBAT-01 (server-authoritative, ignores forged state) | `onMessage` only processes `{type:"input"}`; a `{type:"state"}` message from a client is ignored | unit | `mvn -pl server test -Dtest=InputDispatchTest` | ❌ Wave 0 |
| COMBAT-01 (no-input client stops moving) | `GameState.advance(dt)` applies zero velocity when no direction is held | unit | `mvn -pl server test -Dtest=GameStateTest` | ❌ Wave 0 |
| COMBAT-01 (concurrent input, no drift/crash) | Two headless WS clients sending input concurrently for a sustained period; assert final positions match what both clients received | integration/stress | `mvn -pl server test -Dtest=ConcurrentInputStressTest` | ❌ Wave 0 |
| Success criteria 1-2 (two browsers, live movement) | Manual/Playwright UAT — visual, not unit-testable | manual + optional Playwright dual-context script | N/A (manual) or `npx playwright test movement.spec.ts` | ❌ Wave 0 (if Playwright route chosen) |

### Sampling Rate
- **Per task commit:** `mvn -pl server test` (fast unit tests only)
- **Per wave merge:** `mvn test` (full suite, including the concurrency stress test)
- **Phase gate:** Full suite green + manual two-real-browser-process UAT (per project PITFALLS.md Pitfall 7 — never rely on two-tabs-on-localhost as the only verification) before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `server/pom.xml` — needs JUnit 5 test dependency added (`junit-jupiter`, standard for Java 21/Maven projects)
- [ ] `server/src/test/java/.../GameStateTest.java` — covers delta-time movement, no-input-stops-moving
- [ ] `server/src/test/java/.../InputDispatchTest.java` — covers server ignoring non-input message types
- [ ] `server/src/test/java/.../ConcurrentInputStressTest.java` — see Concurrency Stress-Test Approach below
- [ ] Maven itself — see Environment Availability blocking gap above; must be resolved before any `mvn test` command can run

## Concurrency Stress-Test Approach (success criterion 4)

Two complementary techniques, in order of recommended precedence:

**1. Automated headless WS stress test (primary, repeatable, CI-friendly):**
Add `org.eclipse.jetty.websocket:jetty-websocket-jetty-client` as a `test`-scope Maven dependency (the same artifact Javalin itself uses in its own test suite [CITED: javalin-7.2.3.pom test-scope dependencies]). Write a JUnit test that opens two real WebSocket client connections to a running (or `@BeforeEach`-started) `Match`/server instance, and from two separate threads sends a rapid burst of alternating `{type:"input", direction:...}` messages for several seconds — simulating "both players mash movement keys simultaneously." Assert: (a) no exception/crash, (b) both clients' received `state` broadcasts are identical for the same `tick` number (no per-client divergence — this is the direct check for "no drift"), (c) final tank positions stay within arena bounds. This directly exercises Pattern 3's single-thread executor confinement under real concurrent load — the exact scenario PITFALLS.md Pitfall 4 warns "never reproduces when single-stepping in a debugger."

**2. Two independent browser processes / Playwright dual-context (secondary, human-facing UAT):**
Per the phase notes ("test with two independent browser processes plus artificial latency, not two tabs on localhost") and project PITFALLS.md Pitfall 7: launch two actually-separate browser processes (not two tabs of one browser sharing storage), optionally with Chrome DevTools network throttling on one, and have a human (or a scripted Playwright test using two `BrowserContext`s, which have isolated storage similar to separate processes [CITED: playwright.dev/docs/browser-contexts]) hold movement keys simultaneously for a sustained stretch. Visually confirm both windows agree, and check server logs/console for exceptions.

Both techniques should exist by the end of this phase — automated test 1 catches regressions on every commit; manual/scripted test 2 is the actual success-criterion-4 acceptance check since it matches the literal wording ("two players in separate browsers").

## Sources

### Primary (HIGH confidence)
- Maven Central `maven-metadata.xml` for `io.javalin:javalin` — direct registry query, this session — version 7.2.3 confirmed
- Maven Central `javalin-7.2.3.pom` — direct file inspection, this session — `jackson-databind` optional dependency, Jetty 12 transport confirmed
- Maven Central `maven-metadata.xml` for `com.fasterxml.jackson.core:jackson-databind` — version 2.22.1 confirmed
- `npm view react version` / `npm view vite version` / `npm view vite engines` / `npm view create-vite version` — direct registry queries, this session
- Local environment probes (`java --version`, `node --version`, `npm --version`, `command -v mvn`) — this session

### Secondary (MEDIUM confidence)
- [javalin.io/migration-guide-javalin-6-to-7](https://javalin.io/migration-guide-javalin-6-to-7) — `config.routes.ws(...)` syntax, fetched this session via WebFetch
- [javalin.io/documentation](https://javalin.io/documentation) — WsContext method surface, CORS bundled-plugin syntax, fetched this session via WebFetch
- [javalin.io/tutorials/websocket-example](https://javalin.io/tutorials/websocket-example) — broadcast-to-sessions pattern, fetched this session via WebFetch
- [javalin.io/news/javalin-7.0.0-stable.html](https://javalin.io/news/javalin-7.0.0-stable.html) — Java 17+/Jetty 12 requirement confirmation, fetched this session via WebFetch
- [vite.dev/config/server-options](https://vite.dev/config/server-options) — `server.proxy` `ws:true` option, fetched this session via WebFetch
- [websocket.org/guides/troubleshooting/cors](https://websocket.org/guides/troubleshooting/cors) — WebSocket bypasses browser CORS/SOP, via WebSearch this session
- [playwright.dev/docs/browser-contexts](https://playwright.dev/docs/browser-contexts) — isolated `BrowserContext`s for multi-user simulation, via WebSearch this session

### Tertiary (LOW confidence)
- General Java `ScheduledExecutorService` game-loop idiom — WebSearch only, standard/uncontested JDK pattern, not independently cross-verified against an official Oracle doc this session
- React ref+`requestAnimationFrame` render-loop pattern — WebSearch aggregation of several community blog posts (dev.to, css-tricks.com), consistent with project-level ARCHITECTURE.md's own recommendation but not from one canonical source

## Metadata

**Confidence breakdown:**
- Standard stack (versions): HIGH — every version number verified directly against Maven Central / npm registry this session, not from training memory
- Javalin 7 API specifics (`config.routes.ws`, optional jackson-databind): HIGH — confirmed via direct POM inspection and official migration guide, catches a real gap in the project-level STACK.md
- Architecture/concurrency patterns: MEDIUM — well-established industry patterns, consistent with project ARCHITECTURE.md/PITFALLS.md, but the exact code shown is synthesized (JDK idiom), not copy-pasted from one official tutorial
- Environment availability: HIGH — every tool probed directly on this machine this session

**Research date:** 2026-08-13
**Valid until:** 30 days (2026-09-12) — Javalin/npm ecosystem version numbers should be re-verified if planning is picked back up after that window, given how frequently these packages release (Javalin shipped 7.2.0→7.2.3 within roughly the past month per Maven metadata timestamps).

---
*Phase: 01-networked-movement-skeleton*
*Research completed: 2026-08-13*
