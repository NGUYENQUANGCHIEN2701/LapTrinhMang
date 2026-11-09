# Catalog kinh tế

`game-client/assets/economy/catalog.json` là metadata giá, pack, giới hạn và nghiên cứu mà Spring dùng để kiểm tra giao dịch. Đây là dữ liệu kinh tế tài khoản, không phải simulation.

- Giữ metadata khớp `ShopItem` và `ResearchTech`; `EconomyJournalTest` kiểm tra parity.
- Đồng bộ sang `server-spring/src/main/resources/economy/catalog.json` cho server chạy local. Dockerfile luôn chép bản nguồn client khi đóng gói server.
- Nếu đổi công thức giá/giới hạn động, sửa cả dự đoán client và `EconomyCatalog` server, kèm test tương tác.
- Không tái sử dụng item/tech ID đã lưu với ý nghĩa khác nếu chưa có migration dữ liệu.

Luồng giao dịch ở [networking](../../../docs/networking.md); cách thêm vật phẩm ở [DEVELOPMENT](../../../docs/DEVELOPMENT.md#4-thêm-vật-phẩm-hoặc-nghiên-cứu).
