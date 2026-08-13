# Project Research Summary

**Project:** Tank Battle Online
**Domain:** Real-time 1v1 multiplayer 2D tank battle game (Java backend + React web frontend)
**Researched:** 2026-08-13
**Confidence:** MEDIUM-HIGH

## Executive Summary

This is a real-time 1v1 multiplayer tank battle game using a Java WebSocket backend (Javalin 7.0.x) running an authoritative fixed-tick game loop (20–30 Hz), paired with a React web frontend (19.2.x + Vite) using native browser WebSockets. The stack is lean and justified for a solo, several-week university course project: Javalin unifies REST + WebSocket in one process, H2 provides embedded SQL persistence with zero server setup overhead, and JDBI handles lightweight data access without ORM boilerplate.

The primary technical risk is threading and concurrency: Java WebSocket servers invoke `onMessage` handlers on separate threads per connection, while the game loop runs on its own scheduled thread — both must safely access shared match state. This concurrency strategy (recommended: single-threaded executor per match) must be decided in the first phase and validated early, since retrofitting it later is expensive. Secondary risks are scope creep (building generic network abstractions before the game actually works), incomplete networking edge cases (disconnects mid-match), and testing only on localhost, which hides real-network issues like latency and genuinely concurrent input timing — exactly the conditions of the grading demo.

The feature landscape is well-calibrated to the project's actual decisions: clear table-stakes (account, 1v1 match, movement/shoot sync, HP, wall collision, chat, leaderboard, disconnect handling), unambiguous anti-features to avoid (more than 2 players, full client-prediction/rollback netcode, progression/economy systems), and a short list of sequenced differentiators (bullet ricochet, power-ups, a practice AI bot) that only matter once the core loop works.

## Key Findings

### Recommended Stack

Javalin 7.0.x (Java 17+, on Jetty 12) is recommended over Spring Boot or raw Java-WebSocket because it unifies REST endpoints (auth, leaderboard) and the real-time WebSocket game connection in a single lightweight process, with a WebSocket API that mirrors the raw socket lifecycle instead of hiding it behind annotation/DI magic — better for demonstrating networking concepts than a heavier framework. Persistence stays intentionally minimal.

**Core technologies:**
- Javalin 7.0.x — unified REST + WebSocket Java server — lightweight, doesn't obscure socket lifecycle
- H2 2.4.240 (embedded) + JDBI 3.54.0 — persistence for accounts/matches — zero install, only 2-3 tables needed
- jBCrypt + JJWT 0.13.0 — password hashing + stateless session tokens — avoids pulling in full Spring Security
- React 19.2.x + TypeScript + Vite 8 — web frontend — Vite is the current standard (CRA is deprecated)
- HTML5 Canvas 2D (native, no game library) — rendering — sufficient for a simple top-down 2-tank arena
- Native browser `WebSocket` API + JSON messages — real-time transport — avoid Socket.IO (protocol mismatch with plain Java server)

### Expected Features

**Must have (table stakes):**
- Account register/login, session persistence
- 1v1 real-time match (movement + shooting sync)
- Tank HP with clear win/lose condition
- Map with walls/obstacles blocking movement and bullets
- In-match chat
- Leaderboard of win/loss records
- Disconnect/forfeit handling and a post-match result screen (implicit but necessary — added by research)

**Should have (competitive/polish):**
- Bullet ricochet off walls — highest-value differentiator, cheap once wall collision works, defines the genre's closest reference (Wii Play: Tanks!)
- Practice bot/AI opponent — solves solo-dev testing problem (no second human needed to test)
- Power-ups, multiple tank types, multiple maps

**Defer (v2+ / anti-features):**
- More than 2 players per match — large state-sync complexity jump
- Full client-side prediction + rollback netcode — textbook-correct but overkill for a low-latency 1v1 course demo; plain server-authoritative is standard and sufficient
- Progression/upgrade trees + currency — zero relationship to the networking learning objective, can consume the entire timeline
- Skill-based matchmaking, spectator mode, mobile app

### Architecture Approach

Authoritative-server pattern: the Java server owns `GameState`, runs a fixed-tick loop validating all input and broadcasting snapshots; the React client is a "dumb renderer" that never computes gameplay outcomes locally. This best fits both the real-time-fairness goal and the course's networking focus.

**Major components:**
1. Connection/Session Layer — WebSocket handshake, auth token validation, per-connection identity
2. Matchmaking/Room Manager — pairs two connected players into a match
3. Match/Game Loop — per-match authoritative simulation (movement, collision, HP, win condition) on a fixed tick
4. Persistence Layer — accounts, match results, leaderboard aggregates (H2 + JDBI)
5. Chat routing — independent of the tick loop, low risk
6. Client: Network Client (WebSocket wrapper) → Input Capture → Renderer (Canvas driven by `requestAnimationFrame`, not React `setState` per tick)

### Critical Pitfalls

1. **Client authority (trusting the browser)** — the protocol must carry only input intent (move/fire) from client to server; the server simulates outcomes (damage, position, win) and broadcasts truth. A forged WebSocket message from devtools must never be able to claim a hit or a win.
2. **No independent server game loop** — without a fixed-tick loop (e.g. `ScheduledExecutorService` ~20-30 Hz) advancing bullets/collisions/win-checks, physics only happens when a client happens to send a packet, which is irregular and exploitable.
3. **Threading races on shared match state** — `onMessage` runs on per-connection threads while the game loop runs on its own thread; without thread-confining match state (recommended: single-threaded executor per match) or `java.util.concurrent` structures, concurrent real players will eventually corrupt state — the classic "worked yesterday, crashed during the demo" bug.
4. **Testing only in two browser tabs on localhost** — hides real latency, disconnect handling, and genuinely concurrent input timing, exactly the conditions of the actual grading demo. Must test with two separate browser processes/machines with artificial latency.
5. **Over-building a generic network layer** — the temptation to build reusable transport abstractions "to look sophisticated" burns weeks of a short solo timeline without producing a playable game.

## Implications for Roadmap

Based on research, suggested phase structure (6 phases):

### Phase 1: Networking Foundation & Authority Model
**Rationale:** Front-loads the highest-risk, most novel work — the real-time sync foundation everything else depends on. Retrofitting the authority model or threading strategy later is expensive.
**Delivers:** Basic WebSocket connection, session layer, hardcoded 2-player room, server tick loop skeleton (fixed-rate ~20-30 Hz), locked message protocol (`{ type, payload }` envelope), locked authority model (server is source of truth, client sends inputs only). One tank moves on screen via a real WebSocket round-trip.
**Avoids:** Pitfalls 1-3 (client authority, missing game loop, threading races) by deciding and validating them here, not later.

### Phase 2: Real-Time Movement & Collision
**Rationale:** Core gameplay logic layered directly on the Phase 1 foundation — movement sync, collision, and bullets are architecturally intertwined and expensive to build separately.
**Delivers:** Input → movement sync, wall/obstacle collision, bullet spawn/movement, bullet↔wall and tank↔tank collisions, delta-time physics. Two tanks move, shoot, and collide with no desync between client views.
**Addresses:** Map/walls, movement/shoot sync features from FEATURES.md.
**Avoids:** Pitfall 4 (localhost-only testing) — should include UAT with two independent browser processes and artificial latency.

### Phase 3: Combat & Match End
**Rationale:** Builds win/lose rules and match lifecycle robustness on top of a working real-time loop.
**Delivers:** HP system, win condition, match-end broadcast, disconnect handling (`onClose`), post-match result screen.
**Uses:** Server-authoritative hit detection (server, not client, decides hits).
**Avoids:** Ungraceful disconnects breaking the app; inconsistent match-end messaging.

### Phase 4: Matchmaking, Accounts & Persistence
**Rationale:** Standard, well-documented plumbing (auth, queueing, SQL) — lower risk than Phases 1-3, can follow established patterns.
**Delivers:** Account register/login (JWT + jBCrypt), FIFO matchmaking queue, H2 database (accounts + match results), leaderboard aggregates, token validation at WebSocket handshake.
**Implements:** Persistence Layer and Connection/Session Layer auth pieces from ARCHITECTURE.md.

### Phase 5: Chat & Connection UX
**Rationale:** Low-risk, addable any time after Phase 2 exists; kept separate from the tick loop so it can't interfere with game-critical timing.
**Delivers:** In-match text chat routed via the connection layer (not the game loop), connection status indicator, input focus management so chat doesn't intercept game controls.

### Phase 6: Differentiators & Visual Polish
**Rationale:** Only worth doing once the core 1v1 loop is solid and gradeable; time-permitting scope.
**Delivers:** Bullet ricochet (highest value), power-ups, practice bot, multiple tank types, sound/visual effects, multiple maps.

### Phase Ordering Rationale

- Networking risk (Phases 1-2) is front-loaded before any routine CRUD work (Phase 4), because real-time state sync is the novel, hard-to-retrofit part — this directly counters the over-scoping pitfall.
- Combat rules (Phase 3) sit right after movement/collision (Phase 2) because HP/win-condition logic depends on collision already working.
- Auth/persistence/matchmaking (Phase 4) is deliberately placed after the real-time core works, since it's standard and lower-risk, and doesn't need to block gameplay validation.
- Chat (Phase 5) and polish (Phase 6) are last because they carry no networking-architecture risk and are the easiest to cut if time runs short.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 1:** Concurrency model choice (single-threaded executor vs. locks) and concurrent-input stress testing — foundational and hard to change later.
- **Phase 2:** Collision edge cases (bullet skip-through at high speed vs. tank size) and movement feel under real (non-zero) latency.
- **Phase 6:** AI bot heuristics and ricochet reflection math, if pursued.

Phases with standard patterns (skip research-phase):
- **Phase 3:** Server-authoritative hit detection and match-end handling are canonical, well-documented patterns.
- **Phase 4:** JWT + SQL auth/persistence/matchmaking queue are standard, widely-documented patterns.
- **Phase 5:** Chat routing is straightforward state-driven UI, no network-architecture risk.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM-HIGH | Package versions verified against official npm/Maven sources (HIGH per-item); framework choice (Javalin vs Spring vs raw) cross-verified across multiple sources but not benchmarked against this specific project (MEDIUM) |
| Features | HIGH | Genre conventions are stable and cross-checked against a clear reference (Wii Play: Tanks!, Battle City) plus this project's own already-decided requirements |
| Architecture | MEDIUM | Patterns are well-documented (Gabriel Gambetta's client-server netcode series, Valve Source engine docs, Nakama/Metaplay authoritative-server docs) but no official Java-specific game-server standard exists to cite as HIGH-confidence, and none of it is validated against this specific course's grading rubric |
| Pitfalls | HIGH | Game-networking pitfalls (authority, loop, threading) are grounded in stable, well-documented literature; course-project-scope pitfalls are inferred from this project's own constraints rather than externally sourced (still actionable, slightly lower certainty) |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **Concurrency validation:** Phase 1 planning must include explicit stress tests (both players spamming input simultaneously) to validate whatever threading model is chosen.
- **Collision edge cases:** Phase 2 planning should enumerate and test boundary conditions (bullet speed vs. tank size, wall corners) rather than assume simple AABB checks are sufficient.
- **Grading rubric alignment:** Not yet known whether the course's grading rubric has specific expectations (e.g., must show raw socket code, must demonstrate a specific protocol) — worth confirming before deep implementation, though PROJECT.md already notes raw sockets are not mandatory.
- **Real-network testing infrastructure:** Phases 2 onward need UAT with two independent browser processes and artificial latency (e.g. via Chrome DevTools network throttling), not just two tabs on localhost.
- **Auth/session integration:** Phase 4 planning should confirm exactly how JWT validation happens at the WebSocket handshake (query param vs. subprotocol vs. first-message auth) before implementation.
- **Match concurrency scale:** Whether the server needs to support multiple simultaneous matches (vs. one at a time) wasn't settled — affects whether "one global game loop" is an acceptable shortcut.

## Sources

### Primary (HIGH confidence)
- Official Javalin, React, Vite, H2, JDBI, JJWT, jBCrypt documentation and Maven Central/npm release notes (version and API verification)
- Gabriel Gambetta's client-server networking / prediction & reconciliation article series (industry-canonical reference for authoritative multiplayer architecture)

### Secondary (MEDIUM confidence)
- Valve Source Engine networking documentation, Nakama/Metaplay authoritative-server docs — architecture pattern cross-verification
- Baeldung and Ably Java WebSocket guides — concurrency/reconnection behavior in Java WebSocket servers
- General web search on tank-battle genre conventions (Wii Play: Tanks!, Battle City, Tanki Online) — feature landscape

### Tertiary (LOW confidence)
- Course-project-scope pitfalls (over-scoping, grading expectations) — inferred from this project's PROJECT.md constraints, not independently sourced; should be validated against actual course requirements if/when available

---
*Research completed: 2026-08-13*
*Ready for roadmap: yes*
