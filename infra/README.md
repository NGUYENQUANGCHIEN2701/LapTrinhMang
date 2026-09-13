# infra/ README

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
