# Phase 2: Campaign Tactical Systems — Specification

**Created:** 2026-09-28  
**Ambiguity score:** 0.12  
**Requirements:** 13 locked

## Goal

Hoàn thiện inventory, consumable, tactical terrain, pickup, visibility và HUD của ba màn campaign theo các hành vi đã được người dùng xác nhận.

## Locked Decisions

- Start campaign: 100 Round, 3 Repair, 2 Speed.
- Carry all remaining counts across all three levels.
- Restart restores inventory and transient effects to level-start state.
- Repair heals to full; Speed is +30% for 20 seconds.
- Q pauses; Up/Down moves selection; Enter executes and closes/resumes; Q/Escape do not close.
- Gray/brick obstacle: 4 projectile hits; white obstacle: indestructible; green grass/bush: 1 projectile hit.
- Pickups: L1 Repair; L2 Repair + Speed; L3 two Repair + Speed.
- Visibility covers at least 2/3 of map cells, outside fully black, no exploration memory, enemies still visible.
- HUD is compact and left-aligned; reference asset source is Tankzors Builds.

## Boundaries

**In scope:** campaign core simulation, snapshots, input/menu, renderer/assets, three source maps, synchronized server map copies and tests.

**Out of scope:** co-op protocol/state sync, new Spring gameplay entities, random drops, additional consumables and generated bundle edits.

## Acceptance Criteria

- [ ] Inventory/consumable persistence and restart behavior pass deterministic unit tests.
- [ ] Q menu pauses simulation and consumes/selects only on Enter.
- [ ] Terrain hit counts, collision removal and pickup collection pass simulation tests.
- [ ] Each map contains its specified pickup counts and all map copies match.
- [ ] Visibility calculation includes at least two-thirds of map cells at center and edges.
- [ ] Renderer hides all non-enemy world content outside visibility and renders the compact HUD/menu.
- [ ] Full core tests, GWT `html:dist`, Spring tests and Docker multi-stage build pass.

## Prohibitions

- MUST NOT reset carried inventory during normal next-level transitions.
- MUST NOT consume Repair at full HP or stack Speed multipliers.
- MUST NOT allow AI/game simulation to advance while the Q menu is open.
- MUST NOT reveal terrain/pickups/projectiles outside the visibility region.
- MUST NOT edit generated JavaScript under Spring static resources.

---
*Next: execute 02-01-PLAN.md*
