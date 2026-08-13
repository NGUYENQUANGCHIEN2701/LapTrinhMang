# Phase 1: Networked Movement Skeleton - Context

**Gathered:** 2026-08-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Two players in separate browsers connect to the Java server and see each other's tanks move live over a real client-server connection, with the server owning the truth. This phase delivers ONLY movement sync (COMBAT-01) — no shooting, no walls, no HP, no accounts. It locks the message protocol envelope, the server-authoritative model, and the concurrency strategy for shared match state, since those are expensive to retrofit later.

</domain>

<decisions>
## Implementation Decisions

### Control Scheme & Rotation
- **D-01:** Classic Battle City-style controls — pressing a direction (WASD/arrow keys) both rotates the tank body to face that direction AND moves it that way. No independent turret aim; firing (future phase) will always go in the direction the tank body currently faces. — **Reversibility:** one-way — switching to twin-stick later means adding a second synced angle (turret) to the network payload and rewriting client input handling and rendering; treat this as locked for the whole project, not just this phase.
- **D-02:** 4-directional movement/facing only (up/down/left/right) — no diagonal/8-direction movement.

### Movement Feel
- **D-03:** Free, continuous pixel-by-pixel movement (not grid-snapped) — the tank slides smoothly along continuous coordinates. This is also the simpler fit for continuous-position network sync (vs. discrete tile-index sync).
- **D-04:** Tank speed and bullet speed values are Claude's discretion — pick numbers that feel like a classic tank game (bullets travel noticeably faster than tanks), tune after playtesting. Not user-specified.

### Arena Layout
- **D-05:** Small-to-medium arena that fits entirely on one screen (~800x600px scale) — no camera scrolling/following. The whole arena is visible at all times.
- **D-06:** The two tanks spawn at opposite corners of the arena (diagonal from each other) at match start.

### Claude's Discretion
- Exact tank/bullet speed numbers (D-04).
- Exact arena pixel dimensions within the "fits on one screen, no scrolling" constraint (D-05).
- Visual placeholder style for tanks/arena at this skeleton stage (simple shapes are expected — no shooting/walls/art requirements yet).
- Message protocol envelope shape, tick rate (~20-30Hz per research), and concurrency model (single-threaded executor per match, per research) — these are already recommended in research/STACK.md and research/ARCHITECTURE.md; treat as the default unless research-phase during planning surfaces a reason to deviate.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project & Requirements
- `.planning/PROJECT.md` — core value, constraints (React frontend + Java backend, WebSocket acceptable, solo/several-week course project)
- `.planning/REQUIREMENTS.md` — COMBAT-01 is this phase's only requirement; full v1 scope for context
- `.planning/ROADMAP.md` §Phase 1 — phase goal, success criteria, and locked notes (message protocol envelope, server-authoritative model, concurrency strategy, fixed-tick loop, test-with-two-real-browser-processes requirement)

### Research (informs HOW, already opinionated)
- `.planning/research/STACK.md` — recommended Javalin + native WebSocket + JSON messages + ScheduledExecutorService tick loop
- `.planning/research/ARCHITECTURE.md` — component boundaries (Connection/Session Layer, Match/Game Loop), authoritative-server pattern, Canvas render loop driven by requestAnimationFrame (not React setState per tick)
- `.planning/research/PITFALLS.md` — critical pitfalls this phase must avoid: client authority, missing server tick loop, threading races on shared match state, testing only on localhost

</canonical_refs>

<code_context>
## Existing Code Insights

Greenfield project — no existing code, no `.planning/codebase/` maps. Nothing to reuse or integrate with yet; this phase establishes the foundational patterns (protocol, server loop, client render loop) that all later phases build on.

</code_context>

<specifics>
## Specific Ideas

- Movement/rotation model is explicitly "Battle City-style" (the classic top-down tank game reference the user named during project questioning) — not a twin-stick shooter. This shapes the network payload (position + single facing direction, no separate turret angle) and the input handling on both client and server.
- Arena is a fixed single-screen view, tanks spawn at opposite corners — set up the test scenario for "both players see identical positions after simultaneous input" (Phase 1 success criterion 4).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. Walls/collision, shooting/ricochet, HP, and accounts are already scoped to Phases 2-4 per ROADMAP.md; not re-discussed here.

</deferred>

---
*Phase: 1-networked-movement-skeleton*
*Context gathered: 2026-08-13*
