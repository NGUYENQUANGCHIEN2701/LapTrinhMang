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
| 261006-g29 | Sửa hướng cây, giao diện spawn gốc, giữ Space bắn liên tục và thanh máu theo phe | 2026-10-06 | 11c50f4 | [261006-g29-s-a-h-ng-c-y-giao-di-n-i-m-xu-t-ph-t-g-c](./quick/261006-g29-s-a-h-ng-c-y-giao-di-n-i-m-xu-t-ph-t-g-c/) |
| 261006-gt2 | Vật phẩm Repair và Speed nhặt trên bản đồ được sử dụng ngay thay vì cộng vào kho dự trữ | 2026-10-06 | 7be3904 | [261006-gt2-v-t-ph-m-repair-v-speed-nh-t-tr-n-b-n-c-](./quick/261006-gt2-v-t-ph-m-repair-v-speed-nh-t-tr-n-b-n-c-/) |
| 261006-gz8 | Sửa sprite cờ trong campaign hiển thị đúng chiều hướng lên trên | 2026-10-06 | 3f39d55 | [261006-gz8-s-a-sprite-c-trong-campaign-hi-n-th-ng-c](./quick/261006-gz8-s-a-sprite-c-trong-campaign-hi-n-th-ng-c/) |

## Next Action

Run the three checks in `.planning/phases/04-campaign-tactical-systems/04-UAT.md` on `http://localhost:8081`.

---
*Last activity: 2026-10-06 — campaign flag sprite corrected for the y-down camera; Docker build deployed for gameplay UAT*
