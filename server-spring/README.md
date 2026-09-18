# server-spring

Spring Boot backend chính của Tankzors.

## Trách nhiệm

- Phục vụ website Thymeleaf và bundle LibGDX HTML5.
- Cấp bootstrap map cho campaign qua `GET /api/game/bootstrap`.
- Nhận kết quả idempotent qua `POST /api/game/results`.
- Lưu progression, leaderboard và match result trong PostgreSQL.
- Relay co-op qua WebSocket `/coop`.
- Giữ endpoint `/game` để tương thích trong thời gian hoàn tất cutover.

Gameplay campaign hiện chạy trong `game-client`; scheduler engine phía server
được tắt mặc định bằng `tankzors.engine.scheduler.enabled=false`.

## Build và chạy

Từ root repository:

```powershell
docker compose -f infra\docker-compose.yml up -d
mvn -f server-spring\pom.xml clean package
java -jar server-spring\target\tankzors-server-spring-1.0-SNAPSHOT.jar
```

Hoặc chạy trực tiếp:

```powershell
mvn -f server-spring\pom.xml spring-boot:run
```

Server dùng port `8081`. Health endpoint: `http://localhost:8081/api/health`.

## Test

```powershell
mvn -f server-spring\pom.xml test
```

Database schema được quản lý bởi Flyway trong
`src/main/resources/db/migration`. Không sửa migration đã được áp dụng; hãy tạo
migration phiên bản kế tiếp.
