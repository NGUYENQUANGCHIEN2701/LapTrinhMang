# Co-op relay protocol v2

Protocol v2 dùng tại WebSocket `/coop`. Server chỉ relay và quản lý phòng; host client là authority của trận.

Envelope chung gồm `version`, `type`, tùy chọn `roomId`, `runId`, `seq`, `payload`. Danh tính sender do server lấy từ session handshake.

Các hướng packet:

- Client -> relay: `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `READY`, `START_MATCH`.
- Guest -> host: `PLAYER_INPUT`.
- Host -> guest: `WORLD_STATE`, `MATCH_RESULT`, `PAUSE_MATCH`, `RESUME_MATCH`.
- Relay -> client: `CONNECT`, `ROOM_STATE`, `MATCH_STARTED`, `ROOM_CLOSED`, `ERROR`, `PONG`.

Không tái sử dụng packet v1 của engine server-authoritative.

## MATCH_STARTED hiện tại

```json
{
  "version": "v2",
  "type": "MATCH_STARTED",
  "roomId": "ABC123",
  "runId": "coop-ABC123-...",
  "seq": 0,
  "payload": {
    "levelId": "campaign_1",
    "mapVersion": "v1",
    "runId": "coop-ABC123-...",
    "hostPlayerId": "user:host",
    "members": [
      {"playerId": "user:host", "host": true},
      {"playerId": "user:guest", "host": false}
    ]
  }
}
```

Relay chỉ cho start trong lobby khi đủ ready. Host session dùng run ID/level/version và members để khởi tạo đủ player. Các send trên một WebSocket session được tuần tự hóa; khóa/xóa tài khoản đóng kết nối tương ứng. Origin mặc định cùng site.

Launcher host/guest chưa được nối vào browser game hoàn chỉnh. `MATCH_RESULT` chỉ được relay, chưa tự lưu DB; REST results hiện chỉ nhận campaign có lượt do bootstrap cấp. Chưa xem các trường `seq`/`runId` trong envelope là bảo đảm đầy đủ chống packet cũ: cần hoàn thiện kiểm tra phase/run/sequence khi nối co-op.
