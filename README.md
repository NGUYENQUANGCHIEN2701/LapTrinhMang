# Tankzors Pro

Game xe tăng PvE chạy trên browser. Gameplay campaign được mô phỏng trong
LibGDX client; Spring Boot phục vụ website, dữ liệu map, progression,
leaderboard, kết quả trận và WebSocket relay cho co-op.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21 + Spring Boot 3.3.4 |
| Game client | LibGDX 1.12.1, HTML5/GWT và LWJGL3 desktop |
| Website | Thymeleaf |
| Realtime | Spring WebSocket (`/coop`; `/game` giữ compatibility) |
| Database | PostgreSQL 16 + Flyway + Spring Data JPA |

## Cấu trúc repo

```text
LapTrinhMang/
├── server-spring/      # Backend, website, persistence và WebSocket relay
├── game-client/        # LibGDX core, HTML5 launcher và desktop launcher
├── infra/              # PostgreSQL Docker Compose
├── docs/               # Tài liệu kỹ thuật và lịch sử migration
└── .planning/          # Kế hoạch và feature matrix
```

Các module React/Javalin cũ và `shared-protocol` thử nghiệm đã được loại bỏ sau
cutover. Protocol runtime hiện được duy trì tại biên của `server-spring` và
`game-client`.

## Chạy project

Yêu cầu: Java 21, Maven 3.9+ và Docker Desktop.

```powershell
Set-Location D:\BTLLapTrinhMang\LapTrinhMang
docker compose -f infra\docker-compose.yml up -d
mvn -f server-spring\pom.xml spring-boot:run
```

Mở `http://localhost:8081/` hoặc
`http://localhost:8081/play?levelId=campaign_1`.

Khi thay đổi LibGDX client, build lại bundle HTML5 trước khi chạy server:

```powershell
Set-Location game-client
.\gradlew.bat html:copyToServer
Set-Location ..
```

Desktop launcher:

```powershell
Set-Location game-client
.\gradlew.bat desktop:run --args="--level campaign_1"
```

## Kiểm thử

```powershell
mvn -f server-spring\pom.xml test
Set-Location game-client
.\gradlew.bat test
```

Xem [runbook](docs/runbook.md), [kiến trúc](docs/architecture.md) và
[codebase guide](docs/CODEBASE-GUIDE.md) để biết thêm chi tiết.
