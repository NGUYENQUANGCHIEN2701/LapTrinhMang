# Tankzors Pro

Tankzors Pro là game xe tăng PvE chạy trên trình duyệt. Phần gameplay campaign
được mô phỏng trực tiếp trong LibGDX client; Spring Boot phục vụ website, xác
thực người dùng, dữ liệu bản đồ, tiến trình, bảng xếp hạng và kết quả trận.

## Công nghệ chính

| Thành phần | Công nghệ |
|---|---|
| Backend và website | Java 21, Spring Boot 3.3.4, Thymeleaf, Spring Security |
| Game client | Java 17, LibGDX 1.12.1, GWT/HTML5, LWJGL3 desktop |
| Dữ liệu | PostgreSQL 16, Spring Data JPA, Flyway |
| Realtime | Spring WebSocket (`/coop`; `/game` là endpoint tương thích cũ) |
| Build và runtime mặc định | Docker Compose, Gradle 8.10.2, Maven 3.9 |

## Kiến trúc hiện tại

Luồng campaign là **client-authoritative**:

```text
Trình duyệt tải HTML/JavaScript LibGDX từ Spring Boot
        ↓
Client gọi GET /api/game/bootstrap để nhận map và runId
        ↓
Client tự chạy gameplay, AI, va chạm và tính điểm
        ↓
Client gọi POST /api/game/results để lưu kết quả và mở khóa màn tiếp theo
```

Spring Boot và client không chạy ở hai cổng riêng. Client HTML5 được build thành
các file tĩnh, đóng gói bên trong `app.jar`, rồi được chính Spring Boot phục vụ
tại cổng `8081`.

## Cấu trúc repository

```text
LapTrinhMang/
├── game-client/        # LibGDX core, HTML5 launcher, desktop launcher và assets
├── server-spring/      # Backend, Thymeleaf, REST API, Security, JPA và WebSocket
├── infra/              # Docker Compose cho ứng dụng và PostgreSQL
├── docs/               # Kiến trúc, protocol, runbook và tài liệu migration
├── client/             # Bundle frontend cũ, không được Docker hiện tại sử dụng
├── server/             # Mã server cũ, không được Docker hiện tại sử dụng
└── .planning/          # Kế hoạch và tài liệu theo dõi tính năng
```

Hai module đang chạy chính thức là `game-client` và `server-spring`. Docker bỏ
qua `client` và `server` khi build.

## Chạy toàn bộ dự án bằng Docker

### Yêu cầu

- Docker Desktop đang chạy.
- Có kết nối Internet trong lần build đầu để tải Docker image và dependency.
- Không cần cài Java, Gradle, Maven hay PostgreSQL trên máy host.

### Khởi động

Mở PowerShell tại thư mục gốc repository:

```powershell
Set-Location D:\LTM\LapTrinhMang
docker compose -f infra\docker-compose.yml up --build -d
```

Lần build đầu có thể mất vài phút. Docker lưu dependency Gradle/Maven trong
BuildKit cache nên những lần sau thường nhanh hơn.

Kiểm tra trạng thái:

```powershell
docker compose -f infra\docker-compose.yml ps
```

Kết quả đúng cần có hai container:

```text
tankzors-app        Up
tankzors-postgres   Up (healthy)
```

Xem log ứng dụng:

```powershell
docker compose -f infra\docker-compose.yml logs -f app
```

Khi thấy `Started TankzorsApplication`, server đã sẵn sàng. Nhấn `Ctrl + C` chỉ
để thoát màn hình log; container vẫn tiếp tục chạy.

### Địa chỉ sử dụng

| Chức năng | Địa chỉ |
|---|---|
| Trang chủ | <http://localhost:8081/> |
| Đăng nhập | <http://localhost:8081/login> |
| Đăng ký | <http://localhost:8081/register> |
| Chọn campaign | <http://localhost:8081/campaigns> |
| Chơi màn 1 | <http://localhost:8081/play?levelId=campaign_1> |
| Bảng xếp hạng | <http://localhost:8081/leaderboard> |
| Co-op lobby | <http://localhost:8081/coop> |
| Quản trị | <http://localhost:8081/admin> |

Tài khoản quản trị mặc định được tạo ở lần khởi động database đầu tiên:

```text
Username: admin
Password: admin123
```

Đây chỉ là tài khoản cho môi trường phát triển. Không dùng mật khẩu mặc định khi
triển khai thật.

Điều khiển trong game:

- `W A S D` hoặc phím mũi tên: di chuyển.
- `Space` hoặc `J`: bắn.
- Click vào khung game nếu bàn phím chưa điều khiển được.

### Dừng và bật lại

Dừng container nhưng giữ dữ liệu PostgreSQL:

```powershell
docker compose -f infra\docker-compose.yml down
```

Bật lại khi không sửa source code:

```powershell
docker compose -f infra\docker-compose.yml up -d
```

Không thêm `-v` vào lệnh `down` trừ khi thực sự muốn xóa toàn bộ dữ liệu game.

## Luồng phát triển hằng ngày

### Sau khi sửa client hoặc server

Mỗi khi sửa source trong `game-client` hoặc `server-spring`, chạy:

```powershell
docker compose -f infra\docker-compose.yml up --build -d
```

| Thay đổi | Docker thực hiện |
|---|---|
| Chỉ sửa `game-client` | Build lại HTML5 client, build lại JAR và thay container app |
| Chỉ sửa `server-spring` | Dùng client đã cache, build lại JAR và thay container app |
| Sửa cả hai | Build lại cả client và server |
| Không sửa source | Dùng cache; chỉ bảo đảm các container đang chạy |

`docker compose restart app` chỉ khởi động lại image cũ, vì vậy **không đủ** sau
khi sửa code. Hãy dùng `up --build -d`.

Sau khi build xong, mở lại trang. Nếu trình duyệt vẫn giữ JavaScript hoặc asset
cũ, nhấn `Ctrl + Shift + R` để tải lại không dùng cache.

### Docker đã build và ghép client như thế nào?

Quá trình trong `server-spring/Dockerfile` có ba stage:

1. `client-builder` dùng `gradle:8.10.2-jdk17` và chạy `html:dist`.
2. Output tại `/home/gradle/project/html/build/dist/` được chép vào
   `/build/src/main/resources/static/game/libgdx/` trong stage build server.
3. `server-builder` dùng Maven + JDK 21 để đóng gói toàn bộ thành `app.jar`.
4. Image runtime chỉ dùng JRE 21 để chạy `/app/app.jar`.

Các đường dẫn trên nằm **bên trong Docker build**, không phải thư mục mới trên
máy Windows. Vì vậy với luồng Docker, bạn không cần chạy `html:copyToServer` và
không cần chép bundle thủ công vào `server-spring`.

## Database

Compose chạy PostgreSQL với cấu hình development:

| Thuộc tính | Giá trị |
|---|---|
| Host từ máy host | `localhost:5432` |
| Host từ container app | `postgres:5432` |
| Database | `tankzors` |
| Username | `tankzors` |
| Password | `tankzors_dev_password` |
| Docker volume | `tankzors_pg_data` |

Flyway tự áp dụng migration trong
`server-spring/src/main/resources/db/migration` khi ứng dụng khởi động.

Xem database bằng `psql` trong container:

```powershell
docker exec -it tankzors-postgres psql -U tankzors -d tankzors
```

Xóa và tạo lại toàn bộ database:

```powershell
docker compose -f infra\docker-compose.yml down -v
docker compose -f infra\docker-compose.yml up --build -d
```

> Cảnh báo: `down -v` xóa volume `tankzors_pg_data` và không thể khôi phục dữ
> liệu từ volume đó.

## Build và chạy thủ công (tùy chọn)

Phần này chỉ dành cho trường hợp muốn phát triển ngoài Docker.

### Client HTML5

Yêu cầu JDK 17. Không dùng JDK 24 vì Gradle/Groovy của dự án có thể báo
`Unsupported class file major version 68`.

```powershell
Set-Location D:\LTM\LapTrinhMang\game-client
.\gradlew.bat clean html:copyToServer
Set-Location ..
```

Task này build client vào `game-client/html/build/dist`, sau đó đồng bộ output
sang:

```text
server-spring/src/main/resources/static/game/libgdx/
```

Sau đó phải build hoặc khởi động lại Spring Boot để server dùng bundle mới.

### Client desktop

```powershell
Set-Location D:\LTM\LapTrinhMang\game-client
.\gradlew.bat desktop:run --args="--level campaign_1"
```

### Spring Boot server

Yêu cầu JDK 21 và Maven 3.9+; PostgreSQL phải đang chạy:

```powershell
mvn -f server-spring\pom.xml clean package
java -jar server-spring\target\tankzors-server-spring-1.0-SNAPSHOT.jar
```

## Kiểm thử

Các lệnh dưới đây dùng toolchain cài trên máy:

```powershell
# Server: JDK 21 + Maven 3.9+
mvn -f server-spring\pom.xml test

# Client: JDK 17
Set-Location game-client
.\gradlew.bat test
Set-Location ..
```

Lệnh Docker `up --build -d` có biên dịch client và server nhưng Dockerfile hiện
dùng `-DskipTests` khi đóng gói server; hãy chạy test riêng trước khi commit.

## Xử lý lỗi thường gặp

### Docker build không tải được dependency

Các lỗi như `Unknown host repo.maven.apache.org`, `Temporary failure in name
resolution` hoặc `Premature end of Content-Length` thường là lỗi mạng/DNS tạm
thời. Kiểm tra Internet và Docker Desktop rồi chạy lại:

```powershell
docker compose -f infra\docker-compose.yml up --build -d
```

Cache Maven/Gradle giúp lần chạy lại không phải tải lại mọi dependency.

### Game hiện màn hình đen

1. Nhấn `Ctrl + Shift + R`.
2. Mở DevTools bằng `F12`, xem tab **Console**.
3. Kiểm tra container và log:

   ```powershell
   docker compose -f infra\docker-compose.yml ps
   docker compose -f infra\docker-compose.yml logs --tail 200 app
   ```

4. Nếu Console báo thiếu asset, build lại bằng `up --build -d`; Docker sẽ tạo
   lại manifest và bundle client đồng bộ.

### Port đã được sử dụng

Compose cần cổng `8081` cho ứng dụng và mặc định dùng `5432` cho PostgreSQL.
Kiểm tra tiến trình/container đang chiếm cổng hoặc đổi cổng host của PostgreSQL:

```powershell
$env:POSTGRES_HOST_PORT = "5433"
docker compose -f infra\docker-compose.yml up --build -d
```

Ứng dụng trong Compose vẫn kết nối tới `postgres:5432`.

### Xem trạng thái nhanh

```powershell
docker compose -f infra\docker-compose.yml ps
docker compose -f infra\docker-compose.yml logs --tail 100 app
docker compose -f infra\docker-compose.yml logs --tail 100 postgres
```

## Tài liệu kỹ thuật

- [Kiến trúc hệ thống](docs/architecture.md)
- [Codebase guide](docs/CODEBASE-GUIDE.md)
- [Networking](docs/networking.md)
- [Protocol](docs/protocol-v1.md)
- [Runbook](docs/runbook.md)

Một số tài liệu trong `docs/` ghi lại các giai đoạn migration trước đây. Khi có
khác biệt về cách chạy, phần Docker trong README này và
`infra/docker-compose.yml` là cấu hình hiện hành.
