# shared-protocol Module - README

Module Maven độc lập chứa DTO/enum/constant của wire protocol
(Phiên bản v1, tham khảo `docs/protocol-v1.md`).

Mục đích:
- Chia sẻ wire format giữa `server-spring` và `game-client/core` mà
  không kéo dependency Spring/Javalin/JPA/LibGDX vào module này.
- Đóng băng tên packet, tên field, giá trị enum để migration Phase 2/3
  không vô tình đổi wire format.

## Dependency rule

```
shared-protocol <- server-spring
shared-protocol <- game-client/core
```

Engine/domain packages KHÔNG phụ thuộc module này (vì engine không serialise
ra wire). Module này chỉ chứa plain POJO + enum + constant.

## Build

```bash
# Từ repository root (đảm bảo Maven được cài đặt — xem `mvnw` ở root)
mvn -f shared-protocol/pom.xml clean install
```

Kết quả: file jar `shared-protocol/target/shared-protocol-1.0-SNAPSHOT.jar`
sẵn sàng để import vào server-spring và (viva proguard) LibGDX core.

## Quy tắc thay đổi

- KHÔNG xóa field/enum đã chốt trong `docs/protocol-v1.md`.
- Khi cần đổi wire format -> tạo `protocol-v2.md` và giữ v1 hoạt động song
  song cho tới cutover (Phase 5).
- Mọi class trong module phải không có dependency ngoài JDK + Gson
  (server và client đều dùng được).
