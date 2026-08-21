# Yêu cầu: Tankzors Pro

**Ngày tạo:** 21/08/2026
**Giá trị cốt lõi:** Game xe tăng PvE hoàn chỉnh với 2 chế độ chơi, thể hiện kiến thức Lập trình mạng (WebSocket client-server) và nguyên tắc SOLID trong Java backend.

## Yêu cầu phiên bản 1

Yêu cầu cho phiên bản chấm điểm. Mỗi yêu cầu tương ứng với một giai đoạn trong lộ trình.

### Chiến đấu cơ bản

- [ ] **COMBAT-01**: Người chơi điều khiển xe tăng di chuyển (lên/xuống/trái/phải) mượt mà
- [ ] **COMBAT-02**: Người chơi có thể bắn đạn; đạn di chuyển theo hướng xe tăng
- [ ] **COMBAT-03**: AI đối thủ di chuyển, xoay, và bắn lại người chơi
- [ ] **COMBAT-04**: Xe tăng có máu (HP); trúng đạn giảm máu; HP = 0 thì xe tăng bị phá hủy
- [ ] **COMBAT-05**: Bản đồ có tường/chướng ngại vật chặn di chuyển và chặn đạn

### Chế độ Campaign (Chiến dịch)

- [ ] **CAMP-01**: 3 màn chơi tuần tự (phải thắng màn N mới mở được màn N+1)
- [ ] **CAMP-02**: Mỗi màn có bản đồ riêng với bố trí chướng ngại vật và AI đối thủ khác nhau
- [ ] **CAMP-03**: Thắng màn = nhận 3 sao + tiền thưởng
- [ ] **CAMP-04**: Vật phẩm thu được cộng dồn qua các màn (có giới hạn tối đa mỗi loại)

### Chế độ Online (Trực tuyến)

- [ ] **ONLINE-01**: 3 bản đồ chơi độc lập (không cần mở khóa, chọn map bất kỳ)
- [ ] **ONLINE-02**: Mỗi bản đồ có độ khó cố định (không tăng dần như Campaign)
- [ ] **ONLINE-03**: Kết quả (điểm/sao) được gửi lên server sau mỗi trận

### Lập trình mạng

- [ ] **NET-01**: Client kết nối server qua WebSocket; server xử lý logic game (server quản lý trạng thái)
- [ ] **NET-02**: Client gửi lệnh (di chuyển/bắn), server xử lý và gửi trạng thái game về client
- [ ] **NET-03**: Bảng xếp hạng lưu trên server, nhiều client có thể xem đồng thời

### Kiến trúc phần mềm

- [ ] **ARCH-01**: Java backend thiết kế theo nguyên tắc SOLID (SRP, OCP, LSP, ISP, DIP)
- [ ] **ARCH-02**: Tách biệt rõ ràng: Game Engine / Tầng Mạng / Logic AI / Dữ liệu Bản đồ

## Yêu cầu phiên bản 2 (Phát triển sau - ghi vào README)

- Thêm nhiều màn chơi hơn (5-10 mỗi chế độ)
- Thêm loại xe tăng khác nhau (xe nhanh, xe hạng nặng, xe trinh sát)
- Thêm vũ khí đặc biệt (tên lửa, mìn, khiên)
- Chế độ PvP nhiều người chơi
- Hệ thống tài khoản đầy đủ
- Âm thanh và hiệu ứng hình ảnh
- Vật phẩm tăng sức mạnh trên bản đồ

## Ngoài phạm vi

| Tính năng | Lý do |
|-----------|-------|
| PvP nhiều người chơi thời gian thực | Quá phức tạp; đồng bộ trạng thái cho 2+ người vượt quá thời gian cho phép |
| Hệ thống tài khoản (JWT, OAuth) | Không cần thiết cho demo 3 màn; tập trung vào gameplay + mạng |
| Thoại/Video chat | Hoàn toàn ngoài phạm vi môn học |
| Ứng dụng di động | Ưu tiên web, React Canvas là đủ |
| Hơn 3 màn chơi | Đủ demo; ghi vào kế hoạch phiên bản 2 |
| Ghép trận theo trình độ | Không có PvP nên không cần |
| Trình chỉnh sửa bản đồ | Phạm vi riêng biệt, dùng bản đồ cố định |

## Bảng theo dõi yêu cầu

| Yêu cầu | Giai đoạn | Trạng thái |
|----------|-----------|------------|
| COMBAT-01 | Giai đoạn 1 | Chưa làm |
| COMBAT-02 | Giai đoạn 1 | Chưa làm |
| COMBAT-03 | Giai đoạn 2 | Chưa làm |
| COMBAT-04 | Giai đoạn 1 | Chưa làm |
| COMBAT-05 | Giai đoạn 1 | Chưa làm |
| CAMP-01 | Giai đoạn 2 | Chưa làm |
| CAMP-02 | Giai đoạn 2 | Chưa làm |
| CAMP-03 | Giai đoạn 2 | Chưa làm |
| CAMP-04 | Giai đoạn 2 | Chưa làm |
| ONLINE-01 | Giai đoạn 3 | Chưa làm |
| ONLINE-02 | Giai đoạn 3 | Chưa làm |
| ONLINE-03 | Giai đoạn 3 | Chưa làm |
| NET-01 | Giai đoạn 1 | Chưa làm |
| NET-02 | Giai đoạn 1 | Chưa làm |
| NET-03 | Giai đoạn 3 | Chưa làm |
| ARCH-01 | Giai đoạn 1 | Chưa làm |
| ARCH-02 | Giai đoạn 1 | Chưa làm |

**Tổng kết:**
- Tổng yêu cầu v1: 17
- Đã phân bổ vào giai đoạn: 17
- Chưa phân bổ: 0

---
*Ngày tạo yêu cầu: 21/08/2026*
