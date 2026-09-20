# Requirements: Tankzors

**Defined:** 2026-09-20
**Core Value:** Người chơi mở game từ website, chơi campaign trong trình duyệt và lưu kết quả bằng một kiến trúc mạng rõ ràng, dễ chạy và dễ giải thích.

## v2.0 Requirements

### Gameplay Client

- [x] **GAME-01**: Người chơi di chuyển và bắn bằng bàn phím trong LibGDX.
- [x] **GAME-02**: AI di chuyển, ngắm và bắn theo độ khó của map.
- [x] **GAME-03**: Collision tank, tường, đạn và HP chạy trong client simulation.
- [x] **GAME-04**: Trận có trạng thái WAITING, PLAYING, VICTORY và DEFEAT.
- [x] **GAME-05**: Renderer dùng snapshot và không sở hữu hoặc sửa world state.

### Campaign

- [x] **CAMP-01**: Có ba màn campaign với map, enemy và reward riêng.
- [x] **CAMP-02**: Client tải map và run metadata qua `GET /api/game/bootstrap`.
- [x] **CAMP-03**: Client gửi kết quả idempotent qua `POST /api/game/results`.
- [x] **CAMP-04**: Thắng màn mở khóa màn kế tiếp và cập nhật tổng thưởng.

### Website and Data

- [x] **WEB-01**: Người dùng đăng ký, đăng nhập và đăng xuất bằng Spring Security session.
- [x] **WEB-02**: Website có menu, campaign, play, settings, leaderboard và admin.
- [x] **DATA-01**: PostgreSQL schema được quản lý bằng Flyway.
- [x] **DATA-02**: Match result chống ghi trùng theo run và cập nhật progress, totals và leaderboard.

### Co-op Networking

- [x] **COOP-01**: Server cung cấp WebSocket `/coop` để quản lý room và relay packet v2.
- [x] **COOP-02**: Host session chạy simulation và phát WORLD_STATE cùng MATCH_RESULT.
- [x] **COOP-03**: Guest session gửi PLAYER_INPUT và render snapshot từ host.
- [ ] **COOP-04**: Lobby truyền đúng room, player, role, run và map vào LibGDX host hoặc guest session.
- [ ] **COOP-05**: Hai browser hoàn thành create, join, ready, start và chơi một trận end-to-end.
- [ ] **COOP-06**: Disconnect, room close và reconnect có trạng thái UI rõ ràng và không để session treo.

### Architecture and Operations

- [x] **ARCH-01**: Server không chứa gameplay entity, AI, collision hoặc game loop.
- [x] **ARCH-02**: Không còn WebSocket `/game`, React client hoặc Javalin server.
- [x] **OPS-01**: Một lệnh Docker Compose build và chạy client, server cùng database.
- [x] **OPS-02**: Unit test client, test Spring, GWT build và Docker build đều thành công.
- [x] **DOC-01**: README, docs và OpenGSD mô tả đúng kiến trúc hiện tại.

### Quality

- [x] **QUAL-01**: Campaign simulation dùng fixed-step để hành vi không phụ thuộc FPS.
- [x] **QUAL-02**: Result submit idempotent và có giới hạn dữ liệu phía server.
- [x] **QUAL-03**: Build toolchain được cố định trong Docker bằng JDK 17 và 21.
- [ ] **QUAL-04**: Co-op có test tích hợp tự động cho quyền host, membership và packet direction.
- [ ] **QUAL-05**: Không có finding mức high từ security review trước khi hoàn thành milestone.

## Future Requirements

### Competitive Networking

- **NET-01**: PvP cạnh tranh có reconciliation hoặc rollback phù hợp.
- **NET-02**: Matchmaking ghép người chơi theo tiêu chí xác định.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Server-authoritative gameplay | Mâu thuẫn với kiến trúc hiện tại và tạo gameplay engine thứ hai |
| React/Vite/Javalin stack | Đã được thay bằng Thymeleaf và Spring Boot |
| Mobile application | Không phục vụ milestone v2.0 |
| Voice/video chat | Không phục vụ core gameplay |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| GAME-01 | Phase 1 | Complete |
| GAME-02 | Phase 1 | Complete |
| GAME-03 | Phase 1 | Complete |
| GAME-04 | Phase 1 | Complete |
| GAME-05 | Phase 1 | Complete |
| CAMP-01 | Phase 1 | Complete |
| CAMP-02 | Phase 1 | Complete |
| CAMP-03 | Phase 1 | Complete |
| CAMP-04 | Phase 1 | Complete |
| WEB-01 | Phase 1 | Complete |
| WEB-02 | Phase 1 | Complete |
| DATA-01 | Phase 1 | Complete |
| DATA-02 | Phase 1 | Complete |
| COOP-01 | Phase 1 | Complete |
| COOP-02 | Phase 1 | Complete |
| COOP-03 | Phase 1 | Complete |
| COOP-04 | Phase 2 | Pending |
| COOP-05 | Phase 2 | Pending |
| COOP-06 | Phase 2 | Pending |
| ARCH-01 | Phase 1 | Complete |
| ARCH-02 | Phase 1 | Complete |
| OPS-01 | Phase 1 | Complete |
| OPS-02 | Phase 1 | Complete |
| DOC-01 | Phase 1 | Complete |
| QUAL-01 | Phase 1 | Complete |
| QUAL-02 | Phase 1 | Complete |
| QUAL-03 | Phase 1 | Complete |
| QUAL-04 | Phase 2 | Pending |
| QUAL-05 | Phase 3 | Pending |

**Coverage:**
- v2.0 requirements: 29 total
- Mapped to phases: 29
- Unmapped: 0

---
*Requirements defined: 2026-09-20*
*Last updated: 2026-09-20 after OpenGSD recovery*
