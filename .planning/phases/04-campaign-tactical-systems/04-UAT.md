---
status: testing
phase: 04-campaign-tactical-systems
source: [04-VERIFICATION.md]
started: 2026-09-28T11:05:00+07:00
updated: 2026-09-28T12:01:00+07:00
---

## Current Test

number: 3
name: AI facing, obstacle durability and interior placement retest
expected: |
  AI turns in place before moving or firing, never strafes, and its bullet matches the rendered facing. Player/AI tanks both use 60 px/s and both projectile types use 200 px/s. Both owners damage interior brown walls, which break on hit 4 with visible impact feedback; brown perimeter walls and white walls never break; bushes break on hit 1.
awaiting: user response

## Tests

### 1. HUD and Q inventory menu
expected: Compact HUD and paused menu behave and look correct.
result: pending

### 2. Fog and enemy exception
expected: At least two-thirds of map terrain is visible; outside is black; living enemies remain visible there.
result: pending

### 3. Obstacles, pickups and carry/restart
expected: Brick takes 4 hits, white never breaks, bush takes 1; pickup counts and cross-level/restart inventory match the locked rules.
result: issue_fixed_awaiting_retest

## Summary

total: 3
passed: 0
issues: 1
pending: 2
skipped: 0
blocked: 0

## Gaps

- User found AI facing/projectile mismatch, unclear obstacle variants, and destructible-looking perimeter behavior. Fixed with exclusive AI aim ticks, equal 200 px/s projectile speeds, corrected sprite/layout ranges, indestructible edge-coordinate walls, real-player-shot regression coverage, and visible hit feedback; awaiting visual retest in authenticated campaign.
