# Tankzors Pro

## Mô tả dự án

Game hành động xe tăng PvE trên website cho môn "Lập trình mạng". Người chơi điều khiển xe tăng, chiến đấu với AI trên các bản đồ có chướng ngại vật. Code hiện tại dùng Java WebSocket server và React Canvas client; Spring Boot, Thymeleaf, LibGDX HTML5 và PostgreSQL là stack đích cho các phase sau. Lấy cảm hứng từ game Tankzors (GitHub - Tankzors/Builds).

## Giá trị cốt lõi

Người chơi có thể chơi game xe tăng qua 2 chế độ (Campaign tuần tự + Online độc lập) với server Java xử lý toàn bộ logic, thể hiện kiến thức Lập trình mạng qua kiến trúc client-server WebSocket và nguyên tắc SOLID. Website có menu, settings, save/load, shop, research, debriefing và leaderboard.

## Yêu cầu

### Đã hoàn thành

- [x] Phase 0 baseline (13/09/2026): 112 server tests + 62 client tests + build pass.
  Protocol-wire-format v1 chốt tại `docs/protocol-v1.md`; danh sách
  behavior phải giữ tại `docs/parity-checklist.md`.
- [x] Phase 1 target structure (13/09/2026): Tạo 4 module mới
  (`shared-protocol/`, `server-spring/`, `game-client/`, `infra/`) +
  `docs/migration-mapping.md` mapping class cũ → vị trí mới. Legacy
  code (Javalin + React Canvas) chưa bị xóa, vẫn dùng làm parity
  reference đến Phase 5.

### Đang thực hiện

- [ ] Người chơi điều khiển xe tăng di chuyển và bắn trong thời gian thực
- [ ] AI đối thủ di chuyển, bắn lại người chơi
- [ ] Bản đồ có tường/chướng ngại vật chặn di chuyển và đạn
- [ ] Xe tăng có máu (HP), bị bắn giảm máu, hết máu = thua
- [ ] Chế độ Campaign: 3 màn chơi tuần tự (phải thắng màn trước mới mở màn sau)
- [ ] Thắng màn = nhận 3 sao + tiền thưởng + vật phẩm (cộng dồn, có giới hạn tối đa)
- [ ] Chế độ Online: 3 bản đồ chơi độc lập (không cần mở khóa, mỗi map có độ khó cố định)
- [ ] Bảng xếp hạng lưu trên PostgreSQL, nhiều client xem được cùng lúc

### Ngoài phạm vi

- Chơi nhiều người PvP (quá phức tạp cho phạm vi hiện tại)
- Nhiều hơn 3 màn chơi (phát triển sau)
- Ứng dụng di động
- Thoại/video chat
- Hệ thống tài khoản phức tạp (JWT, OAuth)
- Ghép trận theo trình độ

## Bối cảnh

- Đồ án môn Lập trình mạng, làm một mình
- Lấy cảm hứng gameplay từ Tankzors (GitHub - Tankzors/Builds)
- Mục tiêu: trình bày kiến thức Lập trình mạng + SOLID qua game hoàn chỉnh
- Phạm vi nhỏ gọn: 3 màn chơi, 2 chế độ, đủ để chấm điểm
- Phát triển thêm màn/tính năng sẽ ghi vào README cho kế hoạch tương lai

## Ràng buộc

- **Công nghệ hiện tại**: Java 21 + Maven + Javalin WebSocket + Gson + React + TypeScript + Vite + Canvas
- **Công nghệ đích (chưa triển khai)**: Spring Boot + Thymeleaf + LibGDX HTML5 + WebSocket + PostgreSQL
- **SOLID**: Backend Java phải tuân thủ nguyên tắc SOLID (yêu cầu môn học, card [G02])
- **Phạm vi**: 3 màn cho mỗi chế độ, đủ demo — không làm quá
- **Thời gian**: Vài tuần, làm một mình
- **Mạng**: Phải thể hiện giao tiếp client-server qua WebSocket

## Quyết định chính

| Quyết định | Lý do | Kết quả |
|------------|-------|---------|
| PvE (đánh máy) thay vì PvP nhiều người | Giảm độ phức tạp đồng bộ trạng thái, tập trung vào SOLID + kiến trúc mạng | Đã xác nhận |
| 3 màn mỗi chế độ | Đủ demo hệ thống tiến trình mà không quá nhiều nội dung | Đã xác nhận |
| Server authoritative | Server xử lý logic, client chỉ gửi input và hiển thị — chống sai lệch trạng thái | Đã xác nhận |
| Spring Boot + Thymeleaf | Stack đích để migration website Java sau khi refactor hiện tại ổn định | Để sau |
| LibGDX HTML5 | Stack đích để migration gameplay client sau khi protocol ổn định | Để sau |
| PostgreSQL | Stack đích cho save, progression và leaderboard | Để sau |
| Campaign tuần tự + Online độc lập | 2 chế độ khác nhau từ cùng 3 bản đồ, tăng giá trị demo | Đã xác nhận |

## Lịch sử thay đổi

Tài liệu này được cập nhật khi chuyển giai đoạn hoặc hoàn thành mốc quan trọng.

---
*Cập nhật lần cuối: 13/09/2026 — khóa phạm vi refactor hiện tại; migration stack và tính năng PDF chuyển thành TODO*
