# 08. Build và khởi động hệ thống

**Cập nhật:** task `compileGwt` chờ `syncAssets`; Docker stage server chép catalog kinh tế từ `game-client/assets/economy` vào resources trước đóng gói. Flyway hiện có V1–V8. Dockerfile vẫn không chạy unit test, nên phải chạy core/Spring test riêng. Xem kết quả xác minh thực tế trong [báo cáo sửa lỗi](../CODE-REVIEW.md).

**Tóm tắt:** Một lệnh Docker Compose dựng toàn bộ hệ thống. Dockerfile có 3 giai đoạn: biên dịch game Java sang JavaScript bằng JDK 17, chép kết quả vào server rồi đóng gói JAR bằng Java 21, cuối cùng chạy trên image JRE nhẹ. Khi khởi động, Flyway tự cập nhật database.

**File chính:** [server-spring/Dockerfile](../../server-spring/Dockerfile), [infra/docker-compose.yml](../../infra/docker-compose.yml), [application.yml](../../server-spring/src/main/resources/application.yml), [db/migration/](../../server-spring/src/main/resources/db/migration/)

## Sơ đồ

```text
docker compose -f infra/docker-compose.yml up --build
  │
  ├─ Giai đoạn 1 (gradle:8.10.2-jdk17)
  │    game-client/ → gradle html:dist → html/build/dist/ (JavaScript)
  │
  ├─ Giai đoạn 2 (maven:3.9-eclipse-temurin-21)
  │    xoá static/game/libgdx cũ
  │    chép dist vào src/main/resources/static/game/libgdx/
  │    mvn package → app.jar
  │
  ├─ Giai đoạn 3 (eclipse-temurin:21-jre-alpine)
  │    chỉ giữ app.jar
  │
  └─ Khởi động
       postgres chạy, healthcheck đạt
       └─ app chạy: Flyway áp migration → Hibernate kiểm tra schema
            → DataInitializer tạo admin → phục vụ cổng 8081
```

## Bước 1: Biên dịch game

GWT chạy trên JDK 17, trong khi server dùng Java 21. Tách giai đoạn để máy chủ không cần cài phiên bản Java nào:

[Dockerfile:14-23](../../server-spring/Dockerfile#L14-L23)
```dockerfile
FROM gradle:8.10.2-jdk17 AS client-builder

WORKDIR /home/gradle/project

# Copy riêng game-client để cache Gradle độc lập với backend.
COPY --chown=gradle:gradle game-client/ ./

# Cache dependency Gradle qua các lần build, kể cả khi mạng bị gián đoạn.
RUN --mount=type=cache,id=tankzors-gradle,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle --no-daemon --console=plain html:dist
```

Task `dist` khai báo trong [html/build.gradle](../../game-client/html/build.gradle#L54).

## Bước 2: Đưa game vào server

Bundle cũ luôn bị xoá trước, nên JavaScript trong image khớp với mã Java vừa biên dịch:

[Dockerfile:41-47](../../server-spring/Dockerfile#L41-L47)
```dockerfile
RUN rm -rf ./src/main/resources/static/game/libgdx
COPY --from=client-builder /home/gradle/project/html/build/dist/ \
    ./src/main/resources/static/game/libgdx/

# Build JAR (test chạy riêng; Docker image production bỏ test để build nhanh).
RUN --mount=type=cache,id=tankzors-maven,target=/root/.m2 \
    mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 -DskipTests package
```

Vì vậy game và website cùng nằm trong `app.jar`, cùng cổng, cùng nguồn gốc. Đó là lý do cookie phiên tự đi kèm lời gọi API ([01](01-dang-nhap-va-dinh-danh.md)) và không cần cấu hình CORS.

**Lưu ý:** không sửa tay file trong `static/game/libgdx`, đó là file sinh ra và bị xoá ở mỗi lần build. Bước build dùng `-DskipTests`, nên test phải chạy riêng.

## Bước 3: Khởi động

`app` chỉ chạy sau khi database sẵn sàng:

[docker-compose.yml:41-43](../../infra/docker-compose.yml#L41-L43)
```yaml
depends_on:
  postgres:
    condition: service_healthy
```

Khi Spring Boot khởi động, Flyway chạy các migration chưa áp dụng, rồi Hibernate chỉ kiểm tra entity khớp schema, không tự sửa bảng:

[application.yml:27-38](../../server-spring/src/main/resources/application.yml#L27-L38)
```yaml
jpa:
  hibernate:
    ddl-auto: validate   # Flyway quản lý schema
  open-in-view: false
  properties:
    hibernate:
      dialect: org.hibernate.dialect.PostgreSQLDialect
      format_sql: false
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
```

Dữ liệu nằm trong volume `tankzors_pg_data`, nên còn nguyên khi tạo lại container.

## Lịch sử database

| Migration | Thay đổi |
|---|---|
| [V1__init](../../server-spring/src/main/resources/db/migration/V1__init.sql) | Bảng `leaderboard`, `match_result` |
| [V2__player_progress](../../server-spring/src/main/resources/db/migration/V2__player_progress.sql) | Tiến trình mở màn |
| [V3__match_result_dedup](../../server-spring/src/main/resources/db/migration/V3__match_result_dedup.sql) | `run_id` UNIQUE, `payload_hash`, chống ghi trùng |
| [V4__player_totals](../../server-spring/src/main/resources/db/migration/V4__player_totals.sql) | Tổng Sao/Xu |
| [V5__app_users](../../server-spring/src/main/resources/db/migration/V5__app_users.sql) | Tài khoản |
| [V6__player_shop_state](../../server-spring/src/main/resources/db/migration/V6__player_shop_state.sql) | Túi đồ, cây nghiên cứu |
| [V7__player_progress_completed](../../server-spring/src/main/resources/db/migration/V7__player_progress_completed.sql) | Cột `completed`, mỗi màn chơi một lần |

Muốn đổi schema: thêm file `V8__...sql` mới, không sửa file cũ đã chạy, và cập nhật entity tương ứng. Nếu đổi cấu trúc JSON bản đồ thì sửa cả [game-client/assets/maps/](../../game-client/assets/maps/) và [server-spring/.../maps/](../../server-spring/src/main/resources/maps/).

## Các lệnh hay dùng

```bash
# Dựng và chạy nền
docker compose -f infra/docker-compose.yml up --build -d

# Tự build lại khi sửa code
docker compose -f infra/docker-compose.yml up --build --watch

# Xem log ứng dụng
docker compose -f infra/docker-compose.yml logs -f app

# Chạy test server (cần JDK 21) và client (cần JDK 17)
mvn -f server-spring/pom.xml test
cd game-client && ./gradlew :core:test
```

Mở ứng dụng tại `http://localhost:8081`.

Chế độ `--watch` được khai báo ở [docker-compose.yml:52-68](../../infra/docker-compose.yml#L52-L68): lưu mã nguồn thì build lại image và tạo lại container, trận đang chơi sẽ bị gián đoạn.
