# Roadmap: Tankzors

## Overview

Hoàn thiện campaign ba màn theo hướng client-authoritative trước khi mở rộng co-op.

## Phases

- [x] **Phase 1: Architectural Cleanup** - Một gameplay engine trong LibGDX core.
- [ ] **Phase 2: Co-op End-to-End** - Đồng bộ host-authoritative (deferred).
- [ ] **Phase 3: Release Hardening** - Kiểm tra và đóng gói release (deferred).
- [ ] **Phase 4: Campaign Tactical Systems** - Inventory, vật phẩm, vật cản, tầm nhìn và HUD.

## Phase Details

### Phase 1: Architectural Cleanup

**Goal**: Giữ một gameplay engine duy nhất trong LibGDX core và bảo đảm Spring/GWT/Docker build được.
**Depends on**: Nothing
**Requirements**: Baseline architecture and build constraints
**Plans**: Complete
**Evidence**: `.planning/phases/01-architectural-cleanup/01-VERIFICATION.md`

### Phase 2: Co-op End-to-End

**Goal**: Đồng bộ gameplay host-authoritative qua `/coop`.
**Depends on**: Phase 4
**Requirements**: COOP-01
**Plans**: Deferred

### Phase 3: Release Hardening

**Goal**: Kiểm tra và đóng gói release sau khi campaign/co-op ổn định.
**Depends on**: Phase 2
**Requirements**: Deferred
**Plans**: Deferred

### Phase 4: Campaign Tactical Systems

**Goal**: Ba level campaign dùng chung kho đồ, hỗ trợ Repair/Speed, menu Q, vật cản phá được, pickup, fog-of-war và HUD gọn theo ảnh tham chiếu.
**Depends on**: Phase 1
**Requirements**: INV-01, INV-02, INV-03, INV-04, INV-05, MAP-01, MAP-02, MAP-03, MAP-04, VIS-01, VIS-02, UI-01, UI-02
**Plans**: 1 plan
**Mode:** mvp
**Status:** Human UAT
**UI hint:** yes

**Success Criteria:**
1. Campaign mới bắt đầu với 100 đạn thường, 3 Repair và 2 Speed; tài nguyên còn lại đi xuyên ba level và restart phục hồi snapshot đầu level.
2. Q pause trận; Up/Down chọn; Enter đổi đạn hoặc dùng Repair/Speed rồi tự đóng và resume.
3. Cả ba map có đúng ba loại vật cản với độ bền yêu cầu và đúng số pickup đã chốt.
4. Vùng sáng thích ứng phủ ít nhất 2/3 số ô; ngoài vùng tối đen nhưng địch vẫn hiển thị.
5. HUD gọn theo ảnh tham chiếu, asset/effect lấy từ Tankzors Builds; core tests, GWT build và Docker build đều qua.

Plans:
- [x] 04-01: Campaign inventory, battlefield objects, visibility and HUD

## Progress

| Phase | Status | Requirements |
|-------|--------|--------------|
| 1. Architectural Cleanup | Complete | Baseline |
| 2. Co-op End-to-End | Deferred | 1 |
| 3. Release Hardening | Deferred | Deferred |
| 4. Campaign Tactical Systems | Human UAT | 13 |

---
*Roadmap created: 2026-09-28 after planning recovery*
