# Roadmap: Tankzors

## Milestone: Campaign Tactical Systems

### Phase 1: Architectural Cleanup

**Goal:** Giữ một gameplay engine duy nhất trong LibGDX core và bảo đảm Spring/GWT/Docker build được.
**Status:** Complete
**Requirements:** Baseline architecture and build constraints
**Evidence:** `.planning/phases/01-architectural-cleanup/01-VERIFICATION.md`

### Phase 2: Persistent Arsenal, Battlefield Objects and Visibility

**Goal:** Ba level campaign dùng chung kho đồ, hỗ trợ Repair/Speed, menu Q, vật cản phá được, pickup, fog-of-war và HUD gọn theo ảnh tham chiếu.
**Mode:** mvp
**Status:** Active
**UI hint:** yes
**Requirements:** INV-01, INV-02, INV-03, INV-04, INV-05, MAP-01, MAP-02, MAP-03, MAP-04, VIS-01, VIS-02, UI-01, UI-02

**Success Criteria:**
1. Campaign mới bắt đầu với 100 đạn thường, 3 Repair và 2 Speed; tài nguyên còn lại đi xuyên ba level và restart phục hồi snapshot đầu level.
2. Q pause trận; Up/Down chọn; Enter đổi đạn hoặc dùng Repair/Speed rồi tự đóng và resume.
3. Cả ba map có đúng ba loại vật cản với độ bền yêu cầu và đúng số pickup đã chốt.
4. Vùng sáng thích ứng phủ ít nhất 2/3 số ô; ngoài vùng tối đen nhưng địch vẫn hiển thị.
5. HUD gọn theo ảnh tham chiếu, asset/effect lấy từ Tankzors Builds; core tests, GWT build và Docker build đều qua.

### Phase 3: Co-op End-to-End

**Goal:** Đồng bộ gameplay host-authoritative qua `/coop`, bao gồm các tactical systems sau khi campaign ổn định.
**Status:** Deferred
**Requirements:** COOP-01

## Progress

| Phase | Status | Requirements |
|-------|--------|--------------|
| 1. Architectural Cleanup | Complete | Baseline |
| 2. Campaign Tactical Systems | Active | 13 |
| 3. Co-op End-to-End | Deferred | 1 |

---
*Roadmap created: 2026-09-28 after planning recovery*
