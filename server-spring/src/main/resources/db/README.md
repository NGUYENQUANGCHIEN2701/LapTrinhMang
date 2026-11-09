# Schema và Flyway migration

Spring dùng Flyway tạo/nâng cấp schema; Hibernate chỉ `validate`. Các SQL ở [migration/](migration/) được áp dụng theo version khi app hoặc Spring integration test khởi động. PostgreSQL 16 là database runtime; H2 PostgreSQL mode dùng trong test.

## Bảng hiện tại

| Bảng | Khóa/quy tắc chính | Mục đích |
|---|---|---|
| `app_user` | `id`, username unique | Tài khoản, BCrypt, display name, role, enabled |
| `player_progress` | `(session_id, unlocked_level)` | Màn đã mở và trạng thái completed |
| `player_totals` | `player_id` | Số dư Xu/Sao, revision và row dùng khóa các thay đổi account |
| `player_shop_state` | `player_id` | Research và inventory dạng chuỗi JSON |
| `game_run` | `run_id` | Lượt cấp phát, owner, level/version, hạn, pickup và mốc kho shop |
| `shop_request` | `request_id` (nội bộ gắn account) | Hash chống áp dụng lặp batch giao dịch |
| `match_result` | `id`, `run_id` unique | Kết quả client báo, hash, thống kê, phần thưởng ghi nhận |
| `leaderboard` | `id`, `(player_name, map_id)` unique | Điểm tốt nhất mỗi người/màn |

Người đăng nhập có khóa `user:<username>`. Cột `player_progress.session_id` là tên lịch sử: với tài khoản đã đăng nhập, nó chứa khóa người chơi, không phải cookie `JSESSIONID`. Leaderboard đang lưu theo tên tài khoản. Các bảng chưa dùng foreign key thống nhất theo `app_user.id`; khi đổi định danh hoặc xóa user cần xem đầy đủ các use case liên quan.

## Migration đã có

| Version | Nội dung |
|---|---|
| V1 | Leaderboard và lịch sử trận |
| V2 | Progress |
| V3 | Run ID, payload hash và các trường chống ghi trùng |
| V4 | Ví và phần thưởng ghi trong lịch sử |
| V5 | Tài khoản |
| V6 | Shop state |
| V7 | Trạng thái màn đã hoàn thành và chuyển đổi dữ liệu cũ |
| V8 | Issued run, request journal, revision và khởi tạo inventory chưa có dữ liệu |

`ResultService` hiện cộng thưởng theo map/nghiên cứu trong transaction kết quả. `PlayerShopService` thay ví/kho/nghiên cứu qua thao tác được kiểm tra; cả hai dùng `PlayerStateLock` đến commit. Không ghi trực tiếp repository ví trong API mới để bỏ qua khóa này.

## Thêm hoặc đổi schema

1. Kiểm tra version lớn nhất trong `migration/`; tạo `V<version-tiếp-theo>__<mo_ta>.sql` (hiện kế tiếp là V9 nếu chưa có thay đổi khác).
2. Thiết kế giá trị mặc định/backfill cho row cũ. Nếu thêm constraint, xử lý dữ liệu cũ vi phạm trước khi bật constraint.
3. Cập nhật entity, repository, service và DTO; giới hạn request phải phù hợp độ dài cột và miền giá trị.
4. Chạy Spring test với H2; kiểm tra migration trên PostgreSQL sạch và trên dữ liệu ở version trước bằng môi trường thử riêng. Không chỉ kiểm tra tạo database mới.
5. Chạy Docker build/start; xác nhận Flyway áp dụng đúng version và Hibernate validate thành công.

Không sửa file đã áp dụng; Flyway lưu checksum trong `flyway_schema_history`. Không dùng `ddl-auto=update` để che lỗi migration. Nếu gặp checksum mismatch, đối chiếu file với lịch sử repo và database; chỉ tạo lại database khi dữ liệu thử có thể bỏ. Không dùng `repair` hoặc `baseline` khi chưa xác định nguyên nhân.

Backup/restore và lệnh SQL kiểm tra ở [infra README](../../../../../infra/README.md) và [runbook](../../../../../docs/runbook.md).
