# Runbook

Hướng dẫn build, run, troubleshoot Tankzors Pro. Tài liệu chính của repo.

## 1. Yêu cầu môi trường

| Tool | Version | Lý do |
|---|---|---|
| **JDK** | 17+ (test 17 và 21) | Spring Boot + game-client |
| **Maven** | 3.9+ | Build server-spring, server, shared-protocol |
| **Docker Desktop** | 4.20+ | Chạy PostgreSQL 16 qua docker compose |
| **Node.js** | 18+ | Build React client (`npm`) |
| **Git** | 2.40+ | Để clone & commit |
| **Browser** | bất kỳ hiện | Thymeleaf website |

### Lưu ý đặc thù

- **Maven wrapper**: Repo có `mvnw` (bash) ở root, nhưng `.mvn/install/apache-maven-3.9.9-bin.tar.gz` chưa được commit. Run:
  ```bash
  # Option 1: cài Maven system-wide (khuyến nghị)
  # Option 2: chạy qua WSL + mvnw
  sed 's/\r$//' mvnw > /tmp/mvnw_fixed && bash /tmp/mvnw_fixed -version
  ```

- **Java**: kiểm tra `java -version`. Game-client yêu cầu Java 17 (do LibGDX 1.12.1), server-spring có thể chạy Java 21.

- **Docker**: kiểm tra `docker info`. Nếu daemon chưa start, chạy Docker Desktop trước.

## 2. Quick start (môi trường dev)

### Bước 1: Khởi động PostgreSQL

```bash
docker compose -f infra/docker-compose.yml up -d

# Verify
docker compose -f infra/docker-compose.yml ps
# tankzors-postgres   Up   (healthy)   0.0.0.0:5432->5432/tcp
```

Connection params mặc định:

| Param | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| DB | `tankzors` |
| User | `tankzors` |
| Pass | `tankzors_dev_password` |

Override qua env: `DB_URL`, `DB_USER`, `DB_PASSWORD` (xem `application.yml`).

### Bước 2: Build server-spring

```bash
mvn -f shared-protocol/pom.xml install              # nếu có thay đổi DTO
mvn -f server-spring/pom.xml clean package -DskipTests
```

Output fat jar: `server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar` (~50 MB).

### Bước 3: Chạy server-spring

```bash
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar
```

Log khi boot thành công:

```
INFO  com.tankzors.TankzorsApplication  : Starting TankzorsApplication v1.0-SNAPSHOT
INFO  o.f.c.i.s.JdbcTableSchemaHistory  : Schema history table "public"."flyway_schema_history" does not exist yet
INFO  o.f.core.internal.command.DbMigrate: Migrating schema "public" to version "1 - init"
INFO  o.f.core.internal.command.DbMigrate: Migrating schema "public" to version "2 - player progress"
INFO  com.tankzors.engine.GameLoop       : [GameLoop] STARTED - Target: 30Hz (33ms/tick)
INFO  com.tankzors.TankzorsApplication   : Started TankzorsApplication in 4.123 seconds
```

Truy cập:

- [http://localhost:8081/](http://localhost:8081/) — Home Thymeleaf
- [http://localhost:8081/campaign](http://localhost:8081/campaign) — Chọn màn
- [http://localhost:8081/play?levelId=campaign_1](http://localhost:8081/play?levelId=campaign_1) — Chơi (iframe React)
- [http://localhost:8081/leaderboard](http://localhost:8081/leaderboard) — Bảng xếp hạng
- [http://localhost:8081/api/health](http://localhost:8081/api/health) — Health JSON

### Bước 4 (optional): Build desktop client

LibGDX desktop launcher cho dev parity:

```bash
mvn -f game-client/pom.xml clean package -DskipTests
java -jar game-client/target/tankzors-game-client-1.0-SNAPSHOT.jar
# Cửa sổ 800x600, tự connect ws://localhost:8081/game
```

### Bước 5 (optional): Build React client thủ công

Để rebuild React (sau khi sửa `client/src/...`):

```bash
cd client
npm install     # một lần đầu
npm run build   # output -> client/dist

# Copy sang server-spring static
Remove-Item -Recurse -Force ../server-spring/src/main/resources/static/play
Copy-Item -Recurse client/dist ../server-spring/src/main/resources/static/play

# Rebuild server-spring để pick HTML mới
mvn -f server-spring/pom.xml clean package -DskipTests
```

## 3. Test suite

```bash
# Full server-spring (141 tests, qua WSL bash nếu Maven chưa có)
mvn -f server-spring/pom.xml test

# Legacy server (112 tests, kept làm parity reference)
mvn -f server/pom.xml test

# Shared protocol (7 tests)
mvn -f shared-protocol/pom.xml test

# React client (62 tests, vitest)
cd client && npm test
```

Test coverage (Phase 5):

| Module | Test count | Ghi chú |
|---|---|---|
| server-spring engine | 60 | BulletManager, CollisionManager, EndToEndIntegration |
| server-spring ai | 13 | AIController, Chase/Patrol/Shoot |
| server-spring entity | 14 | PlayerTank |
| server-spring map | 11 | MapLoader, CampaignMaps |
| server-spring engine progression | 5 | CampaignProgressionTest |
| server-spring application | 15 | ScoreCalc + LeaderboardSvc + PlayerProgressSvc + MatchEndSvc |
| server-spring leaderboard/progress JPA | 3 | LeaderboardRepositoryTest |
| server-spring web | 7 | LeaderboardController + ProgressController |
| server-spring context | 3 | TankzorsApplicationContextTest |
| server (legacy) | 112 | engine + ai + map + collision + e2e |
| client (legacy React) | 62 | Renderer + E2E + CampaignProgression |
| shared-protocol | 7 | PacketType + serialization |

## 4. Common operations

### Reset database

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d
```

Volume `-v` xóa `tankzors_pg_data` → migration chạy lại từ V1.

### Reset progression của 1 session

```bash
curl -XPOST -H 'X-Player-Session: <sessionId>' http://localhost:8081/api/progress/reset
```

### Query leaderboard trực tiếp từ DB

```bash
docker exec -it tankzors-postgres psql -U tankzors -d tankzors
tankzors=# SELECT player_name, map_id, score FROM leaderboard ORDER BY score DESC LIMIT 10;
tankzors=# SELECT * FROM player_progress WHERE session_id = 'verify-restart-session';
tankzors=# \q
```

### Đổi port server

Sửa `server-spring/src/main/resources/application.yml`:

```yaml
server:
  port: 8080     # mặc định 8081
```

Hoặc override qua env khi run:

```bash
SERVER_PORT=9000 java -jar server-spring/target/...
```

### Override database connection

```bash
DB_URL=jdbc:postgresql://prod-db:5432/tankzors_prod \
DB_USER=admin \
DB_PASSWORD=secret \
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar
```

## 5. Troubleshooting

### Server không start: "Connection refused localhost:5432"

Postgres chưa chạy:

```bash
docker compose -f infra/docker-compose.yml up -d postgres
docker compose -f infra/docker-compose.yml ps
```

Nếu vẫn không được, thử:

```bash
docker ps -a | findstr postgres        # check container state
docker logs tankzors-postgres          # xem log chi tiết
```

### Flyway báo "Validate failed"

Có thể đã chỉnh schema manually. Reset hoặc dùng baseline:

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d
```

### Build fails với "mvnw has CRLF line endings"

Repo có `mvnw` shell script với Windows line endings. Fix:

```bash
# WSL bash:
sed 's/\r$//' mvnw > /tmp/mvnw_fixed
bash /tmp/mvnw_fixed -f server-spring/pom.xml clean package
```

Hoặc cài Maven system-wide.

### WebSocket không connect

1. Check server đang listen 8081 (`netstat -ano | findstr :8081` trên Windows, `lsof -i :8081` trên Unix).
2. Check CORS: `WebSocketConfig.java` đang `setAllowedOrigins("*")` nên không phải vấn đề.
3. Test bằng wscat:
   ```bash
   npm install -g wscat
   wscat -c ws://localhost:8081/game
   # Server gửi CONNECT packet ngay
   ```

### Tank không di chuyển

1. F12 browser, mở Network → WS tab. Check packet đang gửi.
2. Phải có `MOVE_INPUT` payload `{"direction":"UP","moving":true}` (chứ không `{moving: "true"}` kiểu string).
3. Server log: `[MessageHandler] << sessionId=... payload={"type":"MOVE_INPUT", ...}` 则 đang nhận.
4. Tick rate log mỗi giây: `[GameLoop] Tick rate: 30/s | ... tanks: 1 | bullets: 0`.

### VICTORY nhưng leaderboard không có entry

Score bị submit async (`@Async`). Đợi 1s sau VICTORY query lại. Nếu vẫn không có:

1. Server log check "MatchEndService" thấy INFO log với score + level.
2. DB trực tiếp: `SELECT * FROM leaderboard ORDER BY id DESC LIMIT 1;`.
3. Logic: nếu score mới thấp hơn existing → không insert (best-score upsert).

### React client trong iframe không connect WebSocket

React build mặc định auto-detect URL từ `window.location`:

```ts
// client/src/shared/constants/game.ts
return `${proto}//${window.location.host}/game`;
```

Trong iframe, `window.location` = URL của iframe (`http://localhost:8081/play/index.html`). Vẫn trỏ tới 8081 nên OK.

Nếu chạy dev với `npm run dev` (Vite port 5173), phải override:

```bash
# .env.development
VITE_WS_URL=ws://localhost:8081/game
```

## 6. Production deploy

Tham khảo `infra/docker-compose.yml` cho single-machine deploy. Production nên có:

- Nginx/Caddy reverse proxy phía trước (terminate TLS, gzip).
- PostgreSQL managed (RDS, Cloud SQL) thay vì container.
- Spring Boot profile `prod`:
  ```yaml
  # application-prod.yml
  spring:
    jpa:
      hibernate:
        ddl-auto: validate
  logging:
    level:
      root: WARN
      com.tankzors: INFO
  server:
    port: 8080
  ```

Run:

```bash
java -jar server-spring.jar --spring.profiles.active=prod
```

## 7. Migration kế hoạch còn lại (Phase 3.5+)

Nếu muốn hoàn tất LibGDX HTML5 migration thay React:

1. Convert `game-client/pom.xml` sang multi-module với GWT plugin Hoặc quay lại Gradle
   (libgdx template dùng Gradle default).
2. Implement WebSocket bridge cho browser qua `JsInterop` (browser native WebSocket, không phải Java-WebSocket).
3. Build war với gwt-maven-plugin, deploy vào `server-spring/src/main/resources/static/play/`.
4. Replace iframe src trong `templates/play.html` từ `index.html` sang `game-client/index.html`.
5. Cutover: `git rm -r server/ client/`. Commit `chore(cutover): remove legacy React+Javalin`.

Xem `.planning/ROADMAP.md` Phase 3.5 cho chi tiết.
