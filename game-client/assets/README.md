# Asset nguồn của game

Đây là nơi sửa map, ảnh và âm thanh dùng chung cho desktop/HTML5. Các thư mục gồm `maps`, `map`, `tanks`, `effects`, `gui`, `music`, `sounds` và những asset hỗ trợ hiện có.

Metadata giá và nghiên cứu dùng để kiểm tra giao dịch nằm trong [economy/README.md](economy/README.md); giữ đồng bộ với bản Spring và enum client khi thêm vật phẩm.

## Quy tắc cập nhật

- Đặt tên và đường dẫn nhất quán cả chữ hoa/thường: Docker/Linux phân biệt chữ hoa/thường dù máy Windows có thể vẫn tải được.
- Cập nhật tham chiếu trong `AssetRepository`, renderer hoặc `AudioManager` khi thêm/đổi đường dẫn.
- Âm thanh có các bản MP3/OGG hiện hữu; kiểm tra cách loader chọn định dạng và browser trước khi thay file.
- Không sửa bản copy tại `html/src/main/webapp/assets`, `html/war`, `html/build/dist` hoặc static bundle của Spring. Build đồng bộ/tạo lại các bản này.
- Nếu thêm asset của bên thứ ba, ghi nguồn và điều kiện sử dụng đi kèm.

## Map JSON

Map nguồn ở `maps/campaign_N.json`. Server có bản tương ứng tại `server-spring/src/main/resources/maps` để cấp bootstrap và metadata phần thưởng. Hai bản phải đồng bộ; hiện không có task tự động đồng bộ map sang Spring.

`grid` biểu diễn logic ô, `visualGrid` biểu diễn mã hình ảnh; không thay mã hình ảnh để ngầm đổi luật va chạm. Giá trị tile logic được định nghĩa trong `core/.../gameplay/map/TileType.java`; schema mission nằm trong `MapDefinition` và `MissionConfig`.

Khi sửa: kiểm tra kích thước ma trận, tọa độ spawn, đường đi, mục tiêu nhiệm vụ, wave/allies và rewards; chạy map test hai phía rồi thử desktop/browser. Thêm màn còn phải cập nhật catalog và thứ tự campaign, xem [DEVELOPMENT](../../docs/DEVELOPMENT.md#3-thêm-màn-hoặc-nhiệm-vụ).
