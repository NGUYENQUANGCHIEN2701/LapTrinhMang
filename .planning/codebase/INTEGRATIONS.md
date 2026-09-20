# External Integrations

**Analysis Date:** 2026-09-20

## APIs and External Services

- Không có payment, email, SMS, OAuth hoặc third-party gameplay API.
- Browser gọi REST cùng origin để bootstrap campaign và submit result.
- Browser dùng WebSocket cùng origin tại `/coop` cho room control và relay packet v2.

## Data Storage

**PostgreSQL 16:**
- Chạy bằng service `db` trong `infra/docker-compose.yml`.
- Spring kết nối qua datasource environment variables của Compose.
- Spring Data JPA truy cập dữ liệu.
- Flyway migrations nằm trong `server-spring/src/main/resources/db/migration`.

**Static files:**
- HTML5 bundle được build từ `game-client/html`.
- Docker copy bundle vào Spring static resources và phục vụ tại `/game/libgdx/**`.
- Không có object storage hoặc CDN ngoài dự án.

**Caching:**
- Không có Redis hoặc distributed cache.

## Authentication and Identity

- Spring Security quản lý đăng ký, đăng nhập, đăng xuất và HTTP session.
- WebSocket `/coop` dùng identity từ session phía server; client không tự khai báo quyền host đáng tin cậy.
- Không có OAuth provider bên ngoài.

## Monitoring and Observability

- Spring Actuator health endpoint tại `/api/health`.
- Application và container logs đi ra stdout/stderr.
- Không có Sentry, analytics hoặc log SaaS.

## CI/CD and Deployment

- Docker Compose là môi trường chạy chuẩn trong repo.
- `server-spring/Dockerfile` build LibGDX HTML5 và Spring JAR theo multi-stage.
- Repository hiện không phụ thuộc một hosted CI/CD service để chạy local.

## Environment Configuration

**Development:**
- Compose cung cấp database host, port, database name và credentials cho Spring.
- Secrets phát triển phải đi qua environment/config ngoài source; không ghi giá trị bí mật vào tài liệu OpenGSD.

**Production/Staging:**
- Chưa có cấu hình managed hosting riêng trong repository.
- Khi triển khai thật phải thay credentials mặc định và cấu hình session/database phù hợp môi trường.

## Webhooks and Callbacks

- Không có webhook bên thứ ba.
- Internal browser-to-server flows gồm campaign REST endpoints và `/coop` WebSocket.

---
*Integration audit: 2026-09-20*  
*Update when adding or removing external services*
