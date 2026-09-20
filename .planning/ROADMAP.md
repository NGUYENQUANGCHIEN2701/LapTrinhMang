# Roadmap: Tankzors

## Overview

Milestone v2.0 đưa repository về một kiến trúc gameplay duy nhất, hoàn thiện co-op end-to-end và harden luồng build/test để dự án có thể demo ổn định từ Docker.

## Milestones

- 🚧 **v2.0 Client-Authoritative Hardening** - Phases 1-3 (in progress)

## Phases

- [x] **Phase 1: Architectural Cleanup** - Loại bỏ kiến trúc server-authoritative cũ và xác minh stack client-authoritative.
- [ ] **Phase 2: Co-op End-to-End** - Nối lobby với host/guest game sessions và kiểm thử hai browser.
- [ ] **Phase 3: Release Hardening** - Hoàn tất security, reliability, UX và tài liệu phát hành.

## v2.0 — Client-Authoritative Hardening

**Milestone Goal:** Repository chỉ còn một kiến trúc gameplay, co-op hoạt động end-to-end và luồng build/test đủ ổn định để demo.

### Phase 1: Architectural Cleanup
**Goal**: Repository chỉ còn kiến trúc gameplay client/host-authoritative hiện tại và có đường build/test Docker hoạt động.
**Depends on**: Nothing
**Requirements**: [GAME-01, GAME-02, GAME-03, GAME-04, GAME-05, CAMP-01, CAMP-02, CAMP-03, CAMP-04, WEB-01, WEB-02, DATA-01, DATA-02, COOP-01, COOP-02, COOP-03, ARCH-01, ARCH-02, OPS-01, OPS-02, DOC-01, QUAL-01, QUAL-02, QUAL-03]
**Success Criteria** (what must be TRUE):
  1. Spring không còn entity, AI, collision hoặc game loop gameplay.
  2. WebSocket registry chỉ đăng ký `/coop`; `/game` không còn tồn tại.
  3. Campaign, Spring tests, LibGDX tests, GWT build và Docker build hoạt động.
  4. README, docs và OpenGSD mô tả cùng một kiến trúc.
**Plans**: 1 plan

Plans:
- [x] 01-01: Remove legacy server-authoritative architecture and verify the current stack.

### Phase 2: Co-op End-to-End
**Goal**: Hai browser có thể tạo/join room và chơi chung một trận qua relay `/coop` với host giữ simulation.
**Depends on**: Phase 1
**Requirements**: [COOP-04, COOP-05, COOP-06, QUAL-04]
**Success Criteria** (what must be TRUE):
  1. Launcher chọn đúng host hoặc guest từ context đáng tin cậy của lobby.
  2. Guest input tới host và host world state tới guest trong cùng room.
  3. Match result chỉ được ghi một lần và quyền host được kiểm soát.
  4. Disconnect, room close và reconnect có trạng thái UI rõ ràng.
  5. Automated integration tests và manual two-browser test đều pass.
**Plans**: 1 plan

Plans:
- [ ] 02-01: Integrate lobby context, host/guest sessions, relay tests and two-browser verification.

### Phase 3: Release Hardening
**Goal**: Bản demo v2.0 có security/reliability checks, runbook và bằng chứng xác minh đầy đủ.
**Depends on**: Phase 2
**Requirements**: [QUAL-05]
**Success Criteria** (what must be TRUE):
  1. Security review không còn finding mức high.
  2. Campaign và co-op có runbook kiểm thử lặp lại được.
  3. Docker khởi động từ clone sạch với cache trống.
  4. Requirements, roadmap và verification evidence nhất quán.
**Plans**: 1 plan

Plans:
- [ ] 03-01: Run security, clean-build, regression and documentation hardening.

## Progress

**Execution Order:** Phase 1 → Phase 2 → Phase 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Architectural Cleanup | 1/1 | Complete | 2026-09-20 |
| 2. Co-op End-to-End | 0/1 | Not started | - |
| 3. Release Hardening | 0/1 | Not started | - |
