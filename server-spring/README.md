# Tankzors Spring server

Module này không chứa gameplay engine. Nó chịu trách nhiệm:

- website Thymeleaf và Spring Security;
- `GET /api/game/bootstrap` để cấp map/run cho client;
- `POST /api/game/results` để lưu kết quả idempotent;
- progress, leaderboard và admin;
- Flyway/JPA/PostgreSQL;
- WebSocket `/coop` làm relay, không chạy simulation.

Bundle HTML5 được phục vụ tại `/game/libgdx/**`. Đây là static path, không phải WebSocket gameplay.

## Build

Từ root repository:

```powershell
docker compose -f infra/docker-compose.yml up --build
```

Hoặc nếu máy đã có JDK 21 và Maven:

```powershell
mvn test
mvn spring-boot:run
```

Database schema nằm trong `src/main/resources/db/migration`. Không sửa migration đã áp dụng; hãy tạo migration phiên bản tiếp theo.
