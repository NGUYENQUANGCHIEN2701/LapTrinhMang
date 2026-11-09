# Tài liệu Tankzors cho người tiếp quản

Đọc [README gốc](../README.md) để chạy và chơi thử. Sau đó dùng bảng này theo công việc; các tài liệu mô tả tính năng hiện tại, còn mục hướng phát triển là đề xuất.

| Thứ tự | Tài liệu | Mục đích |
|---|---|---|
| 1 | [Kiến trúc](architecture.md) | Nắm ranh giới client, server, database; tránh thêm engine thứ hai |
| 2 | [Trace các luồng](flows/README.md) | Đi từ thao tác người chơi đến class/hàm xử lý |
| 3 | [Hướng dẫn phát triển](DEVELOPMENT.md) | Biết cần sửa ở đâu khi thêm màn, vật phẩm, API hoặc co-op |
| 4 | [Rà soát code](CODE-REVIEW.md) | Xem các sửa lỗi đã triển khai, kết quả kiểm tra và giới hạn còn lại |
| 5 | [Lộ trình trong README](../README.md#12-hướng-phát-triển-và-bàn-giao) | Chọn việc tiếp theo theo ưu tiên và tiêu chí hoàn thành |

## Theo phần việc

| Phần việc | Tài liệu |
|---|---|
| Gameplay, AI, collision, session | [Client](../game-client/README.md), [core](../game-client/core/README.md) |
| GWT, iframe, bootstrap, gọi REST | [HTML5](../game-client/html/README.md), [networking](networking.md) |
| Thử gameplay không cần database | [Desktop](../game-client/desktop/README.md) |
| Map, ảnh, âm thanh | [Assets](../game-client/assets/README.md), [vật phẩm](CLIENT-SHOP-ITEMS-GUIDE.md) |
| MVC, API, persistence, security | [Spring](../server-spring/README.md), [database](../server-spring/src/main/resources/db/README.md) |
| Build, Docker, log, backup | [Infra](../infra/README.md), [runbook](runbook.md) |
| Co-op | [Protocol v2](protocol-v2.md), [kiến trúc](architecture.md), mục co-op trong [DEVELOPMENT](DEVELOPMENT.md) |
| Tra cứu class | [CODEBASE-GUIDE](CODEBASE-GUIDE.md) |

## Cách giữ tài liệu đúng

- Code, build script và migration là bằng chứng cho hành vi thực tế. Khi sửa luồng, cập nhật README module và tài liệu flow tương ứng.
- Các flow có số dòng gắn commit lịch sử; tìm theo tên class/hàm khi số dòng đã thay đổi.
- Không ghi số test đạt nếu chưa chạy ở phiên bản đang bàn giao. Ghi cả kiểm tra bị giới hạn bởi môi trường.
- Không mô tả co-op là đã chơi được chỉ vì relay hoặc session đã có code.
- Nội dung báo cáo/thuyết trình có thể phục vụ mốc nộp bài cũ; dùng README và source khi phát triển tiếp.
