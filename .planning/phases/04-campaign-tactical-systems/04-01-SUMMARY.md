---
phase: 04-campaign-tactical-systems
plan: "01"
subsystem: gameplay-ui
tags: [libgdx, gwt, campaign, inventory, fog-of-war, destructible-terrain]
requires:
  - phase: 01-architectural-cleanup
    provides: Client-authoritative campaign engine and Docker build path
provides:
  - Persistent three-level campaign inventory with level-start restart checkpoints
  - Repair and timed Speed consumables selected through a paused Q menu
  - Destructible obstacles, fixed pickups, visibility masking and compact HUD
affects: [co-op, campaign-maps, renderer, shop-state]
actuals:
  tokens: 9000
  tasks: 3
  commits: 1
tech-stack:
  added: []
  patterns: [simulation-owned transient effects, snapshot-only renderer, synchronized map sources]
key-files:
  created:
    - game-client/core/src/main/java/com/tankzors/client/render/VisibilityMask.java
    - game-client/core/src/main/java/com/tankzors/client/model/EffectSnapshot.java
  modified:
    - game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java
    - game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java
    - game-client/core/src/main/java/com/tankzors/client/render/GameRenderer.java
    - game-client/core/src/main/java/com/tankzors/client/gameplay/map/MapDefinition.java
key-decisions:
  - "Restart restores only the level-start inventory checkpoint; coins and research remain persistent account state."
  - "Visibility radius is adaptive and recalculated from current player position to guarantee at least two-thirds coverage without exploration memory."
patterns-established:
  - "Gameplay state mutates only in core simulation/session; renderer consumes snapshots."
  - "Client and Spring map JSON copies remain byte-identical."
requirements-completed: [INV-01, INV-02, INV-03, INV-04, INV-05, MAP-01, MAP-02, MAP-03, MAP-04, VIS-01, VIS-02, UI-01, UI-02]
coverage:
  - id: D1
    description: Persistent inventory, Repair, Speed, Q menu pause and restart checkpoint
    requirement: INV-01
    verification:
      - kind: unit
        ref: game-client/core/src/test/java/com/tankzors/client/session/LocalCampaignSessionTest.java
        status: pass
      - kind: unit
        ref: game-client/core/src/test/java/com/tankzors/client/gameplay/GameSimulationTest.java
        status: pass
    human_judgment: false
  - id: D2
    description: Three obstacle types and exact fixed pickup placement across all maps
    requirement: MAP-01
    verification:
      - kind: unit
        ref: game-client/core/src/test/java/com/tankzors/client/gameplay/map/MapObstacleTest.java
        status: pass
      - kind: integration
        ref: PowerShell JSON count and byte-identity check
        status: pass
    human_judgment: true
    rationale: Visual scale, collision feel and pickup placement require playtesting.
  - id: D3
    description: Adaptive two-thirds visibility, always-visible enemies, compact HUD and Q overlay
    requirement: VIS-01
    verification:
      - kind: unit
        ref: game-client/core/src/test/java/com/tankzors/client/render/VisibilityMaskTest.java
        status: pass
      - kind: integration
        ref: Gradle html:compileGwt and Docker html:dist
        status: pass
    human_judgment: true
    rationale: Canvas appearance and keyboard feel require an authenticated browser session.
duration: 2h
completed: 2026-09-28
status: complete
---

# Phase 4 Plan 01: Campaign Tactical Systems Summary

**Campaign now carries ammunition and consumables across levels, supports tactical pickups/terrain and renders a compact visibility-limited battlefield.**

## Accomplishments

- Added 100 Round, 3 Repair and 2 Speed defaults, cross-level carry, level-start restart snapshots, full repair and +30%/20-second speed.
- Added the paused Q inventory menu, exact fixed pickups, 4-hit brick, indestructible white wall, 1-hit bush and repository-native effects/audio.
- Added adaptive visibility covering at least two-thirds of map cells, enemy exception, compact left HUD and synchronized three-level maps.

## Task Commits

1. **Tasks 1-3: Campaign tactical systems and tests** - `63e0dda`

## Verification

- 75 LibGDX core tests: pass.
- GWT `html:compileGwt`: pass.
- 54 Spring tests in Maven JDK 21 container: pass.
- Docker multi-stage app image: pass; deployed container healthy on port 8081.
- Map pickup counts: L1 R1/S0, L2 R1/S1, L3 R2/S1; client/server copies byte-identical.

## Deviations from Plan

- Consolidated the three tightly coupled tasks into one commit because the worktree already contained the immediately preceding campaign movement/ammo fixes from the same user request.
- Browser visual inspection stopped at authenticated bootstrap; no credentials were used automatically. Human UAT remains recorded separately.

## Self-Check

PASSED — source, tests, GWT and Docker artifacts verified; generated Spring LibGDX bundle was not edited directly.
