---
status: testing
phase: 04-campaign-tactical-systems
source: [04-VERIFICATION.md]
started: 2026-09-28T11:05:00+07:00
updated: 2026-09-28T11:05:00+07:00
---

## Current Test

number: 1
name: HUD and Q inventory menu
expected: |
  After login and opening campaign_1, the left HUD shows HP, ammo, Repair=3 and Speed=2. Q pauses the battle; Up/Down changes the highlight; Q/Escape do not close; Enter selects/uses and resumes.
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
result: pending

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps

None reported yet.
