# Protocol v1 - Wire format Tankzors Pro

Tài liệu này đóng băng behavior hiện tại của server (Javalin + Gson) và
client (React + TypeScript + WebSocket) làm chuẩn parity cho migration
sang Spring Boot + LibGDX HTML5. Không thay đổi behavior code trong phase này.

- Phiên bản: 1 (Phase 0 baseline).
- Endpoint WebSocket: `ws://localhost:8080/game`.
- Transport: text frame JSON UTF-8 (mỗi frame 1 message).
- Mã hoá ngày/giờ: `timestamp` dạng epoch milliseconds (`Date.now()` /
  `System.currentTimeMillis()`).
- Hợp đồngutheritative: client chỉ gửi input + JOIN_GAME, server quyết định
  toàn bộ state, điểm số và kết quả trận.

## 1. Mô tả luồng chính

```text
1. Client mở WebSocket tới /game.
2. Server gửi CONNECT (sessionId, message, timestamp) ngay trên onConnect.
3. Client gửi JOIN_GAME { mode, levelId, playerName }.
4. Server:
   - Nạp map (nếu level đổi hoặc chưa có map).
   - Reset AI/bullet về trận mới khi cần.
   - Spawn player tại SPAWN_PLAYER.
   - Spawn các AI theo enemies metadata trong map JSON.
   - setStatus(PLAYING) và broadcast STATE ngay.
5. Mỗi tick 30Hz: server broadcast STATE đến mọi client.
6. Client gửi MOVE_INPUT / SHOOT_INPUT khi có thay đổi trạng thái phím.
7. Khi player chết -> trạng thái DEFEAT; khi toàn bộ AI chết -> VICTORY.
   State cuối cùng (DEFEAT/VICTORY) vẫn được broadcast.
8. Client ngắt kết nối -> server_Remove tank; nếu không còn player thì
   WAITING và dọn AI/bullet.
```

### Tần suất server

| Thông số | Giá trị | Nguồn |
|---|---|---|
| Tick rate | 30 Hz | `ServerConfig.TICK_RATE` |
| Tick interval | `1000 / 30 ≈ 33ms` | `ServerConfig.TICK_INTERVAL_MS` |
| HTTP port | 8080 | `ServerConfig.PORT` |
| Scheduler | `ScheduledExecutorService.scheduleAtFixedRate` |
| HTTP health | `GET /` -> "Tankzors Server is running"; `GET /api/health` -> `{"status":"UP","port":8080}` |

Game loop là single-threaded (daemon thread "GameLoop-Thread"). Input từ
network thread đẩy vào `ConcurrentLinkedQueue`, loop poll và xử lý tuần tự
trong `step(dt)`.

## 2. Bảng packet type

| type | Hướng | Phát sinh tại | Mục đích |
|---|---|---|---|
| `CONNECT` | S -> C | `WebSocketServer.onConnect` | Chào mừng + gán sessionId |
| `JOIN_GAME` | C -> S | `MessageBuilder.buildJoinGame` | Yêu cầu vào trận |
| `MOVE_INPUT` | C -> S | `MessageBuilder.buildMoveInput` | Đặt/bỏ направлением đang giữ |
| `SHOOT_INPUT` | C -> S | `MessageBuilder.buildShootInput` | Yêu cầu bắn 1 viên |
| `STATE` | S -> C | `GameLoop.broadcastState` | Snapshot toàn state, 30Hz |
| `ACK` | S -> C | `MessageHandler` | Xác nhận đã nhận input/generic message |
| `PONG` | S -> C | `MessageHandler.handlePing` | Đáp lại `PING` |
| `ERROR` | S -> C | `MessageHandler` | JSON sai, message trống, v.v. |

> Lưu ý: server chấp nhận thêm alias dạng `INPUT` (flat action=MOVE/SHOOT)
> và payload lồng bên trong (xem `MessageHandler.parseInput`). React client
> hiện gửi packet dạng `{ type, payload, timestamp }` (nested).

## 3. Định dạng packet chi tiết

### 3.1 CONNECT (server -> client)

```json
{
  "type": "CONNECT",
  "sessionId": "<ws-session-id>",
  "message": "Kết nối tới Tankzors Server thành công!",
  "timestamp": 1716000000000
}
```

- Phát ngay sau when `ws.onConnect`.
- Client lưu `sessionId` để xác định player tank trong STATE afterwards
  (`useGameSession` đọc `packet.sessionId`).

### 3.2 JOIN_GAME (client -> server)

Form React client (`MessageBuilder.buildJoinGame`):

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

Server (`MessageHandler.parseInput`) "flat hoá" payload lên top-level trước
khi gson bind, vậy nên `InputMessage.getGameMode()`, `getLevelId()`,
`getPlayerName()` hoạt động với cả dạng nested và flat.

Trường bắt buộc nếu muốn vào trận đúng:

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `mode` / `gameMode` | string ("campaign" \| "online") | không (mặc định campaign) | |
| `levelId` | string (vd "campaign_1") | không | Nếu thiếu, giữ map hiện tại |
| `playerName` | string | không | Chỉ log, chưa lưu |

Server phản hồi: `ACK { action: "JOIN_GAME", status: "RECEIVED" }` + STATE
snapshot ngay lập tức (do `handleJoinGame` gọi `broadcastState()`).

### 3.3 MOVE_INPUT (client -> server)

```json
{
  "type": "MOVE_INPUT",
  "payload": { "direction": "UP", "moving": true },
  "timestamp": 1716000000000
}
```

| Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|
| `direction` | `"UP" \| "DOWN" \| "LEFT" \| "RIGHT"` | Có | Map sang `com.tankzors.entity.Direction` |
| `moving` | boolean | Không (mặc định true) | `false` kiểu như Stop một direction |

Server xử lý qua `GameLoop.handleMoveInput`:
- Chỉ có hiệu lực khi `gameState.isPlaying()`.
- Gọi `player.setMoving(direction, moving)` cập nhật set held directions.
- `PlayerTank.updateMovement` tính vector nhập từ các direction đang giữ,
  normalize để tốc độ chéo = tốc độ cardinal, rồi stepping 5px/check
  `CollisionManager.resolveCollision`.

Có alias `action=MOVE`/`MOVE_INPUT`/`STOP`. `STOP` không kèm direction ->
`player.stopMoving()` toàn bộ.

### 3.4 SHOOT_INPUT (client -> server)

```json
{ "type": "SHOOT_INPUT", "timestamp": 1716000000000 }
```

Server: `GameLoop.handleShootInput` -> `tank.shoot()`. Nếu cooldown chưa
hết trả về `null`. Cooldown player = 0.5s (`PlayerTank.DEFAULT_SHOOT_COOLDOWN`).
 Đạn spawn tại tâm tank, hướng = `tank.getRotation()`.

### 3.5 STATE (server -> client) - wire format chính

```json
{
  "type": "STATE",
  "tanks": [
    {
      "id": "session-1",
      "x": 100.0,
      "y": 100.0,
      "rotation": 90.0,
      "hp": 75,
      "maxHp": 100,
      "alive": true,
      "isPlayer": true
    }
  ],
  "bullets": [
    {
      "id": "bullet-uuid",
      "active": true,
      "x": 220.5,
      "y": 100.0,
      "direction": 90.0,
      "ownerId": "session-1"
    }
  ],
  "map": {
    "width": 20,
    "height": 15,
    "tileSize": 40,
    "grid": [[1,1,...], ...]
  },
  "score": 100,
  "gameStatus": "PLAYING",
  "timestamp": 1716000000000
}
```

Quy ước serialization (xem `GameState.toStateMessage`):

- Tank: `id`, `x`, `y` (tâm), `rotation` (độ, 0=UP, 90=RIGHT, 180=DOWN,
  270=LEFT), `hp`, `maxHp`, `alive`, `isPlayer`.
- Bullet: `id` (UUID), `active`, `x`, `y`, `direction` (độ), `ownerId`.
  Bullet không có `damage` / `speed` trên wire -> client chỉ render.
- Map object: `width`, `height`, `tileSize`, `grid` 2D `[row][col]`. Giá
  trị tile: `0=GROUND`, `1=WALL`, `2=WATER`, `3=SPAWN_PLAYER`,
  `4=SPAWN_ENEMY`.
- `score`: tổng điểm tích luỹ (+100 mỗi khi một tank chết do đạn, chỉ có
  ý nghĩa tương đối).
- `gameStatus`: một trong `WAITING` | `PLAYING` | `VICTORY` | `DEFEAT`.
- Server KHÔNG broadcast khi `status == WAITING`.
- VICTORY/DEFEAT vẫn tiếp tục broadcast STATE (tank đã chết đã bị remove).

### 3.6 ACK (server -> client)

```json
{
  "type": "ACK",
  "status": "RECEIVED",
  "action": "MOVE_INPUT",
  "timestamp": 1716000000000
}
```

Dành cho mọi input (`JOIN_GAME`, `MOVE_INPUT`, `SHOOT_INPUT`). Pattern khác
(generic message không match): `{ type:"ACK", receivedType, message,
timestamp }`.

### 3.7 PING/PONG

Client gửi `{ "type":"PING" }`. Server đáp:

```json
{ "type": "PONG", "timestamp": 1716000000000 }
```

> Client React hiện chưa dùng PING (auto-reconnect dựa vào onClose).

### 3.8 ERROR (server -> client)

```json
{
  "type": "ERROR",
  "message": "Định dạng JSON không hợp lệ: ...",
  "code": 400
}
```

Phát khi message trống hoặc parse JSON fail.

## 4. Entity & tuning constants (cần parity)

### 4.1 PlayerTank

| Thuộc tính | Giá trị | Nguồn |
|---|---|---|
| Speed | 150 px/s | `PlayerTank.DEFAULT_SPEED` |
| HP / maxHP | 100 / 100 | `PlayerTank.DEFAULT_HP` |
| Width × Height | 40 × 40 px | `PlayerTank.DEFAULT_WIDTH` |
| Shoot cooldown | 0.5 s | `PlayerTank.shootCooldown` (private default) |
| Bullet speed | 300 px/s | `Bullet.DEFAULT_SPEED` |
| Bullet damage | 25 HP | `Bullet.DEFAULT_DAMAGE` |
| Bullet width × height | 8 × 8 px | `Bullet.DEFAULT_WIDTH` |
| timeSinceLastShot init | 0.5 (cho phép bắn ngay) | `PlayerTank` |

Di chuyển multi-direction: vector `(dx, dy)` tổng hợp từ held set, được
`normalize()` rồi nhân `speed * dt`. Tức là đi chéo có đúng cùng tốc độ với
cardinal. Rotation tính bằng `atan2(dy, dx)` (+ 450) % 360.

### 4.2 AITank

- HP / maxHP: 100 / 100.
- Width × Height: 40 × 40.
- Difficulty enum (`DifficultyLevel`): speed / vision / reaction /
  shootCooldown / accuracySpread.

  | Level | speed | vision | reaction | shootCooldown | spread |
  |---|---|---|---|---|---|
  | EASY | 80 | 200 | 0.5 | 2.0 | ±15° |
  | MEDIUM | 90 | 240 | 0.5 | 2.5 | ±10° |
  | HARD | 105 | 280 | 0.3 | 2.0 | ±6° |

- AI controller: Idle -> Chase -> Patrol -> Shoot (song song). Target là
  PlayerTank gần nhất + tiebreak theo id.
- ShootBehavior range: `< 250px`, phải `LineOfSight.hasClearShot`,
  冷め cooldown mỗi lần bắn = `difficulty.getShootCooldown()`. Góc trúng
  thêm offset ngẫu nhiên `± accuracySpread`.

### 4.3 Bullet physics

```
rad = toRadians(direction)
dx =  sin(rad); dy = -cos(rad)        // 0°=UP, 90°=RIGHT, ...
position += (dx, dy) * speed * dt
```

### 4.4 Collision rules

- Tank vs wall/sát biên: bounding box co 4px mỗi cạnh (margin
  `CollisionManager.TANK_WALL_MARGIN`). Vượt ngoài map hoặc `!isWalkable`
  -> chặn. Che nhiều ô thì quét `firstCol..lastCol × firstRow..lastRow`.
- `resolveCollision`: thử target -> nếu chặn, trượt X (giữ Y cũ) -> trượt
  Y (giữ X cũ) -> corner nudging (≤6px) -> đứng yên.
- Bullet vs wall: đạn active và bounding box cắt wall -> mất. Ngoài biên
  map -> mất.
- Bullet vs tank: chỉ trúng khi `bullet.ownerId != tank.id` và cả hai còn
  sống; dùng AABB intersection.
- Tank va chạm WATER và WALL đều chặn di chuyển, nhưng WATER không chặn
  đạn (chỉ wall chặn đạn).

### 4.5 Map format (JSONS tại `resources/maps/*.json`)

```json
{
  "name": "Màn 1 - Khu vực tập luyện",
  "difficulty": "EASY",
  "width": 20,
  "height": 15,
  "tileSize": 40,
  "enemies": [
    { "type": "AI", "difficulty": "EASY", "spawnX": 16, "spawnY": 11 }
  ],
  "rewards": { "stars": 3, "coins": 100 },
  "grid": [[1,1,...20...], ...15 rows...]
}
```

- `tileSize` mặc định 40 nếu thiếu hoặc <=0.
- `enemies`: thêm từ Task 2.3, dùng để spawn AI; nếu thiếu, server quét ô
  `SPAWN_ENEMY` làm fallback.
- `rewards`: field metadata, chưa được engine phát trên wire trong v1.

Bản đồ campaign hiện có (Phase 0):

| Map | Kích thước | Số tường | Số enemies | Difficulty metadata |
|---|---|---|---|---|
| `campaign_1` | 20×15 (tileSize 40) | 83 | 1 (EASY tại (16,11)) | EASY |
| `campaign_2` | 20×15 (tileSize 40) | 126 | 2 (MEDIUM tại (17,2), (17,12)) | MEDIUM |
| `campaign_3` | 20×15 (tileSize 40) | 148 | 3 (HARD tại (10,2), (17,7), (10,12)) | HARD |

Player spawn = ô `SPAWN_PLAYER` đầu tiên tìm được theo hàng đầu tiên.
Pixel position = `((col+0.5)*tileSize, (row+0.5)*tileSize)`.

### 4.6 Victory / Defeat

- `cleanupDeadTanks`: xóa tank hp<=0 khỏi state ngay sau
  `broadcastState()` (client nhận `alive=false` lần cuối trước khi biến
  mất).
- Nếu không còn PlayerTank alive -> `status = DEFEAT`.
- Nếu không còn AITank alive (khoảng trống sau khi dọn) -> `status = VICTORY`.
- Cập nhật status rồi gọi `broadcastState()` lần nữa -> client nhận
  gameStatus=VICTORY/DEFEAT.
- Client (`useGameSession`) tự unlock màn kế tiếp khi thấy VICTORY và lưu
  vào `localStorage['tankzors_unlocked_levels']`.

### 4.7 LEAVE_GAME / disconnect

- Client chủ động: packet `{ "type":"LEAVE_GAME" }` hoặc ngắt socket.
- Server onDisconnect synth `LEAVE_GAME` -> `gameLoop.applyInput` xoá tank.
- Khi không còn PlayerTank nào trong state -> `setStatus(WAITING)` + clear
  enemies và bullets.

## 5. Cấm thay đổi trong migration (parity contract)

Những thứ sau phải KO đổi khi migrate sang Spring Boot/LibGDX mà không cập
nhật protocol-v2 song song:

1. Endpoint `/game`.
2. Tên packet `CONNECT`, `JOIN_GAME`, `MOVE_INPUT`, `SHOOT_INPUT`,
   `STATE`, `ACK`, `PONG`, `ERROR`.
3. Tên field trong STATE: `tanks`, `bullets`, `map`, `score`,
   `gameStatus`, `timestamp`.
4. Giá trị `gameStatus`: `WAITING` | `PLAYING` | `VICTORY` | `DEFEAT`.
5. Tick rate 30Hz và mô hình server-authoritative.
6. Tuning constants ở mục 4.
7. Ngữ nghĩa JOIN_GAME reset trận khi đổi level / player chết / chưa
   PLAYING.
8. Ngữ nghĩa di chuyển chéo (normalize vector -> tốc độ bằng cardinal).
9. Map format JSON (kể cả module `enemies` và `rewards`).
10. Unlock progression lưu ở client localStorage (chưa chuyển sang server
    trong migration này).

Nếu một packet field cần đổi tên/đổi kiểu -> tạo `protocol-v2.md` và giữ
v1 hoạt động song song tới khi cutover xong (Phase 5).
