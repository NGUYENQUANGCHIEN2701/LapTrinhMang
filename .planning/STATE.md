# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-09-28)

**Core value:** Campaign ba level chạy hoàn chỉnh với tài nguyên và gameplay nhất quán xuyên màn.  
**Current focus:** Phase 4 — Campaign Tactical Systems

## Current Position

- Phase: 4
- Status: Implementation complete; human UAT pending
- Previous verified phase: Phase 1 Architectural Cleanup
- Active plan: `.planning/phases/04-campaign-tactical-systems/04-01-PLAN.md`

## Decisions

- Default campaign inventory: 100 rounds, 3 Repair, 2 Speed.
- Repair heals to full; Speed gives +30% for 20 seconds.
- Inventory persists across levels; restart restores the level-start snapshot.
- Q pauses gameplay; Enter selects/uses and closes the menu.
- Adaptive light region covers at least 2/3 of map tiles; outside is fully black; enemies remain visible.
- Fixed pickups: L1 R1, L2 R1/S1, L3 R2/S1.
- Reference assets come from Tankzors Builds.

## Blockers/Concerns

- Current worktree contains uncommitted changes from the immediately preceding user-requested campaign fixes; preserve and integrate them.
- Map JSON source must remain synchronized between `game-client/assets/maps` and `server-spring/src/main/resources/maps`.
- Co-op support for new mechanics is explicitly deferred.
- Authenticated canvas UAT must be completed by the user; automated browser session was intentionally not logged in.

## Next Action

Run the three checks in `.planning/phases/04-campaign-tactical-systems/04-UAT.md` on `http://localhost:8081`.

---
*Last activity: 2026-09-28 — implementation/build verification complete; awaiting gameplay UAT*
