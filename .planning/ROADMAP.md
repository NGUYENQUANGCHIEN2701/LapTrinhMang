# Lộ trình: Tankzors Pro

## Tổng quan

Dự án được chia thành 3 giai đoạn gọn gàng. Giai đoạn 1 xây dựng lõi game (di chuyển xe tăng, bắn, va chạm, hạ tầng WebSocket) — đây là nền tảng mà mọi thứ khác xây trên. Giai đoạn 2 thêm AI đối thủ và chế độ Campaign 3 màn. Giai đoạn 3 thêm chế độ Online + Bảng xếp hạng để hoàn thiện phần lập trình mạng. Mỗi giai đoạn kết thúc với một sản phẩm chạy được, có thể demo cho giáo viên.

## Các giai đoạn

- [ ] **Giai đoạn 1: Lõi Game + Khung cơ bản** - Xe tăng di chuyển, bắn, va chạm với tường, WebSocket client-server hoạt động
- [ ] **Giai đoạn 2: Chế độ Campaign + AI** - 3 màn tuần tự, AI đối thủ, hệ thống phần thưởng (sao + tiền + vật phẩm)
- [ ] **Giai đoạn 3: Chế độ Online + Bảng xếp hạng** - 3 bản đồ độc lập, bảng xếp hạng lưu MySQL đồng bộ trên server

## Chi tiết từng giai đoạn

### Giai đoạn 1: Lõi Game + Khung cơ bản

**Mục tiêu**: Một xe tăng di chuyển được trên bản đồ có tường, bắn đạn, va chạm hoạt động — toàn bộ qua kiến trúc WebSocket client-server, Java backend theo SOLID.

**Phụ thuộc**: Không (giai đoạn đầu tiên)
**Yêu cầu liên quan**: COMBAT-01, COMBAT-02, COMBAT-04, COMBAT-05, NET-01, NET-02, ARCH-01, ARCH-02
**Tiêu chí thành công**:

  1. React client kết nối Java WebSocket server; nhấn phím di chuyển xe tăng trên màn hình
  2. Xe tăng không đi xuyên tường — phát hiện va chạm hoạt động
  3. Nhấn bắn, đạn bay ra theo hướng xe tăng, dừng khi chạm tường
  4. Xe tăng có máu (HP) hiển thị; đạn trúng xe tăng giảm HP
  5. Code Java backend tuân thủ SOLID — có thể chỉ ra từng nguyên tắc được áp dụng ở đâu

**Kiến trúc SOLID (phải thể hiện trong giai đoạn này):**
- **S** (Nguyên tắc đơn trách nhiệm): Tách GameEngine / NetworkHandler / MapLoader / BulletManager / TankEntity
- **O** (Nguyên tắc đóng-mở): Có thể thêm loại đạn/vũ khí mới mà không sửa code cũ (dùng interface Weapon)
- **L** (Nguyên tắc thay thế Liskov): Lớp con của Tank (PlayerTank, AITank) thay thế được lớp cha Tank
- **I** (Nguyên tắc phân tách interface): Client không cần biết logic AI; AI không cần biết render
- **D** (Nguyên tắc đảo ngược phụ thuộc): GameEngine phụ thuộc vào abstraction (IMap, ITank, IBullet), không phụ thuộc vào lớp cụ thể

**Kế hoạch chi tiết**: Chưa tạo

### Giai đoạn 2: Chế độ Campaign + AI

**Mục tiêu**: Người chơi có thể chơi 3 màn Campaign tuần tự, mỗi màn có AI đối thủ với hành vi riêng, thắng nhận phần thưởng.

**Phụ thuộc**: Giai đoạn 1
**Yêu cầu liên quan**: COMBAT-03, CAMP-01, CAMP-02, CAMP-03, CAMP-04
**Tiêu chí thành công**:

  1. AI xe tăng di chuyển, tìm người chơi, và bắn lại (không chỉ đứng yên)
  2. Màn 1 dễ (1 AI chậm), Màn 2 trung bình (2 AI), Màn 3 khó (2-3 AI nhanh hơn)
  3. Phải thắng Màn 1 mới mở Màn 2, thắng Màn 2 mới mở Màn 3
  4. Thắng màn hiển thị màn hình kết quả: 3 sao + tiền thưởng + vật phẩm nhặt được
  5. Vật phẩm từ màn trước cộng vào kho đồ, có giới hạn tối đa mỗi loại

**Kế hoạch chi tiết**: Chưa tạo

### Giai đoạn 3: Chế độ Online + Bảng xếp hạng

**Mục tiêu**: Chế độ Online cho phép chọn bất kỳ bản đồ nào chơi ngay, kết quả lưu MySQL, bảng xếp hạng hiển thị cho tất cả clients.

**Phụ thuộc**: Giai đoạn 2
**Yêu cầu liên quan**: ONLINE-01, ONLINE-02, ONLINE-03, NET-03
**Tiêu chí thành công**:

  1. Từ menu Online, chọn 1 trong 3 bản đồ và bắt đầu chơi ngay (không cần mở khóa)
  2. Mỗi bản đồ có độ khó cố định (không dễ dần như Campaign)
  3. Sau khi hoàn thành bản đồ, điểm/kết quả gửi lên server và lưu vào MySQL
  4. Mở trang Bảng xếp hạng thấy thứ hạng điểm cao — refresh thấy dữ liệu mới nhất từ server
  5. 2 trình duyệt mở cùng lúc đều thấy cùng dữ liệu bảng xếp hạng

**Kế hoạch chi tiết**: Chưa tạo

## Tiến độ

**Thứ tự thực hiện:** 1 → 2 → 3

| Giai đoạn | Kế hoạch hoàn thành | Trạng thái | Ngày xong |
|-----------|---------------------|------------|-----------|
| 1. Lõi Game + Khung cơ bản | 0/Chưa xác định | Chưa bắt đầu | - |
| 2. Chế độ Campaign + AI | 0/Chưa xác định | Chưa bắt đầu | - |
| 3. Chế độ Online + Bảng xếp hạng | 0/Chưa xác định | Chưa bắt đầu | - |

## Phân bổ yêu cầu

| Yêu cầu | Giai đoạn |
|----------|-----------|
| COMBAT-01 | Giai đoạn 1 |
| COMBAT-02 | Giai đoạn 1 |
| COMBAT-03 | Giai đoạn 2 |
| COMBAT-04 | Giai đoạn 1 |
| COMBAT-05 | Giai đoạn 1 |
| CAMP-01 | Giai đoạn 2 |
| CAMP-02 | Giai đoạn 2 |
| CAMP-03 | Giai đoạn 2 |
| CAMP-04 | Giai đoạn 2 |
| ONLINE-01 | Giai đoạn 3 |
| ONLINE-02 | Giai đoạn 3 |
| ONLINE-03 | Giai đoạn 3 |
| NET-01 | Giai đoạn 1 |
| NET-02 | Giai đoạn 1 |
| NET-03 | Giai đoạn 3 |
| ARCH-01 | Giai đoạn 1 |
| ARCH-02 | Giai đoạn 1 |

**Tổng kết:** 17/17 yêu cầu đã phân bổ, không trùng lặp, không thiếu sót.

---
*Lộ trình tạo ngày: 21/08/2026 — chuyển từ 5 giai đoạn PvP sang 3 giai đoạn PvE*
