# Chiến lược kiểm thử

## Client

```powershell
Set-Location game-client
.\gradlew.bat core:test
.\gradlew.bat html:dist
```

Core test kiểm tra simulation/AI/collision/session; `html:dist` bắt lỗi GWT-specific.

## Server

```powershell
mvn -f server-spring/pom.xml test
```

Test profile dùng H2 và Flyway; hiện có 54 test sau cleanup.

## Tích hợp

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

Smoke check: `/api/health`, `/game/libgdx/index.html`, favicon, container logs và `/game` phải 404.

Phase 2 cần bổ sung test hai connection cho relay room/role/direction và manual UAT hai browser.
