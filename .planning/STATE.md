# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-09-28)

**Core value:** Campaign ba level chạy hoàn chỉnh với tài nguyên và gameplay nhất quán xuyên màn.  
**Current focus:** Phase 2 — Persistent Arsenal, Battlefield Objects and Visibility

## Current Position

- Phase: 2
- Status: Planning complete; implementation pending
- Previous verified phase: Phase 1 Architectural Cleanup
- Active plan: `.planning/phases/02-campaign-tactical-systems/02-01-PLAN.md`

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

## Next Action

Execute `.planning/phases/02-campaign-tactical-systems/02-01-PLAN.md` sequentially with tests.

---
*Last activity: 2026-09-28 — planning restored for campaign tactical systems*
