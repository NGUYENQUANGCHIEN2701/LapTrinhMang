# Phase 1: Networked Movement Skeleton - Pattern Map

**Mapped:** 2026-08-13
**Files analyzed:** 12 (new — no modified files, greenfield project)
**Analogs found:** 0 / 12 (no existing codebase)

## Codebase Status

**Verified this session:** `find /home/nqc/Documents/LapTrinhMang -maxdepth 2` (excluding `.git`, `.planning`, `.claude`) returns nothing. This is a genuinely empty repository — zero application source code, zero prior commits with code, no `server/` or `client/` directories yet. There is **no codebase to search for analogs**. This is expected and stated plainly per the orchestrator's note, rather than inventing or fabricating an analog.

Because there is no codebase, this PATTERNS.md deviates from the standard template: instead of "existing file X -> analog," every file's "closest analog" is the **verified pattern from 01-RESEARCH.md**, which contains concrete, source-cited code (Javalin migration guide, Javalin tutorials, JDK docs, React/Canvas community patterns). The planner should treat RESEARCH.md's Pattern 1-4 code blocks (lines 192-296) as the authoritative source to copy from for this phase, since no in-repo precedent exists yet. **All files in this phase are precedent-setting** — whatever patterns are used here become the analog for Phase 2+.

## File Classification

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|-----------------|----------------|
| `server/pom.xml` | config | N/A | RESEARCH.md Standard Stack (lines 88-97) | no-analog (spec provided) |
| `server/src/main/java/.../Main.java` | config/bootstrap | request-response | RESEARCH.md Pattern 1 (lines 192-215) | no-analog (spec provided) |
| `server/src/main/java/.../connection/GameWebSocketHandler.java` | controller (WS handler) | event-driven | RESEARCH.md Pattern 1 & 2 (lines 192-231) | no-analog (spec provided) |
| `server/src/main/java/.../match/Match.java` | service (owns tick loop + concurrency) | event-driven + streaming (broadcast) | RESEARCH.md Pattern 3 (lines 233-263) | no-analog (spec provided) |
| `server/src/main/java/.../match/GameState.java` | model | CRUD (in-memory mutation) | RESEARCH.md Pattern 3 (`state.advance(dt)`, lines 255-262) | no-analog (spec provided) |
| `server/src/main/java/.../match/protocol/ClientMessage.java` | model (DTO) | transform | RESEARCH.md Code Examples (lines 342-346) | no-analog (spec provided) |
| `server/src/main/java/.../match/protocol/ServerMessage.java` | model (DTO) | transform | RESEARCH.md Code Examples (lines 348-371) | no-analog (spec provided) |
| `server/src/test/java/.../GameStateTest.java` | test | CRUD | RESEARCH.md Validation Architecture (lines 432-450) | no-analog (spec provided) |
| `server/src/test/java/.../InputDispatchTest.java` | test | event-driven | RESEARCH.md Validation Architecture (lines 432-450) | no-analog (spec provided) |
| `server/src/test/java/.../ConcurrentInputStressTest.java` | test | event-driven/streaming | RESEARCH.md Concurrency Stress-Test Approach (lines 452-460) | no-analog (spec provided) |
| `client/src/network/useGameSocket.ts` | hook | streaming | RESEARCH.md Pattern 4 (lines 270-280) | no-analog (spec provided) |
| `client/src/game/GameCanvas.tsx` | component | streaming (render loop) | RESEARCH.md Pattern 4 (lines 282-295) | no-analog (spec provided) |
| `client/src/game/input.ts` | utility | event-driven | RESEARCH.md Architecture Diagram note (lines 125-129, edge-triggered throttled send) | no-analog (spec provided) |
| `client/src/App.tsx` | component (composition root) | request-response | Standard Vite `react-ts` template scaffold (no project precedent) | no-analog (framework default) |

## Pattern Assignments

### `server/src/main/java/.../Main.java` (config/bootstrap, request-response)

**Source:** RESEARCH.md Pattern 1 (verified against `javalin.io/migration-guide-javalin-6-to-7`)

**Critical constraint — must use Javalin 7 syntax, NOT the common Javalin 5/6 tutorial shape:**
```java
var app = Javalin.create(config -> {
    config.routes.ws("/ws", ws -> {
        ws.onConnect(ctx -> { /* assign PlayerSlot 1 or 2 */ });
        ws.onMessage(ctx -> {
            ClientMessage msg = ctx.messageAsClass(ClientMessage.class);
            match.getExecutor().execute(() -> match.applyInput(playerId, msg));
        });
        ws.onClose(ctx -> { /* mark disconnected */ });
        ws.onError(ctx -> System.out.println("Errored"));
    });
}).start(7000);
```
**Do NOT write** `app.ws(...)` after a no-arg `Javalin.create()` — that is the Javalin 5/6 shape and will not compile against Javalin 7.2.3 (pinned in `pom.xml` per RESEARCH.md Standard Stack).

**pom.xml must explicitly declare** (not transitively pulled in — `optional=true` in Javalin's own POM):
```xml
<dependency><groupId>io.javalin</groupId><artifactId>javalin</artifactId><version>7.2.3</version></dependency>
<dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.22.1</version></dependency>
```

---

### `server/src/main/java/.../connection/GameWebSocketHandler.java` (controller, event-driven)

**Source:** RESEARCH.md Pattern 1 & 2

**Core pattern — broadcast to both sessions, filtered by open state:**
```java
void broadcast(ServerMessage state) {
    for (WsContext ctx : List.of(playerASession, playerBSession)) {
        if (ctx != null && ctx.session.isOpen()) {
            ctx.send(state); // Jackson serializes via ctx.send(Object)
        }
    }
}
```

**Error handling:** `ws.onError(ctx -> System.out.println("Errored"))` — RESEARCH.md does not specify richer error handling for this phase; minimal logging is sufficient (skeleton phase, no reconnect logic in scope).

**Server-authority constraint (must be enforced here):** `onMessage` recognizes ONLY `{type:"input"}`. A `{type:"state"}` or any other type from a client must be ignored/logged, never applied to `GameState` — this is what makes forged client state a no-op (phase success criterion 3).

---

### `server/src/main/java/.../match/Match.java` (service, event-driven + streaming)

**Source:** RESEARCH.md Pattern 3 — single-threaded-executor-per-match concurrency confinement

**Core pattern (concurrency + tick loop):**
```java
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
        state.advance(dt); // delta-time, not fixed increment — see Anti-Patterns
        broadcast(state.snapshot());
    }
}
```

**Concurrency rule (applies to every file that touches `GameState`):** only ever mutate `GameState` by submitting a `Runnable` to this single-thread `executor`. Never use `synchronized`, locks, or concurrent collections as a substitute — the executor itself is the sole serialization point. No exceptions.

**Anti-pattern to avoid:** `x += speed` per tick instead of `x += speed * deltaTimeSeconds` — looks correct at a steady local tick rate but drifts once timing jitters. Implement delta-time movement from the first line of `tick()`, not as a later fix.

---

### `server/src/main/java/.../match/GameState.java` (model, CRUD)

**Source:** RESEARCH.md Pattern 3 (`state.advance(dt)`) + Code Examples (state shape, lines 348-359)

**Shape to implement (from the wire protocol in RESEARCH.md Code Examples):**
```json
{ "tankA": { "x": 40.0, "y": 40.0, "facing": "DOWN" },
  "tankB": { "x": 740.0, "y": 540.0, "facing": "UP" } }
```
`advance(dt)` applies `position += speed * dt` per held direction, clamped to arena bounds (800x600 per D-05). `setIntent(playerId, direction)` records the last-known held direction per player; a `NONE`/no-input state means zero velocity (per Validation Architecture test `GameStateTest`).

---

### `server/.../protocol/ClientMessage.java` and `ServerMessage.java` (model/DTO, transform)

**Source:** RESEARCH.md Code Examples (lines 342-371) — exact wire shapes, envelope-discipline is a hard constraint

**Client -> Server:**
```json
{ "type": "input", "direction": "UP" }
```
`direction` in `"UP" | "DOWN" | "LEFT" | "RIGHT" | "NONE"`.

**Server -> Client (state broadcast, every tick):**
```json
{ "type": "state", "tick": 4821,
  "players": { "P1": {"x":40.0,"y":40.0,"facing":"DOWN"}, "P2": {"x":740.0,"y":540.0,"facing":"UP"} } }
```

**Server -> Client (join/handshake, once per player):**
```json
{ "type": "joined", "playerId": "P1",
  "arena": {"width":800,"height":600},
  "spawn": {"x":40.0,"y":40.0,"facing":"DOWN"},
  "opponentSpawn": {"x":740.0,"y":540.0,"facing":"UP"} }
```

**Envelope discipline (hard constraint, applies to both DTOs):** every message uses `{ "type": "...", ...payload }`, dispatched via a switch/discriminated union on both ends. Adding a new message kind later (e.g. `chat`, `matchStart`) means a new `type` value, not a protocol redesign. Do not deviate from this envelope shape.

---

### `client/src/network/useGameSocket.ts` (hook, streaming)

**Source:** RESEARCH.md Pattern 4

```ts
function useGameSocket(url: string) {
  const stateRef = useRef<GameState | null>(null);
  useEffect(() => {
    const ws = new WebSocket(url);
    ws.onmessage = (e) => { stateRef.current = JSON.parse(e.data); };
    return () => ws.close();
  }, [url]);
  return stateRef;
}
```
**Hard constraint:** write into a `useRef`, never `useState`, on every incoming tick message — a `useState` write here would trigger a React re-render at 30Hz, which is the anti-pattern this whole phase's client architecture is designed to avoid (project ARCHITECTURE.md Anti-Pattern 2).

---

### `client/src/game/GameCanvas.tsx` (component, streaming render loop)

**Source:** RESEARCH.md Pattern 4

```ts
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
Drawing logic reads `stateRef.current` only — no local game-logic computation in this component (server is sole authority).

---

### `client/src/game/input.ts` (utility, event-driven)

**Source:** RESEARCH.md Architecture Diagram note (lines 125-129) — no ready-made code block in RESEARCH.md, so this is a spec, not a verbatim excerpt.

**Pattern to follow:** track held-direction via `keydown`/`keyup` listeners, send `{type:"input", direction}` on **edge-triggered state change only** (i.e., when the held direction actually changes), not on every native `keydown` repeat event. RESEARCH.md's project-level PITFALLS.md explicitly calls out "sending one WS message per keypress event" as a performance trap to avoid. Throttle target ~33ms (matching server tick rate) if using a polling approach instead of pure edge-triggering.

---

## Shared Patterns

### Message Envelope (applies to every file that touches the wire protocol)
**Source:** RESEARCH.md Code Examples (lines 342-373)
**Apply to:** `ClientMessage.java`, `ServerMessage.java`, `GameWebSocketHandler.java`, `useGameSocket.ts`, `input.ts`
```json
{ "type": "...", ...payload }
```
One `type` string per message, dispatched via switch/discriminated union on both client and server. This is locked for the whole project per CONTEXT.md D-01 reversibility note — do not deviate even for "just this phase."

### Concurrency Confinement (applies to all server-side match-state mutation)
**Source:** RESEARCH.md Pattern 3 (lines 233-263)
**Apply to:** `Match.java`, `GameState.java`, `GameWebSocketHandler.java`
Single `Executors.newSingleThreadScheduledExecutor()` per match. ALL mutation of `GameState` — whether from the tick loop or from `onMessage` — must be submitted via `executor.execute(...)`. Never touch `GameState` fields directly from a Jetty I/O thread.

### Server Authority / Ignore Forged Client State (applies to all message-handling code)
**Source:** RESEARCH.md Architecture Diagram note (line 164) + Code Examples section intro
**Apply to:** `GameWebSocketHandler.java`, `InputDispatchTest.java`
`onMessage` only recognizes and acts on `{type:"input"}`. Any other `type` (e.g., a hand-crafted `{type:"state"}` sent from devtools) is ignored/logged, never applied to `GameState`.

### Ref-based Render Loop (applies to all client rendering)
**Source:** RESEARCH.md Pattern 4 (lines 265-296)
**Apply to:** `useGameSocket.ts`, `GameCanvas.tsx`
Server-pushed state lives in a `useRef`, updated in the WS `onmessage` callback; `<canvas>` draws via `requestAnimationFrame` reading that ref. `useState`/React re-render is never invoked per network tick.

## No Analog Found

All 12+ files below have no in-repo analog because the repository is empty. Every entry's "closest match" is a RESEARCH.md-verified pattern instead of codebase precedent — see Pattern Assignments above for the concrete excerpt to use in each case.

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| All files listed in File Classification table | various | various | Greenfield repository — zero existing application source code (verified via `find`, only `.git/`, `.planning/`, `.claude/` present). RESEARCH.md's cited/verified code blocks (Patterns 1-4, Code Examples section) are the substitute source of truth for the planner. |

## Metadata

**Analog search scope:** Entire repository root (`/home/nqc/Documents/LapTrinhMang`), `find -maxdepth 2` excluding `.git`, `.planning`, `.claude` — confirmed empty.
**Files scanned:** 0 source files (none exist)
**Pattern extraction date:** 2026-08-13
**Pattern source:** `.planning/phases/01-networked-movement-skeleton/01-RESEARCH.md` (all excerpts cross-referenced with line numbers above)
