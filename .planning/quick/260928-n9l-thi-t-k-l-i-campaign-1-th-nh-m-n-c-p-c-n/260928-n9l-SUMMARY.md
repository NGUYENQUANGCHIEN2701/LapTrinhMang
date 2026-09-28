---
quick_id: 260928-n9l
status: complete
completed: 2026-09-28
commit: 1441af2
---

# Quick Task 260928-n9l Summary

## Outcome

`campaign_1` is now a capture-the-flag level based on the supplied Tankzors layout. The player wins only by reaching the yellow flag. Player and AI deaths no longer end this level: each tank respawns immediately with full HP at its fixed green/red spawn, and player inventory is preserved.

## Changes

- Added a flag map tile and original flag/spawn artwork sourced from Tankzors Builds.
- Added capture-goal detection and fixed-spawn respawning to `GameSimulation` without changing the terminal rules of levels 2 and 3.
- Rebuilt and synchronized both `campaign_1.json` sources with one player spawn, one AI spawn, one flag, and reachable routes.
- Added regression coverage for victory, respawn, inventory preservation, map markers, and route reachability.

## Verification

- Core: 82 tests passed.
- Spring: 54 tests passed.
- GWT HTML compilation passed.
- Docker multi-stage build passed.
- Deployed container responds HTTP 200 on `http://localhost:8081/`.

## Commit

- `1441af2 feat(campaign): turn level one into capture the flag`
