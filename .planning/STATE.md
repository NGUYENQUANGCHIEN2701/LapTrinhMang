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
- `campaign_1` is a capture-the-flag level: the player wins by reaching the flag; both sides respawn at their fixed spawn with full HP while inventory remains unchanged.

## Blockers/Concerns

- Map JSON source must remain synchronized between `game-client/assets/maps` and `server-spring/src/main/resources/maps`.
- Co-op support for new mechanics is explicitly deferred.
- Authenticated canvas UAT must be completed by the user; automated browser session was intentionally not logged in.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|---|---|---|---|
| 260928-n9l | Thiết kế lại campaign_1 thành màn cướp cờ có hồi sinh tại spawn cố định | 2026-09-28 | 1441af2 | [260928-n9l-thi-t-k-l-i-campaign-1-th-nh-m-n-c-p-c-n](./quick/260928-n9l-thi-t-k-l-i-campaign-1-th-nh-m-n-c-p-c-n/) |

## Next Action

Run the three checks in `.planning/phases/04-campaign-tactical-systems/04-UAT.md` on `http://localhost:8081`.

---
*Last activity: 2026-09-28 — campaign_1 capture-the-flag implementation verified and deployed; awaiting gameplay UAT*
