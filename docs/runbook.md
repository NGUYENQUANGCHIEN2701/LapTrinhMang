# Runbook

## Khởi động

1. Mở Docker Desktop và chờ Linux engine chạy.
2. Từ root repository:

```powershell
docker compose -f infra/docker-compose.yml up --build
```

3. Mở http://localhost:8081.

Kiểm tra trạng thái:

```powershell
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs -f app
```

## Dừng

```powershell
docker compose -f infra/docker-compose.yml down
```

Chỉ dùng `down -v` khi thực sự muốn xóa dữ liệu database local.

## Sau khi sửa code

Client hoặc server đều dùng cùng lệnh:

```powershell
docker compose -f infra/docker-compose.yml up --build
```

Nếu chỉ build client ngoài Docker, dùng JDK tương thích với Gradle/GWT (khuyến nghị JDK 17):

```powershell
Set-Location game-client
.\gradlew.bat clean core:test html:copyToServer
```

## Game bị màn hình đen

1. Mở DevTools -> Console.
2. Nếu báo thiếu `assets/...`, chạy lại `html:copyToServer` hoặc Docker `up --build`.
3. Hard reload trình duyệt (`Ctrl+Shift+R`).
4. Network phải tải được `/game/libgdx/index.html`, file GWT và asset với HTTP 200.
5. Bootstrap `/api/game/bootstrap?levelId=campaign_1` phải trả 200.

## Docker build lỗi tải dependency

Lỗi `Temporary failure in name resolution`/`Unknown host repo.maven.apache.org` là DNS/network trong Docker. Chạy lại build khi mạng ổn; cache Maven/Gradle sẽ giữ phần đã tải.

Nếu báo không tìm thấy pipe `dockerDesktopLinuxEngine`, Docker Desktop chưa chạy hoặc Linux engine chưa sẵn sàng.

## Port

- App: `8081`
- PostgreSQL host: `5432` mặc định, có thể đổi bằng `POSTGRES_HOST_PORT`
- WebSocket co-op: `/coop`

Không kiểm tra WebSocket `/game`; endpoint đó đã bị xóa. `/game/libgdx` là static path.
