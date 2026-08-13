---
phase: 1
slug: networked-movement-skeleton
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-13
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (server-side `Match`/`GameState` unit tests) — none exists yet, greenfield. No client-side test framework needed this phase (rendering is visually verified via UAT). |
| **Config file** | none — Wave 0 installs (`server/pom.xml` needs the `junit-jupiter` dependency added) |
| **Quick run command** | `mvn -q -pl server test` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~10-30 seconds (small test suite, no client tests this phase) |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q -pl server test` (fast unit tests only)
- **After every plan wave:** Run `mvn test` (full suite, including the concurrency stress test)
- **Before `/gsd-verify-work`:** Full suite must be green, plus manual two-real-browser-process UAT (per project PITFALLS.md Pitfall 7 — never rely on two-tabs-on-localhost as the only verification)
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-TBD | 01 | 0 | COMBAT-01 | — | Server broadcasts identical state to both sessions every tick | integration (headless WS client) | `mvn -pl server test -Dtest=MatchBroadcastTest` | ❌ W0 | ⬜ pending |
| 01-01-TBD | 01 | 0 | COMBAT-01 | — | `onMessage` only processes `{type:"input"}`; a forged `{type:"state"}` message is ignored (server-authoritative) | unit | `mvn -pl server test -Dtest=InputDispatchTest` | ❌ W0 | ⬜ pending |
| 01-01-TBD | 01 | 0 | COMBAT-01 | — | `GameState.advance(dt)` applies zero velocity when no direction is held | unit | `mvn -pl server test -Dtest=GameStateTest` | ❌ W0 | ⬜ pending |
| 01-01-TBD | 01 | 0 | COMBAT-01 | — | Two headless WS clients sending concurrent input for a sustained period end with matching final positions (no drift/crash) | integration/stress | `mvn -pl server test -Dtest=ConcurrentInputStressTest` | ❌ W0 | ⬜ pending |
| 01-01-TBD | 01 | 0 | COMBAT-01 (success criteria 1-2) | — | Two real browsers see the same arena and live movement | manual (+ optional Playwright dual-context) | manual UAT or `npx playwright test movement.spec.ts` | ❌ W0 | ⬜ pending |

*Exact Task IDs filled in once PLAN.md is written by the planner — this table's rows are the required coverage set, not final IDs.*

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `server/pom.xml` — add JUnit 5 (`junit-jupiter`) test dependency
- [ ] `server/src/test/java/.../GameStateTest.java` — delta-time movement, no-input-stops-moving
- [ ] `server/src/test/java/.../InputDispatchTest.java` — server ignores non-input message types (server authority)
- [ ] `server/src/test/java/.../MatchBroadcastTest.java` — both sessions receive identical broadcast state
- [ ] `server/src/test/java/.../ConcurrentInputStressTest.java` — two headless WS clients, sustained concurrent input, no drift/crash
- [ ] Maven installation — not present on the dev machine per RESEARCH.md; must be resolved (apt install or IDE-bundled Maven) before any `mvn test` command can run

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Two separate real browser processes see the same arena and identical live tank movement | COMBAT-01 (success criteria 1, 2, 4) | Visual/UX confirmation and real-network timing can't be fully captured by headless unit tests; project PITFALLS.md explicitly warns against relying only on two-tabs-on-localhost | Start the Javalin server (`mvn -pl server exec:java` or equivalent) and the Vite dev server (`npm run dev`). Open two separate browser *processes* (not just tabs) pointed at the client, e.g. one normal window + one incognito/different profile, ideally with artificial latency added via browser devtools network throttling. Move both tanks simultaneously and confirm both windows show identical positions with no drift. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
