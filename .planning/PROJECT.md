# Tankzors

## What This Is

Tankzors là game xe tăng PvE chạy trên website cho đồ án Lập trình mạng. Spring Boot/Thymeleaf cung cấp website, tài khoản và persistence; LibGDX HTML5 sở hữu gameplay, AI, collision và render của campaign.

## Core Value

Người chơi có thể mở game từ website, hoàn thành ba màn campaign trong trình duyệt và giữ xuyên màn toàn bộ đạn/vật phẩm bằng gameplay rõ ràng, nhất quán.

## Requirements

### Validated

- ✓ Campaign client-authoritative chạy bằng LibGDX core và nạp map qua REST — existing
- ✓ Spring giữ web/API/persistence, không chạy gameplay engine thứ hai — Phase 1
- ✓ Campaign có ba level, AI bốn hướng, đạn hữu hạn và workshop sau chiến thắng — existing
- ✓ GWT, Spring và Docker multi-stage build thành công — Phase 1

### Active

- [ ] Kho đồ campaign mặc định gồm 100 đạn thường, 3 Repair và 2 Speed; số còn lại đi xuyên ba level
- [ ] Menu Q tạm dừng trận, cho chọn bằng phím lên/xuống và Enter chọn/dùng rồi đóng menu
- [ ] Repair hồi đầy HP; Speed tăng 30% trong 20 giây
- [ ] Restart phục hồi kho đồ đúng trạng thái đầu level
- [ ] Ba loại vật cản có độ bền 4 hit, không thể phá và 1 hit
- [ ] Pickup Repair/Speed cố định trên ba map và cộng vào kho đồ khi xe đi qua
- [ ] Fog-of-war chiếu sáng ít nhất 2/3 diện tích map, phần còn lại tối đen nhưng vẫn thấy địch
- [ ] HUD trong trận gọn theo bố cục Tankzors Lux và dùng asset/effect từ kho Builds

### Out of Scope

- Co-op parity cho các cơ chế mới — phase riêng vì co-op cần host-authoritative synchronization
- Thêm loại vật phẩm ngoài đạn, Repair và Speed — chưa được yêu cầu
- Thay gameplay engine bằng simulation phía Spring — trái ranh giới kiến trúc

## Context

- Codebase đã có `PlayerShopState`, sáu loại đạn, asset `icons.png`, `powerups.png`, âm thanh repair/speed và cơ chế giữ inventory khi chuyển level.
- Map hiện chỉ hỗ trợ ground, wall, water và spawn; wall chưa có HP và pickup chưa có entity/snapshot.
- HUD hiện chiếm bốn panel ngang trên cùng; yêu cầu mới chuyển sang dải trạng thái gọn bên trái.
- Asset chuẩn tham chiếu: https://github.com/Tankzors/Builds/tree/master

## Constraints

- **Gameplay ownership**: Mọi simulation mới nằm trong `game-client/core`; Spring không nhận entity/AI/collision/game loop.
- **Build**: GWT dùng JDK 17 trong Docker, Spring dùng Java 21.
- **Assets**: Asset nguồn đặt trong `game-client/assets`; không sửa bundle sinh ra trong Spring static resources.
- **Maps**: Schema map thay đổi phải đồng bộ client assets và Spring resources.
- **Verification**: Gameplay mới phải có test core; Docker multi-stage là gate tích hợp cuối.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Repair hồi đầy HP | Người dùng xác nhận | — Pending |
| Speed +30% trong 20 giây | Người dùng xác nhận | — Pending |
| Q pause; Enter chọn/dùng và tự đóng | Không để AI hành động khi người chơi chọn đồ | — Pending |
| Fog ngoài phạm vi tối đen, vùng sáng ≥2/3 map | Giữ cảm giác khám phá nhưng vùng tối ít | — Pending |
| Restart khôi phục snapshot đầu level | Tránh mất tài nguyên do chơi lại | — Pending |
| Dùng asset Tankzors Builds | Khớp giao diện và hiệu ứng tham chiếu | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition:**
1. Move validated requirements from Active to Validated.
2. Record new decisions and surfaced constraints.
3. Keep scope boundaries aligned with current architecture.

---
*Last updated: 2026-09-28 after planning recovery and campaign tactical systems clarification*
