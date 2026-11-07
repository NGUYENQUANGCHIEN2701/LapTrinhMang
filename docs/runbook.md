# Runbook

Hướng dẫn build, run, troubleshoot Tankzors Pro. Tài liệu chính của repo.

## 1. Yêu cầu môi trường

| Tool | Version | Lý do |
|---|---|---|
| **JDK** | 17+ (test 17 và 21) | Spring Boot + game-client |
| **Maven** | 3.9+ | Build server-spring |
| **Docker Desktop** | 4.20+ | Chạy PostgreSQL 16 qua docker compose |
| **Git** | 2.40+ | Để clone & commit |
| **Browser** | bất kỳ hiện | Thymeleaf website |

### Lưu ý đặc thù

- **Maven**: Repo không kèm Maven wrapper. Cài Maven system-wide (3.9+) và
  kiểm tra bằng `mvn -version`.

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
mvn -f server-spring/pom.xml clean package -DskipTests
```

Output fat jar: `server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar` (~50 MB).

### Bước 3: Chạy server-spring

```bash
java -jar server-spring/target/tankzors-server-spring-1.0-SNAPSHOT.jar
```

Log khi boot thành công sẽ có Spring Boot, Flyway và datasource; scheduler
server-side được tắt mặc định vì campaign chạy trong LibGDX client.

```
INFO  com.tankzors.TankzorsApplication  : Starting TankzorsApplication v1.0-SNAPSHOT
INFO  o.f.c.i.s.JdbcTableSchemaHistory  : Schema history table "public"."flyway_schema_history" does not exist yet
INFO  o.f.core.internal.command.DbMigrate: Migrating schema "public" to version "1 - init"
INFO  o.f.core.internal.command.DbMigrate: Migrating schema "public" to version "2 - player progress"
INFO  com.tankzors.TankzorsApplication   : Started TankzorsApplication in 4.123 seconds
```

Truy cập:

- [http://localhost:8081/](http://localhost:8081/) — Home Thymeleaf
- [http://localhost:8081/campaigns](http://localhost:8081/campaigns) — Chọn màn
- [http://localhost:8081/play?levelId=campaign_1](http://localhost:8081/play?levelId=campaign_1) — Chơi LibGDX HTML5
- [http://localhost:8081/leaderboard](http://localhost:8081/leaderboard) — Bảng xếp hạng
- [http://localhost:8081/api/health](http://localhost:8081/api/health) — Health JSON

### Bước 4 (optional): Build lại HTML5 client

```powershell
Set-Location game-client
.\gradlew.bat html:copyToServer
Set-Location ..
```

## 3. Test suite

```bash
# Full server-spring (141 tests, qua WSL bash nếu Maven chưa có)
mvn -f server-spring/pom.xml test

# LibGDX client
Set-Location game-client
.\gradlew.bat test
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

## 4. Common operations

### Reset database

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d
```

Volume `-v` xóa `tankzors_pg_data` → migration chạy lại từ V1.

### Reset progression của 1 session

```bash
# Cần cookie phiên của tài khoản đã đăng nhập và CSRF token (lấy từ trình duyệt).
curl -XPOST -b 'JSESSIONID=<cookie>' -H 'X-CSRF-TOKEN: <token>' http://localhost:8081/api/progress/reset
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

1. Click vào iframe game để chuyển focus.
2. Mở browser console và kiểm tra lỗi GWT hoặc asset.
3. Xác nhận bootstrap trả `200`: `/api/game/bootstrap?levelId=campaign_1`.
4. Nếu vừa sửa client, chạy lại `game-client\\gradlew.bat html:copyToServer`.

### VICTORY nhưng leaderboard không có entry

Kết quả campaign được client gửi tới `POST /api/game/results`. Nếu chưa có:

1. Browser Network kiểm tra request `/api/game/results`.
2. DB trực tiếp: `SELECT * FROM leaderboard ORDER BY id DESC LIMIT 1;`.
3. Logic: nếu score mới thấp hơn existing → không insert (best-score upsert).

### LibGDX HTML5 không tải

1. Kiểm tra `server-spring/src/main/resources/static/game/libgdx/index.html`.
2. Build lại bằng `game-client\\gradlew.bat html:copyToServer`.
3. Mở browser console để xem lỗi GWT và kiểm tra asset trả HTTP 200.

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

## 7. Trạng thái cutover

LibGDX HTML5 đã thay React trong luồng `/play`; các module React, Javalin và
`shared-protocol` thử nghiệm đã được xóa. Công việc tiếp theo tập trung vào
hoàn thiện co-op, reward/debriefing và loại endpoint compatibility `/game` khi
không còn caller.
