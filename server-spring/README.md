# server-spring Module - README

Spring Boot target server cho migration Phase 2+. Phase 1 chỉ là skeleton.

## Mục đích

- Phase 1: tạo structure package, có application + WebSocket /game handler
  placeholder + health endpoint. Start được nhưng chưa có game logic.
- Phase 2: thay Javalin -> Spring WebSocket, giữ nguyên engine/AI/collision
  từ `server/` cũ.
- Phase 4: thêm JPA (PostgreSQL) + Thymeleaf.
- Phase 5: cutover, xóa `server/` Javalin implementation.

## Cấu trúc package

```
server-spring/
├── src/main/java/com/tankzors/
│   ├── TankzorsApplication.java      # @SpringBootApplication entry point
│   ├── config/
│   │   └── GsonConfig.java           # Gson bean (cùng format v1)
│   ├── network/
│   │   ├── GameHandlerConfig.java    # Bean cho handler
│   │   ├── GameWebSocketHandler.java # Phase 1 placeholder
│   │   └── WebSocketConfig.java      # Đăng ký /game endpoint
│   └── web/
│       └── HealthController.java     # GET / và GET /api/health
└── src/main/resources/
    └── application.yml               # Port 8081 (tránh xung đột legacy 8080)
```

## Port

| Module | Port | Ghi chú |
|---|---|---|
| `server/` Javalin cũ | 8080 | Legacy, chưa được xóa |
| `server-spring/` (this) | 8081 | Phase 1 - chưa có game logic |

Phase 5 cutover: đổi `server-spring` về port 8080 sau khi legacy bị xóa.

## Build & Run

```bash
# Cần install shared-protocol trước
mvn -f shared-protocol/pom.xml install

# Build
mvn -f server-spring/pom.xml clean package

# Run (port 8081)
mvn -f server-spring/pom.xml spring-boot:run

# Test
curl http://localhost:8081/
# => "Tankzors Server (Spring) is running"

curl http://localhost:8081/api/health
# => {"status":"UP","port":8081}

# WebSocket /game handshake
wscat -c ws://localhost:8081/game
# => {"type":"CONNECT","sessionId":"...","message":"Kết nối tới Tankzors Spring Server thành công!","timestamp":...}
```

## Phase 2 kế hoạch

Replace `GameWebSocketHandler.handleTextMessage` bằng route sang:
1. `MessageParser` dùng Gson bind vào `BasePacket` từ shared-protocol.
2. Switch theo `packet.type`:
   - `JOIN_GAME` -> `GameSessionService.handleJoin` (port interface).
   - `MOVE_INPUT`/`SHOOT_INPUT` -> `GameSessionService.queueInput`.
3. Adapter mới cho WebSocket session ( annotation tương đương
   `WsContext`) để re-use `GameLoop` cũ mà không sửa nó.

Target: giữ nguyên `GameLoop` + `GameState` + `AITank` + `CollisionManager`
trong `server/` jar cũ hoặc copy sang `server-spring`. Quyết định: copy
sang để độc lập, chỉ swap transport/network layer.
