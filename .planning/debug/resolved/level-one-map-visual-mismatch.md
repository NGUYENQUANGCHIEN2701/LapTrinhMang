---
status: resolved
trigger: "Level 1 rendered differently from the original Tankzors map"
created: 2026-10-06
updated: 2026-10-06
resolved: 2026-10-06
---

# Debug Session: Level 1 map visual mismatch

## Symptoms

- Expected: Campaign 1 should preserve the original Tankzors Level 1 layout and tile appearance.
- Actual: Gameplay positions broadly matched, but terrain decoration, white walls and bushes used incorrect sprites, making the map look substantially different.
- Runtime errors: None.

## Evidence

- Converting the original `maps/en/000.json` codes into the gameplay grid produced zero categorical mismatches.
- `AssetRepository` loaded only the first five 20px frames from `ground.png`, discarding the original terrain variation IDs.
- White-wall fallback selected wall frames 7 through 11 instead of original IDs 9 and 10.
- Bush fallback selected wall frames 12 through 18, which included cactus and unrelated sprites; the original map uses bush IDs 16 through 18.
- The simplified gameplay grid collapsed legacy source IDs such as terrain 1..90, walls 130..139, bush 145, flag 115 and repair 164.

## Eliminated

- The Level 1 row/column layout was not the root cause: its gameplay-category conversion matched the source map.
- Camera dimensions were not the source of the wrong wall and vegetation art.
- Fog of war was retained because it is an intentional gameplay requirement, not the sprite mismatch.

## Resolution

- Root cause: Original visual tile IDs were discarded during map conversion, and several atlas frame ranges were mapped incorrectly.
- Fix: Added an optional `visualGrid` containing the exact original Level 1 codes, propagated it through local/network snapshots, and rendered exact legacy ground/wall frames while retaining the gameplay grid for collision and destruction state. Corrected white-wall and bush frame selection and loaded the full ground atlas.
- Verification:
  - Client and server campaign map resources have identical SHA-256 hashes.
  - New legacy tile mapping, map visual-grid and asset-frame tests pass.
  - Full `:core:test` passes.
  - Spring `MapLoaderTest` passes under JDK 21.
  - Docker multi-stage build succeeds with GWT/JDK 17 and Spring/JDK 21.
  - Rebuilt container runs the new image and serves `/game/libgdx/index.html` with cache tag `campaign-map-visual-v3` (HTTP 200).
- Files changed: Campaign 1 map resources, map/snapshot DTOs and loaders, asset repository, renderer, guest snapshot parser, cache tag, and focused tests.
