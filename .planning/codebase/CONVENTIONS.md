# Quy ước codebase

- Java package theo trách nhiệm, không theo “phase migration”.
- REST DTO và persistence entity tách khỏi gameplay entity.
- Thay đổi gameplay phải có test trong `game-client/core/src/test`.
- Thay đổi Spring service/controller phải có test H2/MockMvc phù hợp.
- Không sửa generated bundle trong `server-spring/src/main/resources/static/game/libgdx` bằng tay.
- Asset nguồn đặt trong `game-client/assets`.
- Map JSON nguồn cần đồng bộ giữa client assets và Spring resources khi schema thay đổi.
- Docker build là kiểm tra tích hợp cuối cho thay đổi chạm cả client và server.
