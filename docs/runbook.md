# Runbook Tankzors

Workflow chuẩn là Docker multi-stage, build GWT bằng JDK 17 và Spring bằng JDK 21. Chạy lệnh từ root repository. Chi tiết cấu hình, Watch và backup/restore nằm trong [infra README](../infra/README.md).

## 1. Khởi động và xác nhận

```powershell
docker version
docker compose version
docker compose -f infra/docker-compose.yml up --build -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs --tail 200 app
curl.exe http://localhost:8081/api/health
```

Database phải healthy và log app có `Started TankzorsApplication`. Health `UP` chỉ xác nhận HTTP của app. Tiếp tục smoke test:

1. Mở `http://localhost:8081/`, đăng ký hoặc đăng nhập.
2. Vào màn đang mở; Network có bootstrap `200`, game tải đủ ảnh/âm thanh.
3. Chơi đến kết thúc; kiểm tra POST kết quả trả `saved: true`, xem shop sync nếu ví/inventory đổi.
4. Reload trang, kiểm tra tiến trình, ví và bảng xếp hạng phù hợp. Đóng/mở lại app để xác nhận dữ liệu DB còn nguyên.

Admin chỉ được seed nếu chưa tồn tại và `ADMIN_PASSWORD` có giá trị. Compose mặc định `admin123` cho dev; thay biến không đổi mật khẩu đã lưu. Đọc [các hạn chế](../README.md#10-trạng-thái-và-hạn-chế) trước khi đưa bản dev ra môi trường dùng chung.

## 2. Sau khi sửa code

```powershell
docker compose -f infra/docker-compose.yml up --build -d
```

`restart app` không build source. Sau khi app khởi động, tải lại browser để dùng bundle mới. Mỗi lần thay app sẽ mất session/phòng trong RAM; database giữ trong volume. Dockerfile không chạy test, xem [DEVELOPMENT](DEVELOPMENT.md#6-kiểm-tra-và-bàn-giao) để chạy test trước.

Chạy local ngoài Docker chỉ khi cần debug: bật riêng service `postgres`, build `html:copyToServer` bằng JDK 17, rồi chạy Spring bằng JDK 21/Maven. Trình tự đầy đủ ở [README gốc](../README.md#chạy-không-dùng-docker).

## 3. Chẩn đoán theo triệu chứng

| Triệu chứng | Kiểm tra và xử lý |
|---|---|
| Không có lệnh Docker | Kiểm tra Docker, PATH và Docker Desktop; chưa thể kiểm chứng pipeline Docker chỉ bằng Maven |
| App không nối được DB | Xem `ps`, log `postgres`, credential; Compose dùng `postgres:5432`, server local dùng cổng host |
| Cổng bận | Đổi `POSTGRES_HOST_PORT` cho DB; đổi mapping host `8081:8081` nếu cổng web bận |
| GWT build hết bộ nhớ | Cấp thêm RAM cho Docker; GWT có heap cấu hình 2 GB, toàn stack cần phần bộ nhớ khác |
| Thiếu asset/màn đen | Console WebGL/GWT, Network `404`; build lại toàn image, kiểm tra source asset và chữ hoa/thường |
| Xe không nhận phím | Click iframe; đóng briefing/túi đồ/cửa hàng trước khi thử |
| Bootstrap `403` | Level chưa mở hoặc đã thắng; kiểm tra progress, không bypass ở client |
| API `401` | Phiên hết hạn/chưa đăng nhập; đăng nhập lại, không retry vô hạn |
| Kết quả `400` | Xem body lỗi; kiểm tra level, status và miền giá trị; không mặc định là lỗi mạng |
| Kết quả `409` | Cùng run ID có payload khác; không coi là đã lưu nội dung mới |
| Thắng nhưng không mở màn | Đọc trạng thái lưu; kiểm tra POST results và DB. Game chờ xác nhận trước Next; 401 cần đăng nhập lại |
| Giao dịch bị từ chối/409 | Không coi là đã lưu; nhấn R nạp lại state server. Request bị từ chối được giữ riêng trong sessionStorage để chẩn đoán |
| Flyway validate lỗi | Đối chiếu migration, checksum và schema; không sửa SQL cũ hoặc reset dữ liệu cần giữ |

Không có WebSocket `/game`. `/game/libgdx/**` là static bundle; relay co-op duy nhất là `/coop`. Handshake relay cần phiên đăng nhập. Campaign không cần WebSocket để di chuyển/bắn. Co-op chưa được nối thành luồng chơi hoàn chỉnh, vì vậy mở socket thành công chưa chứng minh đã chơi co-op được.

## 4. Kiểm tra dữ liệu

```powershell
docker exec -it tankzors-postgres psql -U tankzors -d tankzors
```

Trong psql:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT run_id, player_id, map_id, status, score FROM match_result ORDER BY id DESC LIMIT 20;
SELECT * FROM player_progress WHERE session_id = 'user:<username>';
SELECT * FROM player_totals WHERE player_id = 'user:<username>';
SELECT player_name, map_id, score FROM leaderboard ORDER BY score DESC LIMIT 20;
```

Progress reset qua `POST /api/progress/reset` cần cookie và CSRF token; thao tác này không reset ví/nghiên cứu/lịch sử. Chỉ chạy `down -v` khi chủ đích xóa toàn bộ database thử; với dữ liệu cần giữ, backup và thử restore trước khi thay schema. Xem [database README](../server-spring/src/main/resources/db/README.md).

## 5. Bằng chứng khi bàn giao sự cố

Ghi commit, cách chạy (Docker/local), bước tái hiện, HTTP status/body, đoạn log liên quan và kết quả kiểm tra DB. Loại cookie, mật khẩu và dữ liệu tài khoản khỏi log chia sẻ. Đánh dấu lỗi đã tái hiện hay mới suy ra từ code, kiểm tra nào chưa chạy. Danh sách vấn đề của lần rà soát ngày 09/11/2026 ở [CODE-REVIEW](CODE-REVIEW.md).
