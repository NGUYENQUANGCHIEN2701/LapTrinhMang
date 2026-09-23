# Co-op relay protocol v2

Protocol v2 dùng tại WebSocket `/coop`. Server chỉ relay và quản lý phòng; host client là authority của trận.

Envelope chung gồm `version`, `type`, tùy chọn `roomId`, `runId`, `seq`, `payload`. Danh tính sender do server lấy từ session handshake.

Các hướng packet:

- Client -> relay: `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `READY`, `START_MATCH`.
- Guest -> host: `PLAYER_INPUT`.
- Host -> guest: `WORLD_STATE`, `MATCH_RESULT`, `PAUSE_MATCH`, `RESUME_MATCH`.
- Relay -> client: `CONNECT`, `ROOM_STATE`, `MATCH_STARTED`, `ROOM_CLOSED`, `ERROR`, `PONG`.

Không tái sử dụng packet v1 của engine server-authoritative.
