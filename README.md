# Tankzors

Tankzors là game xe tăng chạy theo kiến trúc **client-authoritative**:

- LibGDX client tự chạy gameplay, AI, collision và render.
- Spring Boot phục vụ website, xác thực, bootstrap map, lưu kết quả, tiến trình và bảng xếp hạng.
- PostgreSQL lưu dữ liệu người dùng và kết quả.
- WebSocket `/coop` là relay cho co-op; server không mô phỏng trận đấu.

Engine server-authoritative và frontend React/Javalin cũ đã được loại khỏi repository.

## Kiến trúc hiện tại

```text
Browser
  -> GET /play
  -> tải bundle /game/libgdx/**
  -> GET /api/game/bootstrap?levelId=campaign_1
  -> LibGDX chạy GameSimulation ngay trên client
  -> POST /api/game/results
  -> Spring Boot xác thực/lưu PostgreSQL
```

Lưu ý: `/game/libgdx/**` chỉ là URL static của bundle HTML5. Nó không phải endpoint WebSocket `/game` của engine cũ.

## Thư mục

```text
game-client/     LibGDX core, HTML5 launcher, desktop launcher và asset nguồn
server-spring/   Spring Boot web/API/persistence/co-op relay
infra/           Docker Compose cho app và PostgreSQL
docs/            Tài liệu kiến trúc, networking và vận hành hiện tại
```

## Chạy toàn bộ dự án

Yêu cầu duy nhất: Docker Desktop đang chạy.

```powershell
Set-Location D:\LTM\LapTrinhMang
docker compose -f infra/docker-compose.yml up --build
```

Sau khi app báo started:

- Website: http://localhost:8081
- Đăng nhập: http://localhost:8081/login
- Game: vào `Game` -> `Campaigns` -> chọn màn
- Admin: http://localhost:8081/admin
- Health check: http://localhost:8081/api/health

Tài khoản seed mặc định được tạo bởi cấu hình ứng dụng; xem `application.yml`/log khởi động nếu thông tin đã được thay đổi.

Dừng stack:

```powershell
docker compose -f infra/docker-compose.yml down
```

Muốn xóa cả dữ liệu PostgreSQL:

```powershell
docker compose -f infra/docker-compose.yml down -v
```

## Khi sửa code

Dockerfile build theo ba stage:

1. JDK 17 + Gradle build `game-client` thành bundle HTML5.
2. JDK 21 + Maven chép bundle đó vào Spring resources và build JAR.
3. JRE 21 chạy ứng dụng.

Vì vậy, sau khi sửa client hoặc server chỉ cần chạy lại:

```powershell
docker compose -f infra/docker-compose.yml up --build
```

Docker cache dependency nên các lần sau thường nhanh hơn.

Nếu muốn build bundle client vào source tree để chạy Spring ngoài Docker:

```powershell
Set-Location game-client
.\gradlew.bat clean html:copyToServer
```

Output được tạo ở:

```text
game-client/html/build/dist/
```

và được đồng bộ tới:

```text
server-spring/src/main/resources/static/game/libgdx/
```

Không sửa trực tiếp JavaScript sinh ra trong `static/game/libgdx`; hãy sửa source trong `game-client` rồi build lại.

## Test

Client:

```powershell
Set-Location game-client
.\gradlew.bat core:test
```

Server (khi có Maven/JDK 21):

```powershell
Set-Location server-spring
mvn test
```

## Tài liệu

- [Kiến trúc](docs/architecture.md)
- [Hướng dẫn đọc code](docs/CODEBASE-GUIDE.md)
- [Networking](docs/networking.md)
- [Runbook](docs/runbook.md)
