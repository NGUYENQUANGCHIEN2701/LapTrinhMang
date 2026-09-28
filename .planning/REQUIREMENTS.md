# Requirements: Tankzors

**Defined:** 2026-09-28  
**Core Value:** Người chơi hoàn thành ba màn campaign với tài nguyên và gameplay nhất quán xuyên màn.

## Current Milestone Requirements

### Inventory and Consumables

- [ ] **INV-01**: Một campaign mới khởi tạo đúng 100 đạn thường, 3 Repair và 2 Speed; không khởi tạo lại khi sang level.
- [ ] **INV-02**: Đạn, Repair, Speed và pickup còn lại được giữ nguyên từ level 1 sang level 2 và level 3.
- [ ] **INV-03**: Restart level khôi phục HP, hiệu ứng và inventory về snapshot lúc bắt đầu level đó.
- [ ] **INV-04**: Nhấn Q khi đang chơi tạm dừng simulation và mở menu các mục đang sở hữu; Up/Down đổi dòng; Enter chọn vũ khí hoặc dùng consumable, tự đóng menu và tiếp tục trận.
- [ ] **INV-05**: Repair hồi đầy HP và chỉ tiêu hao khi người chơi đang thiếu HP; Speed tăng tốc độ xe ta 30% trong 20 giây, dùng lại chỉ làm mới thời gian và không cộng dồn.

### Battlefield Objects

- [ ] **MAP-01**: Tường gạch/xám chặn xe và đạn, vỡ sau 4 lần trúng đạn; tường trắng chặn xe/đạn và không thể phá; bụi/cỏ xanh chặn xe/đạn và vỡ sau 1 lần trúng.
- [ ] **MAP-02**: Ba map chứa pickup cố định: level 1 có 1 Repair; level 2 có 1 Repair và 1 Speed; level 3 có 2 Repair và 1 Speed.
- [ ] **MAP-03**: Xe ta đi vào ô pickup sẽ thu thập đúng 1 vật phẩm, xóa pickup khỏi map và giữ vật phẩm qua các level sau.
- [ ] **MAP-04**: Asset, animation/ảnh vỡ và âm thanh cho vật cản/pickup lấy từ bộ Tankzors Builds đã cung cấp.

### Visibility and HUD

- [ ] **VIS-01**: Khi trận đang chạy, vùng sáng thích ứng quanh xe ta luôn bao phủ ít nhất 2/3 số ô map; phần còn lại tối đen hoàn toàn và không lưu lịch sử khám phá.
- [ ] **VIS-02**: Xe địch còn sống vẫn được hiển thị ngoài vùng sáng; terrain, pickup, đạn và hiệu ứng ngoài vùng sáng bị che.
- [ ] **UI-01**: HUD trong trận dùng bố cục gọn bên trái gồm HP, đạn/vũ khí hiện tại, Repair và Speed cùng số lượng; bỏ panel Reverse, Coins và Stars khỏi HUD đang chơi.
- [ ] **UI-02**: Menu Q có selection rõ ràng bằng icon/tên/số lượng và không để phím Up/Down đồng thời điều khiển xe.

## Deferred

- **COOP-01**: Đồng bộ inventory, fog, destructible terrain và pickup trong co-op host-authoritative.
- **ITEM-01**: Thêm các consumable/special weapon ngoài Repair và Speed.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Simulation gameplay phía Spring | Vi phạm kiến trúc client/host-authoritative |
| Random pickup drops | Người dùng đã chốt pickup cố định theo từng level |
| Lưu vùng fog đã khám phá | Người dùng yêu cầu ngoài phạm vi hiện tại tối đen hoàn toàn |
| Asset tự vẽ thay bộ Builds | Người dùng yêu cầu ưu tiên asset repo tham chiếu |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| INV-01..05 | Phase 4 | Pending |
| MAP-01..04 | Phase 4 | Pending |
| VIS-01..02 | Phase 4 | Pending |
| UI-01..02 | Phase 4 | Pending |
| COOP-01 | Deferred | Deferred |
| ITEM-01 | Deferred | Deferred |

**Coverage:**
- Current milestone requirements: 13
- Mapped to phases: 13
- Unmapped: 0

---
*Requirements defined: 2026-09-28*
