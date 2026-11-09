# Tankzors Spring server

Module này không chứa gameplay engine. Nó chịu trách nhiệm:

- website Thymeleaf và Spring Security;
- `GET /api/game/bootstrap` để cấp map/run cho client;
- `POST /api/game/results` để lưu kết quả idempotent;
- progress, leaderboard và admin;
- Flyway/JPA/PostgreSQL;
- WebSocket `/coop` làm relay, không chạy simulation.

Bundle HTML5 được phục vụ tại `/game/libgdx/**`. Đây là static path, không phải WebSocket gameplay.

## Build

Từ root repository:

```powershell
docker compose -f infra/docker-compose.yml up --build
```

Hoặc nếu máy đã có JDK 21 và Maven:

```powershell
mvn -f server-spring/pom.xml test
mvn -f server-spring/pom.xml spring-boot:run
```

Database schema nằm trong `src/main/resources/db/migration`. Không sửa migration đã áp dụng; hãy tạo migration phiên bản tiếp theo.

Các lệnh Maven ở trên chạy từ root repository. Chạy server local cần PostgreSQL và bundle HTML5 đã build bằng `html:copyToServer` từ thư mục client. Maven không tự build GWT; test H2 cũng không xác nhận game đã được đóng gói. Xem [client README](../game-client/README.md).

## Bản đồ source

Các package dưới `src/main/java/com/tankzors/`:

| Package | Trách nhiệm |
|---|---|
| `web` | MVC, REST, model trang, settings, bootstrap/result/shop endpoints |
| `application` | Campaign query, progress, shop, catalog kinh tế, khóa account và leaderboard use cases |
| `result` | Cấp lượt, validate owner/metadata, lưu kết quả, hash và retry/conflict |
| `progress`, `leaderboard` | Entity và repository JPA |
| `security`, `identity` | Auth, BCrypt, quyền admin, định danh từ phiên HTTP |
| `map` | DTO/loader metadata map; không có gameplay entity |
| `network`, `room` | WebSocket relay, membership và metadata phòng trong RAM |

Template ở `src/main/resources/templates`, CSS/JS web ở `static`, map ở `maps`, schema ở `db/migration`. Map tương ứng còn tồn tại trong `game-client/assets/maps`; cần đồng bộ khi sửa dữ liệu.

## Các hợp đồng quan trọng

- `/api/game/bootstrap` kiểm tra màn, lưu lượt được cấp với owner/level/version/hạn dùng và trả map, shop state cùng revision.
- `/api/game/results` kiểm tra issued run/owner/level/version; khóa row tài khoản đến commit; lưu kết quả, cộng thưởng, đổi progress và best score trong cùng transaction. Retry trả metadata hiện tại; payload khác trả `409`. Hiện chỉ nhận campaign.
- `ResultService` tính thưởng từ map và nghiên cứu đã xác nhận, rồi cộng ví một lần. Client không quyết định phần thưởng thực ghi.
- `/api/game/shop/sync` nhận journal thao tác có request ID/expected revision; server kiểm tra catalog, giá, kho, nghiên cứu và tiền. Không nhận snapshot ví/kho kiểu cũ. `/api/game/account` cấp định danh và revision để browser khôi phục outbox.
- `/api/progress/reset` cần CSRF token và chỉ reset progression; không xóa toàn bộ ví/lịch sử.
- `/coop` dùng protocol v2, server gắn sender và relay theo quyền host/member. `MATCH_RESULT` relay chưa tự ghi persistence. Phòng và socket mất khi app restart.

Xem [networking](../docs/networking.md), [protocol](../docs/protocol-v2.md) và [báo cáo sửa lỗi](../docs/CODE-REVIEW.md).

## Cấu hình local

`application.yml` mặc định cổng `8081`, PostgreSQL `localhost:5432/tankzors`, user `tankzors`. Có thể đặt `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_DRIVER`, `SERVER_PORT` trước khi chạy Maven/JAR. Đây là cấu hình server; thay biến host không tự thay credential của Postgres trong Compose.

`ADMIN_PASSWORD` chỉ dùng tạo tài khoản admin nếu chưa tồn tại. Chạy local không đặt biến này sẽ bỏ qua seed admin. Thay giá trị biến không cập nhật mật khẩu admin đã lưu.

## Test và migration

```powershell
# Chạy từ root, JDK 21
mvn -f server-spring/pom.xml test
mvn -f server-spring/pom.xml '-Dtest=ResultServiceTest' test
```

Test report nằm ở `target/surefire-reports`. Phần lớn test bật profile `test`: datasource H2 PostgreSQL mode, Flyway vẫn chạy, JPA `validate`. `TestSecurityConfig` permit-all và tắt CSRF, vì vậy test controller dùng profile này không chứng minh phân quyền production đúng. Khi sửa auth, dùng cấu hình thật như `ApiAuthenticationTest`/`LoginFailureMessageTest`.

Khi đổi schema, thêm migration mới và test H2, sau đó kiểm tra PostgreSQL/Docker. H2 không đảm bảo mọi SQL, lock và transaction có cùng hành vi với PostgreSQL. Chi tiết bảng và migration ở [db/README.md](src/main/resources/db/README.md).

## Phát triển tiếp

Nền tảng lượt/giao dịch/xác nhận lưu đã có. Tiếp theo cần CI Docker/PostgreSQL, migration upgrade và hoàn thiện launcher co-op; xem rõ giới hạn xác minh trong báo cáo sửa lỗi. API mới cần DTO riêng, validation theo cột DB, transaction đúng use case và test lỗi/quyền/retry. Giữ gameplay trong client. Các bước cụ thể ở [DEVELOPMENT](../docs/DEVELOPMENT.md) và [lộ trình](../README.md#12-hướng-phát-triển-và-bàn-giao).
