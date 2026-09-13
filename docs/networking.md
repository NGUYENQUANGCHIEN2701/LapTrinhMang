# Networking

Tài liệu client-server networking cho Tankzors Pro. Tham khảo [`protocol-v1.md`](protocol-v1.md) cho wire format gốc.

## 1. Tổng quan

Server Spring Boot lắng nghe 2 loại kết nối:

| Loại | Endpoint | Port | Mục đích |
|---|---|---|---|
| HTTP REST | `/api/*` | 8081 | Health check, leaderboard query, progression |
| WebSocket | `/game` | 8081 | Realtime input/state của trận |

Cả hai chia sẻ port; Spring Boot đứng giữa dispatch theo URL path.

```
Browser                                 Spring Boot (8081)
   │                                          │
   ├─[HTTP]── /                 ────────────► │  Thymeleaf controller
   ├─[HTTP]── /api/health       ────────────► │  HealthController
   ├─[HTTP]── /api/leaderboard  ────────────► │  LeaderboardController
   ├─[HTTP]── /api/progress     ────────────► │  ProgressController
   │
   └─[WS]─── /game              ────────────► │  WebSocket → GameWebSocketHandler
         (Upgrade HTTP → WS)                    │     ↓ queueInput
                                                │  GameLoop (30Hz single-thread)
                                                │     ↓ broadcast
                                                │  WebSocketStatePublisher
                                                ◄── (broadcast tới tất cả WS sessions)
```

## 2. WebSocket protocol

### 2.1 Handshake

```
GET /game HTTP/1.1
Host: localhost:8081
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: <random>
Sec-WebSocket-Version: 13
```

Spring accept handshake → server gửi `CONNECT` packet ngay:

```json
{
  "type": "CONNECT",
  "sessionId": "0d648dc1-4c84-ae48-c60b-20dca42c6f7d",
  "message": "Kết nối tới Tankzors Server thành công!",
  "timestamp": 1716000000000
}
```

Client dùng `sessionId` để identify player tank trong các STATE packet kế tiếp.

### 2.2 Inbound packets (client → server)

Tất cả packet JSON UTF-8, mỗi frame 1 packet.

#### `JOIN_GAME`

Gửi khi vào game. Format nested payload (parity v1):

```json
{
  "type": "JOIN_GAME",
  "payload": {
    "mode": "campaign",
    "levelId": "campaign_1",
    "playerName": "Player"
  },
  "timestamp": 1716000000000
}
```

| Field | Required | Notes |
|---|---|---|
| `payload.mode` | no | "campaign" mặc định |
| `payload.levelId` | no | nếu thiếu, giữ map hiện tại |
| `payload.playerName` | no | chỉ log |

Server phản hồi ngay `ACK` + `STATE` (trạng thái sau reset match nếu cần).

#### `MOVE_INPUT`

Khi bấm/nhả phím di chuyển:

```json
{
  "type": "MOVE_INPUT",
  "payload": {
    "direction": "UP",
    "moving": true
  },
  "timestamp": 1716000000000
}
```

`direction`: `"UP" | "DOWN" | "LEFT" | "RIGHT"`. `moving=false` để ngừng hướng đó.

**Multi-direction**: Để đi chéo, gửi 2 packet (`UP` + `RIGHT` moving=true). Hướng chéo = vector tổng.

**STOP package**: nếu chỉ `{"type":"STOP"}` không direction → dừng toàn bộ.

#### `SHOOT_INPUT`

Bắn 1 viên đạn (respecting cooldown 0.5s):

```json
{
  "type": "SHOOT_INPUT",
  "timestamp": 1716000000000
}
```

#### `PING`

```json
{ "type": "PING", "timestamp": 1716000000000 }
```

Server đáp `PONG` ngay.

### 2.3 Outbound packets (server → client)

#### `ACK`

Sau mỗi input từ client:

```json
{
  "type": "ACK",
  "status": "RECEIVED",
  "action": "MOVE_INPUT",
  "timestamp": 1716000000000
}
```

#### `STATE` (broadcast loop 30Hz)

Snapshot toàn state, server phát khi `status != WAITING`:

```json
{
  "type": "STATE",
  "tanks": [
    {
      "id": "session-xyz",
      "x": 100.0,
      "y": 100.0,
      "rotation": 90.0,
      "hp": 75,
      "maxHp": 100,
      "alive": true,
      "isPlayer": true
    },
    {
      "id": "ai-16-11",
      "x": 660.0,
      "y": 460.0,
      "rotation": 270.0,
      "hp": 100,
      "maxHp": 100,
      "alive": true,
      "isPlayer": false
    }
  ],
  "bullets": [
    {
      "id": "uuid-...",
      "active": true,
      "x": 220.5,
      "y": 100.0,
      "direction": 90.0,
      "ownerId": "session-xyz"
    }
  ],
  "map": {
    "width": 20,
    "height": 15,
    "tileSize": 40,
    "grid": [[1,1,1,...], ...]
  },
  "score": 100,
  "gameStatus": "PLAYING",
  "timestamp": 1716000000000
}
```

`gameStatus`: `WAITING | PLAYING | VICTORY | DEFEAT`.

#### `PONG`

```json
{ "type": "PONG", "timestamp": 1716000000000 }
```

#### `ERROR`

Khi client gửi JSON sai:

```json
{
  "type": "ERROR",
  "message": "Định dạng JSON không hợp lệ: ...",
  "code": 400
}
```

### 2.4 Match lifecycle

```
1. onConnect    → server gửi CONNECT
2. JOIN_GAME    → server reset match (nếu đổi level/player chết), spawn AI, setStatus(PLAYING)
                  → broadcast STATE ngay
3. Mỗi 33ms tick → server broadcast STATE mới nhất
4. When tank HP=0:
   4a. tick N:   takeDamage -> alive=false, hp=0
   4b. tick N+1: STATE có alive=false broadcast lần cuối
   4c. tick N+2: cleanup -> tank remove khỏi state
5. Nếu hết AI   → setStatus(VICTORY) + STATE
6. Nếu hết player → setStatus(DEFEAT) + STATE
7. MatchEndObserver async → submit score + unlock progression
```

## 3. REST API

### `GET /api/health`

```bash
curl http://localhost:8081/api/health
# {"port":8081,"status":"UP"}
```

### `GET /api/leaderboard/{mapId}`

Top 10 entries cho map:

```bash
curl http://localhost:8081/api/leaderboard/campaign_1
```

Response:

```json
[
  {
    "id": 2,
    "playerName": "Hero2",
    "mapId": "campaign_1",
    "score": 2000,
    "timeSeconds": 30,
    "hpRemain": 95,
    "createdAt": "2026-09-13T05:41:44.800887Z"
  },
  {
    "id": 1,
    "playerName": "Hero1",
    "mapId": "campaign_1",
    "score": 1200,
    "timeSeconds": 45,
    "hpRemain": 80,
    "createdAt": "2026-09-13T05:41:44.760492Z"
  }
]
```

### `POST /api/leaderboard`

Submit/upsert score cho (playerName, mapId):

```bash
curl -XPOST http://localhost:8081/api/leaderboard \
  -H 'Content-Type: application/json' \
  -d '{
    "playerName":"Hero",
    "mapId":"campaign_1",
    "score":1500,
    "timeSeconds":40,
    "hpRemain":75
  }'
```

Response:

```json
{
  "status": "OK",
  "score": 1500,
  "createdAt": "2026-09-13T05:42:00.123456Z"
}
```

Logic: nếu đã có entry của cùng `playerName+mapId` và score **cao hơn** → giữ nguyên; nếu **thấp hơn hoặc bằng** → update. Upsert best-score.

Lưu ý: Khi VICTORY qua WebSocket, server tự động submit score qua endpoint tương đương (`MatchEndService.onMatchEnd`). Client không cần chủ động gọi.

### `GET /api/progress`

Trả về danh sách màn đã unlock cho session hiện tại:

```bash
# Auto-generate HttpSession khi không có header
curl http://localhost:8081/api/progress

# Hoặc explicit session ID (từ WebSocket CONNECT packet)
curl -H 'X-Player-Session: 0d648dc1-...' http://localhost:8081/api/progress
```

Response:

```json
{
  "sessionId": "0d648dc1-...",
  "unlockedLevels": ["campaign_1", "campaign_2"]
}
```

Note: `X-Player-Session` header cho phép align HttpSession (HTTP) ↔ WebSocket session id. Khi client gửi CONNECT nhận được `sessionId`, dùng nó cho subsequent REST calls.

### `POST /api/progress/reset`

Reset progression về mặc định (chỉ `campaign_1`):

```bash
curl -XPOST -H 'X-Player-Session: <sid>' http://localhost:8081/api/progress/reset
```

## 4. Tick rate & timing

| Tham số | Giá trị | Nguồn |
|---|---|---|
| Tick rate | 30 Hz | `ServerConfig.TICK_RATE` |
| Tick interval | 33 ms | `1000 / TICK_RATE` |
| Scheduler | `ScheduledExecutorService.scheduleAtFixedRate` | single-thread daemon |
| deltaTime tính theo | `(now - lastTickTime) / 1e9` (giây) | nhận thực tế |
| Actual đo | 30-32 tick/s | log metrics |

Player speed `150 px/s * deltaTime' ≈ 5 px/tick`. Tương tự bullet `300 px/s × 0.033s ≈ 10 px/tick`.

## 5. Wire format compatibility

- Tất cả packet có `type: String`, `timestamp: long` (epoch millis).
- Gson dùng `new GsonBuilder().create()` — không pretty, không serialize null (parity với legacy Javalin).
- Numbers:
  - Integers: int / Integer (score, hp, maxHp, timeSeconds).
  - Doubles: vị trí `x`, `y`, `rotation`, `direction` luôn float.

## 6. Tham khảo implementation

| Class | File |
|---|---|
| WebSocket handler | `server-spring/.../network/GameWebSocketHandler.java` |
| Parser (flat hoá payload) | `server-spring/.../network/InputPacketParser.java` |
| State publisher | `server-spring/.../network/WebSocketStatePublisher.java` |
| Session registry | `server-spring/.../network/SessionRegistry.java` |
| Engine tick | `server-spring/.../engine/GameLoop.java#step` |
| JSON serializer | `server-spring/.../config/GsonConfig.java` |
| StateMessage builder | `server-spring/.../engine/GameState.java#toStateMessage` |

## 7. Legacy protocol parity

Wire format hoàn toàn parity với server Javalin cũ (`server/.../network`). React client cũ vẫn kết nối được tới server-spring nếu `DEFAULT_SERVER_URL` trỏ tới `ws://localhost:8081/game`.

Cross-reference: [`docs/protocol-v1.md`](protocol-v1.md), [`docs/parity-checklist.md`](parity-checklist.md).
