# Networking

Tankzors hiện có hai nhóm giao tiếp: REST cho campaign và WebSocket relay cho co-op.

## REST campaign

### Bootstrap

```http
GET /api/game/bootstrap?levelId=campaign_1&mode=campaign
```

Response chứa `levelId`, `runId`, `mapVersion` và `map`. Client dùng dữ liệu này để tạo simulation local.

### Kết quả

```http
POST /api/game/results
Content-Type: application/json
```

Client gửi `runId`, level, status, score và thống kê trận. Server áp giới hạn hợp lệ, dùng `runId` để chống ghi trùng, cập nhật progress/totals/leaderboard.

Kết quả campaign là client-reported; server không chạy lại simulation để đối chiếu.

## WebSocket co-op

Endpoint duy nhất:

```text
ws://localhost:8081/coop
```

Envelope v2:

```json
{
  "version": "v2",
  "type": "PLAYER_INPUT",
  "roomId": "ABC123",
  "runId": "coop-ABC123-...",
  "seq": 1,
  "payload": {}
}
```

Nhóm packet lobby: `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `READY`, `START_MATCH`, `ROOM_STATE`, `ROOM_CLOSED`.

Nhóm packet trận: `PLAYER_INPUT` từ guest tới host; `WORLD_STATE` và `MATCH_RESULT` từ host tới guest.

Server lấy danh tính từ HTTP session trong handshake, tự gắn `senderId`, kiểm tra thành viên/phân quyền rồi relay. Server không nhận lệnh MOVE/SHOOT để cập nhật một GameLoop.

## Namespace `/game`

Không có WebSocket `/game`. Các URL `/game/libgdx/**` là file HTML/JavaScript/asset do Spring static resource handler phục vụ.
