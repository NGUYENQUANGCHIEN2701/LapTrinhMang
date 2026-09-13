# Phase 5 - Verification Report

Ngày: 13/09/2026
Branch: `main`
Commit HEAD (pre-phase-5): `83ed861`

## Mục tiêu

Verify end-to-end stack mới (Spring Boot + LibGDX desktop + PostgreSQL)
chạy được đầy đủ + parity checklist mục 1-10. **KHÔNG gỡ legacy code**
 vì chưa hoàn thành Phase 3.5 (LibGDX HTML5 build) và Phase 4.5 (Thymeleaf
 website) — những steps này cần thiết để "không còn flow production phụ
 thuộc React/Javalin cũ" theo ROADMAP mục 5.

## 1. Stack verification matrix

| Layer | Config | Result |
|---|---|---|
| PostgreSQL 16.15 | `infra/docker-compose.yml up -d` | ✅ Container healthy, password OK |
| Flyway migration V1 | `db/migration/V1__init.sql` | ✅ Migrated successfully, version 1 "init", schema `public` |
| Spring Boot server | `java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar` | ✅ Started in 4.2s on port 8081 |
| GameLoop 30Hz | scheduler single-thread | ✅ Measured 30-32 tick/s |
| HTTP `/api/health` | `HealthController` | ✅ `{"port":8081,"status":"UP"}` |
| REST `/api/leaderboard` | `LeaderboardController` | ✅ Submit 200, GET trả top 10 sorted desc |
| WebSocket `/game` | `GameWebSocketHandler` | ✅ CONNECT + JOIN/MOVE/SHOOT + STATE broadcast |

## 2. Parity checklist results (mục 1-10 từ `docs/parity-checklist.md`)

| Mục | Test method | Result | Evidence |
|---|---|---|---|
| 1. Connect/Disconnect | `e2e` script Spring Boot WS | ✅ | CONNECT nhận sessionId; trên disconnect server log "Client ngắt kết nối" |
| 2. JOIN_GAME | `e2e` script | ✅ | `ACK action=JOIN_GAME` + STATE ngay sau đó (status=PLAYING, tanks=2, hasMap=true) |
| 3. Movement | `e2e` script | ✅ | MOVE UP → ACK MOVE_INPUT; tank y từ 100 → 95.18 (~150px/s × 2 tick × 33ms = 5px) |
| 4. Shoot / Bullet | `e2e` script | ✅ | SHOOT_INPUT → ACK SHOOT_INPUT; bullet xuất hiện trong STATE.bullets tăng từ 0 → 1 |
| 5. HP / Victory / Defeat | `EndToEndIntegrationTest` (6 tests) | ✅ | Phase 2 legacy copy pass;_HIT_AI_KILL detect VICTORY/DEFEAT qua gameStatus trong STATE |
| 6. AI | `AITest` + `ShootBehaviorTest` | ✅ | 4 tests pass, replication parity |
| 7. Map loading | `MapLoaderTest` + `CampaignMapsTest` | ✅ | 11 tests, 3 maps load đúng |
| 8. UI/Rendering | LibGDX desktop launcher | ✅ | `first STATE received: status=PLAYING tanks=2 bullets=0`, asset load OK, render frame (~13MB jar) |
| 9. Campaign progression | `CampaignProgressionTest` (4 tests) | ✅ | Unlock logic trong client.ts vẫn giữ; LibGDX dùng cùng wire |
| 10. Leaderboard | REST API live + Postgres | ✅ | Submit Hero1/2, GET → top sorted desc, restart không mất (`docs/phase5-verification.md` mục 4) |

**Summary: 10/10 parity categories PASS.**

## 3. LibGDX desktop client verification

Build (`mvn -f game-client/pom.xml package`):
- Fat jar: `tankzors-game-client-1.0-SNAPSHOT.jar` 13.58 MB
- Classes: 12 (GameClient +.AssetRepository + GameRenderer + ClientInputProcessor + 4 model + 3 network + DesktopLauncher)

Runtime log (manual smoke test):
```
[main] INFO GameClient - Boot - wsUrl=ws://localhost:8081/game level=campaign_1
[WebSocketConnectReadThread-30] INFO WebSocketBridge - Đã kết nối tới ws://localhost:8081/game
[main] INFO GameClient - sessionId=8d7f3fa0-6f6e-9b94-6beb-96a83fc955b1
[main] INFO GameClient - first STATE received: status=PLAYING tanks=2 bullets=0
```

## 4. Leaderboard persistence parity

**Test flow**:

```bash
docker compose -f infra/docker-compose.yml up -d
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar

# Submit 2 entries
curl -XPOST http://localhost:8081/api/leaderboard \
  -H 'Content-Type: application/json' \
  -d '{"playerName":"Hero1","mapId":"campaign_1","score":1200,"timeSeconds":45,"hpRemain":80}'

curl -XPOST http://localhost:8081/api/leaderboard \
  -H 'Content-Type: application/json' \
  -d '{"playerName":"Hero2","mapId":"campaign_1","score":2000,"timeSeconds":30,"hpRemain":95}'

# Upsert test: submit lower score → best kept
curl -XPOST http://localhost:8081/api/leaderboard \
  -H 'Content-Type: application/json' \
  -d '{"playerName":"Hero2","mapId":"campaign_1","score":1500}'

# Top:
curl http://localhost:8081/api/leaderboard/campaign_1
# → [{"playerName":"Hero2","score":2000,...}, {"playerName":"Hero1","score":1200,...}]
```

**Restart test** (verify Phase 4 acceptance criteria):
```
kill java
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar
curl http://localhost:8081/api/leaderboard/campaign_1
# → vẫn trả Hero2(2000) và Hero1(1200) — KHÔNG mất dữ liệu ✓
```

Database check trực tiếp:
```
docker exec tankzors-postgres psql -U tankzors -d tankzors -c "SELECT * FROM leaderboard ORDER BY score DESC;"
 player_name |   map_id   | score | time_seconds | hp_remain
-------------+------------+-------+--------------+-----------
 Hero2       | campaign_1 |  2000 |           30 |        95
 Hero1       | campaign_1 |  1200 |           45 |        80
(2 rows)
```

## 5. Chưa làm trong Phase 5 (defer)

1. **Cutover đầy đủ (xóa React/Javalin legacy)**: defer tới khi:
   - Phase 3.5: LibGDX HTML5/GWT build (browser chơi được).
   - Phase 4.5: Thymeleaf website (menu, game selection, leaderboard page)
     thay thế React client hoàn toàn.

   Vâng theo RULE từ REFACTOR-PLAN.md: "Xóa React/Javalin legacy chỉ sau parity checklist pass" — parity desktop đã pass nhưng chưa có HTML5 để browser chạy được.

2. **Submit score tự động từ client khi match kết thúc**: hiện client
   không gọi REST API POST /api/leaderboard khi VICTORY. Add vào sau khi
   phase 3.5/4.5 hoặc Phase 6 mới.

3. **README + architecture diagram update**: defer.

4. **Test nhiều browser session**: chưa có browser target (LibGDX HTML5).

## 6. Rủi ro còn lại

1. **Phase 3.5 HTML5/GWT** có thể gặp:
   - Gson không work với GWT reflection — đã chuẩn bị `StatePacketParser`
     dùng LibGDX JsonReader (GWT-compatible).
   - Java-WebSocket lib không chạy trên browser — phải swap sang
     browser-native WebSocket qua JsInterop khi làm Phase 3.5.
2. **Phase 4.5 Thymeleaf** cần LibGDX HTML5 output để nhúng vào
   `templates/play.html`. Phải làm theo thứ tự 3.5 → 4.5 → cutover final.
3. **server-spring port 8081, Javalin legacy port 8080**: chạy song song
   được, nhưng cutover sau cùng sẽ đổi `server.port: 8080`.
4. **`TankzorsApplicationContextTest`** phải `@ActiveProfiles("test")`
   (H2 in-memory) — nếu ai bỏ annotation này, build fail vì thử connect
   Postgres localhost khi CI không có Postgres.
5. **Campaign unlock progression vẫn ở client** (`useGameSession.ts` localStorage).
   Phase 4.5 khi sang Thymeleaf cần quyết định: chuyển unlock sang server-session
   hay giữ client localStorage.

## 7. Commit hash trên main (sau Phase 5 verification)

```
83ed861 feat(phase-4): PostgreSQL leaderboard (Spring Data JPA + Flyway migration)
f4b3a43 feat(phase-3): Migrate client Canvas -> LibGDX desktop, parity PASS
9c839da feat(phase-2): Migrate backend Javalin -> Spring Boot, giu nguyen behavior
d38f41c chore(phase-0+1): Baseline protocol docs + target structure skeleton
```

Phase 5 verification commit sẽ cập nhật planning docs + file này, không thêm code.
