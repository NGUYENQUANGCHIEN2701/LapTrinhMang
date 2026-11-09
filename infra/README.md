# Docker và PostgreSQL

Chạy các lệnh trong tài liệu này từ root repository. Máy host chỉ cần Docker Desktop/Engine và Compose; Java/Gradle/Maven được cung cấp trong build image.

## Pipeline hiện tại

`server-spring/Dockerfile` có ba giai đoạn:

1. `gradle:8.10.2-jdk17` build `html:dist`.
2. `maven:3.9-eclipse-temurin-21` thay bundle trong Spring resources bằng output vừa build rồi đóng gói JAR.
3. `eclipse-temurin:21-jre-alpine` chạy ứng dụng tại cổng 8081.

Dockerfile bỏ qua test Maven và không chạy core test; chạy test riêng trước khi đóng gói.

## Chạy và kiểm tra

```powershell
docker compose -f infra/docker-compose.yml up --build -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs --tail 200 app
curl.exe http://localhost:8081/api/health
```

`postgres` có healthcheck; `app` chờ database healthy mới khởi động. Endpoint health chỉ xác nhận app phản hồi, chưa chứng minh game load được hay kết quả lưu được. Sau build, mở browser, đăng nhập, chơi một màn và kiểm tra kết quả sau reload.

`restart app` chạy lại image cũ; sửa source cần `up --build -d`. Client HTML5 được build trong Docker nên không cần copy bundle thủ công trước đó.

## Tự build khi lưu file

Compose 2.22 trở lên, giữ terminal này mở:

```powershell
docker compose -f infra/docker-compose.yml up --build --watch
```

Watch rebuild khi source/asset client, source server, POM hoặc Dockerfile đổi theo cấu hình hiện tại. Build output và static bundle generated được bỏ qua. Sửa Compose cần chạy lại lệnh Compose. Khi container app được thay thế, trận đang chơi và phiên đăng nhập/phòng trong RAM có thể bị gián đoạn; dữ liệu PostgreSQL vẫn giữ trong volume.

## Cấu hình và dữ liệu

| Thành phần | Mặc định |
|---|---|
| Website | `http://localhost:8081` |
| PostgreSQL phía host | `localhost:5432`, đổi bằng `POSTGRES_HOST_PORT` |
| PostgreSQL trong mạng Compose | `postgres:5432` |
| Database / user | `tankzors` / `tankzors` |
| Mật khẩu DB dev | `tankzors_dev_password` |
| Mật khẩu seed admin dev | `${ADMIN_PASSWORD:-admin123}` |
| Volume | `tankzors_pg_data` (tên volume cố định) |

```powershell
# PowerShell: đổi cổng host và mật khẩu seed trước khi chạy
$env:POSTGRES_HOST_PORT = "5433"
$env:ADMIN_PASSWORD = "<mat-khau-rieng>"
docker compose -f infra/docker-compose.yml up --build -d
```

App trong Compose luôn nối `postgres:5432`; không đổi `DB_URL` thành cổng host 5433. `DB_URL`, `DB_USER`, `DB_PASSWORD` trong `application.yml` phục vụ cấu hình Spring, còn Compose hiện gán các giá trị DB cố định. Muốn dùng credential khác cần chỉnh cấu hình cả hai service hoặc tạo file override riêng. Đổi `POSTGRES_PASSWORD` không tự đổi mật khẩu role trong volume DB đã khởi tạo; đổi `ADMIN_PASSWORD` cũng không cập nhật admin đã tồn tại.

Để chỉ dùng database khi chạy Spring local:

```powershell
docker compose -f infra/docker-compose.yml up -d postgres
docker exec -it tankzors-postgres psql -U tankzors -d tankzors
```

Nếu đã đổi host port, đặt `DB_URL` tương ứng cho Spring local. Chạy toàn stack rồi chạy thêm Spring local sẽ đụng cổng 8081; dừng service app trước nếu muốn chuyển sang cách chạy đó.

## Dừng, backup và restore

```powershell
docker compose -f infra/docker-compose.yml down
docker compose -f infra/docker-compose.yml up -d
```

`down` giữ dữ liệu. `down -v` xóa volume cùng tài khoản, ví, tiến trình và điểm; chỉ dùng để bỏ database thử. Volume và container có tên cố định nên hai checkout chạy cùng stack trên một máy sẽ xung đột nếu chưa đổi tên/cổng/volume.

Backup định dạng custom bằng file trong container để tránh chuyển binary qua PowerShell:

```powershell
New-Item -ItemType Directory -Force output/backups | Out-Null
docker exec tankzors-postgres pg_dump -U tankzors -d tankzors -Fc -f /tmp/tankzors.dump
docker cp tankzors-postgres:/tmp/tankzors.dump output/backups/tankzors.dump
```

Sao lưu bản dump ra nơi lưu trữ riêng nếu cần giữ lâu; không commit dữ liệu tài khoản. Để kiểm tra backup, khôi phục vào database thử riêng, app vẫn dùng database `tankzors`:

```powershell
# Chỉ tạo một lần; đổi tên nếu database thử này đã tồn tại
docker exec tankzors-postgres createdb -U tankzors tankzors_restore_check
docker cp output/backups/tankzors.dump tankzors-postgres:/tmp/tankzors-restore.dump
docker exec tankzors-postgres pg_restore -U tankzors -d tankzors_restore_check --no-owner --exit-on-error /tmp/tankzors-restore.dump
docker exec tankzors-postgres psql -U tankzors -d tankzors_restore_check -c "SELECT count(*) FROM app_user;"
```

## Phát triển vận hành tiếp

Thêm CI cho core/Spring test và Docker build, cấu hình triển khai riêng với TLS và credential riêng, healthcheck app, backup định kỳ có kiểm tra restore. Giới hạn origin `/coop` theo nơi triển khai thực tế. Xem [runbook](../docs/runbook.md) và [lộ trình](../README.md#12-hướng-phát-triển-và-bàn-giao); đây là việc tương lai, chưa được Compose hiện tại cung cấp.
