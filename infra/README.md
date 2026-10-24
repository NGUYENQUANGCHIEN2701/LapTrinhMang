# infra/ README

## Tự cập nhật khi sửa code

Chạy từ thư mục gốc dự án bằng Docker Compose 2.22 trở lên:

```powershell
docker compose -f infra/docker-compose.yml up --build --watch
```

Giữ terminal này mở. Khi lưu source hoặc asset của client/server, Compose tự
build lại image và tạo lại container ứng dụng. Java/GWT cần thời gian biên dịch;
khi ứng dụng chạy lại, tải lại trang trình duyệt để dùng bản mới. Trận đang chơi
sẽ bị gián đoạn khi container được thay thế. Dữ liệu PostgreSQL nằm trong volume
riêng và được giữ lại.

Lệnh `up -d --build` chỉ build và khởi chạy một lần, không bật Watch.

Skeleton hạ tầng Docker Compose cho desenvolvimento.

## PostgreSQL (Phase 1 skeleton, Phase 4 sử dụng)

```bash
# Start & detach
docker compose -f infra/docker-compose.yml up -d

# Xem log
docker compose -f infra/docker-compose.yml logs -f postgres

# Kết nối thử (cần psql client hoặc DBeaver)
docker exec -it tankzors-postgres psql -U tankzors -d tankzors

# Dọn dẹp (giữ volume)
docker compose -f infra/docker-compose.yml down

# Dọn dẹp sạch (xóa data)
docker compose -f infra/docker-compose.yml down -v
```

Connection params (mặc định):

| Tham số | Giá trị |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `tankzors` |
| Username | `tankzors` |
| Password | `tankzors_dev_password` |

> **Security note**: mật khẩu trên chỉ dành cho development. Production
> sẽ inject qua environment variable hoặc secret manager, không commit.

## Tương lai

- Phase 4: thêm service `pgadmin` để duyệt schema qua UI.
- Phase 5 (optional): thêm `server` image khi đã Dockerfile Spring Boot.
