---
phase: 04-campaign-tactical-systems
verified: 2026-09-28T12:01:00+07:00
status: human_needed
score: 13/13 automated requirements verified
behavior_unverified: 3
---

# Phase 4: Campaign Tactical Systems Verification Report

**Phase Goal:** Campaign ba màn có inventory nhất quán, vật cản/pickup, tầm nhìn và HUD mới.  
**Status:** human_needed

## Automated Evidence

| Area | Status | Evidence |
|------|--------|----------|
| Inventory/menu/consumables | VERIFIED | PlayerShopState, GameSimulation, LocalCampaignSession and ClientInputProcessor tests |
| AI facing/projectiles | VERIFIED | Turn-only/shot-only regression tests; player and AI tanks both 60 px/s and projectiles both 200 px/s |
| Obstacles/pickups/maps | VERIFIED | Real player-shot 4-hit regression, indestructible brown-boundary regression, exact JSON count, byte-identity and enemy-route checks |
| Visibility/HUD buildability | VERIFIED | VisibilityMaskTest, visible impact-effect regression, 79 core tests and GWT compile |
| Backend/integration | VERIFIED | 54 Spring tests, Docker multi-stage build and healthy deployed container |

## Requirements Coverage

All INV-01..05, MAP-01..04, VIS-01..02 and UI-01..02 have implementation and automated evidence.

## Human Verification Required

1. In an authenticated campaign, confirm the compact HUD icons/counts and Q menu layout match the intended visual reference.
2. Confirm fog coverage feels like at least two-thirds of the map, hidden terrain is fully black and enemies remain visible outside it.
3. Play through/restart levels to confirm obstacle hit feel, pickups and cross-level inventory behavior end-to-end.

## Gaps Summary

No automated blocker. Browser automation could load the public play shell and compiled canvas, but authenticated bootstrap correctly redirected the unauthenticated test tab; visual gameplay UAT therefore remains manual.

---
*Verifier: Codex inline verification*
