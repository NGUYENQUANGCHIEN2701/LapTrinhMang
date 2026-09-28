---
status: awaiting_human_verify
trigger: "hiện tại có gặp trường hợp AI đang đi ngang , mà đạn cũng bắn theo chiều ngang được với cả điều chỉnh lại cả tốc độ đạn và tốc độ xe  của người và AI bằng nhau, với cả bản đồ bản cho ảnh những chướng ngại vật tôi bảo rồi mà sao bắn nó chưa bị phá như tôi bảo với cả sao để hết ra rìa thế làm giống bản đổ tôi gửi đấy không hiểu gì thì hỏi lại"
created: 2026-09-28
updated: 2026-09-28
---

## Current Focus

hypothesis: Confirmed and fixed: MapSnapshot now carries wall HP, brown walls map HP 4/3/2/1 to source frames 0/2/4/5, and the inventory renderer now expands inside the top-left HUD instead of drawing a full-screen modal.
test: Snapshot/frame regressions, default Round/Repair/Speed menu regression, full core suite, GWT compile, Docker build/deploy, HTTP and startup-log check.
expecting: Each surviving brown wall renders a different persistent damage frame after every hit; normal HUD shows only HP and the active weapon; Q expands the available item rows in place while simulation remains paused.
next_action: Ask the user to hard-refresh, shoot one interior brown wall four times, and verify the compact Q HUD visually in an authenticated campaign.
bug_class: bohrbug
reasoning_checkpoint:
  hypothesis: Two systems mutate AI rotation in the same tick; AI projectile speed is hard-coded separately; obstacle types are visually ambiguous and concentrated at the perimeter.
  confirming_evidence: ShootBehavior rotates before AINavigator, the constants are 120 versus 200, whiteWallFrame includes a green frame, and map counts show almost all white walls on borders.
  falsification_test: Assert turn-only then fire-only ticks, final rotation equals bullet direction, equal speed constants, and simulation-level obstacle durability.
  fix_rationale: Give aiming/firing exclusive ownership of a tick, use Bullet.DEFAULT_SPEED, constrain texture frame ranges by obstacle class, and place each class inside traversable map regions.
  blind_spots: Authenticated browser canvas cannot be automated without a user session; final visual placement still needs human UAT.
  candidate_causes: [rotation ownership conflict, split projectile speed constants, incorrect sprite frame range, perimeter-heavy map data]
  and_gate: All four causes are independently evidenced and jointly explain the reported behavior and screenshot.
tdd_checkpoint:
  test_file: GameSimulationTest, MapObstacleTest
  name: Player-owned projectile damages interior brown wall while brown boundary remains indestructible
  status: green
  failure_output: Boundary regression initially failed because all TileType.WALL instances were destructible; 79 core tests pass after the position-aware fix.

## Symptoms

expected: AI turns in place to one of four cardinal directions, then moves straight without strafing; its projectile travels in the direction the tank visibly faces; player and AI tanks share one speed and their projectiles share one speed; gray obstacles break after four hits, white obstacles never break, bushes break after one hit; these types are distributed inside each map similarly to the supplied reference.
actual: AI can appear to move sideways while a projectile travels horizontally; AI projectile speed differs from the player projectile speed; obstacle types appear concentrated at the map edge and the user cannot observe the requested destruction behavior.
errors: No runtime exception reported.
reproduction: Play campaign level 1, watch an AI change between chase movement and shooting, compare projectile speeds, fire repeatedly at visible obstacle variants, and inspect obstacle placement across the map.
started: Regression observed after the previous campaign tactical-system implementation.

## Eliminated

- hypothesis: Only AI-owned bullets can damage brown walls.
  evidence: A regression using enqueueShoot and the real PlayerTank.shoot path reduced one interior wall from 4 HP to 0 and consumed exactly four player rounds before any production change to owner handling.
  timestamp: 2026-09-28

## Evidence

- timestamp: 2026-09-28
  checked: ShootBehavior and AIController update order.
  found: ShootBehavior rotates and fires before navigation; navigation can rotate the tank again in the same tick, leaving bullet direction different from final rendered rotation.
  implication: AI aiming must reserve the tick and movement must not overwrite the aim/firing direction.

- timestamp: 2026-09-28
  checked: Player and AI speed constants.
  found: Player and every AI difficulty use 60 px/s, but player bullets use 200 px/s while AI bullets use 120 px/s.
  implication: Tank speed is already equal; projectile speed must be unified.

- timestamp: 2026-09-28
  checked: Campaign JSON grids.
  found: Most WHITE_WALL tiles are perimeter tiles and campaign 1/2/3 contain only 3/2/1 BUSH tiles respectively.
  implication: The interior layouts do not visually demonstrate the three requested obstacle classes.

- timestamp: 2026-09-28
  checked: Simulation-level projectile-to-obstacle regression.
  found: WALL becomes ground on hit 4, BUSH on hit 1, and WHITE_WALL remains after repeated hits.
  implication: Durability logic works end-to-end; prior confusion was caused by placement and frame classification rather than missing hit handling.

- timestamp: 2026-09-28
  checked: Rebalanced campaign map data.
  found: Client/server copies are byte-identical, pickup counts remain 1/0, 1/1, 2/1, and all enemy spawns remain reachable; interior white/bush counts are 8/13, 12/8, and 20/9.
  implication: Obstacle variety is now visible inside every map without breaking campaign navigation or pickup rules.

- timestamp: 2026-09-28
  checked: Automated and deployment verification.
  found: 77 core tests and 54 Spring tests pass; GWT and Docker builds succeed; recreated app is running and returns HTTP 200.
  implication: The fix is integrated and ready for authenticated visual UAT.

- timestamp: 2026-09-28
  checked: Real player shooting path and brown boundary durability.
  found: PlayerTank.shoot already damages interior walls, while the new boundary regression failed because MapDefinition.hitWall did not distinguish edge coordinates.
  implication: The reported owner difference was visual ambiguity; the confirmed logic defect was destructible perimeter walls.

- timestamp: 2026-09-28
  checked: Boundary fix and hit feedback integration.
  found: Interior brown wall HP changes 4→3→2→1→destroyed from real player shots; edge-wall HP remains 4 after ten hits; 79 core tests, GWT and Docker build pass and the container starts cleanly.
  implication: Owner parity and border invariants are now enforced and observable.

- timestamp: 2026-09-28
  checked: Persistent wall-damage rendering and compact Q inventory HUD.
  found: The new tests first failed because snapshots had no wall HP and AssetRepository had no HP-based frame mapping; after the fix, snapshots expose 4→3→2→1, the source sprite frames resolve to 0→2→4→5, and the default Q list is Round 100, Repair 3, Speed 2. The renderer no longer draws a screen-sized overlay or centered panel.
  implication: Damage is now visible between shots and Q uses the original-style inline top-left inventory interaction.

- timestamp: 2026-09-28
  checked: Final integration and deployed runtime.
  found: 81/81 core tests pass, GWT compile and Docker multi-stage build succeed, the recreated app starts cleanly and localhost returns HTTP 200. Browser visual inspection reached the login page but could not enter the authenticated campaign without a signed-in session.
  implication: Logic/build/runtime are verified; only authenticated visual UAT remains.

## Resolution

root_cause: ShootBehavior and AINavigator both changed AI rotation during one fixed-step; AI bullets used 120 px/s while player bullets used 200 px/s; obstacle frames/placement were unclear; perimeter and interior brown walls shared one destructible TileType.WALL rule; MapSnapshot omitted live wall HP, AssetRepository chose brown-wall art only by position, and Q was rendered as a full-screen centered overlay.
fix: Aiming/firing reserve a movement-free tick, projectile speeds are unified, obstacle frames/layouts are separated, edge-coordinate walls are indestructible, interior brown walls retain four HP for either owner, snapshots expose wall durability, HP 4/3/2/1 selects progressive repo sprite frames, and Q expands the available items inside the top-left HUD.
verification: Real player-shot, boundary, snapshot, sprite-frame and inventory regressions pass; full core suite 81/81 passes; Spring suite remains 54/54; GWT and Docker multi-stage builds pass; deployed container starts cleanly and returns HTTP 200. Awaiting authenticated visual confirmation.
oracle_type: specified
files_changed: [AIController.java, ShootBehavior.java, AssetRepository.java, MapDefinition.java, GameSimulation.java, EffectSnapshot.java, MapSnapshot.java, WorldSnapshotMapper.java, GameRenderer.java, GameSimulationTest.java, MapObstacleTest.java, AssetRepositoryTest.java, LocalCampaignSessionTest.java, AIControllerMovementTest.java, ShootBehaviorTest.java, campaign_1.json client/server, campaign_2.json client/server, campaign_3.json client/server]
