# Core: gameplay, session và render

Core là nguồn sự thật gameplay của Tankzors, được dùng bởi desktop và HTML5. Module chỉ phụ thuộc LibGDX core cho runtime; backend và transport cụ thể thuộc các module launcher.

## Bản đồ trách nhiệm

Các package dưới `src/main/java/com/tankzors/client/`:

| Package/class | Trách nhiệm |
|---|---|
| `GameClient` | Vòng đời ứng dụng, input và render qua một `GameSession` |
| `gameplay/GameSimulation`, `GameWorld` | Fixed-step, world, hành vi gameplay, thắng/thua |
| `gameplay/entity`, `ai`, `collision` | Xe, đạn, công trình, AI và tương tác |
| `gameplay/map`, `MissionSpawner` | Định nghĩa map, nhiệm vụ và spawn |
| `session/LocalCampaignSession` | Bootstrap, retry/next, shop, kết quả campaign |
| `session/HostGameSession`, `GuestGameSession` | Host mô phỏng; guest nhận snapshot, nền tảng co-op |
| `input` | Chuyển phím/chuột thành lệnh session |
| `model`, `WorldSnapshotMapper` | Dữ liệu snapshot cung cấp cho renderer |
| `render`, `ui`, `audio` | Hình ảnh, overlay và âm thanh |
| `shop` | Catalog, nghiên cứu, ví và inventory phía client |
| `network` | Interface socket và tạo packet; triển khai socket ở HTML/desktop |

Luồng mỗi frame: input → session → `GameSimulation.advance` → snapshot → renderer. `GameplayConfig` cấu hình 30 Hz, kẹp delta mỗi frame và giới hạn số bước chạy bù. Renderer không sửa world; không đặt logic sát thương hoặc AI trong Spring.

## Điểm mở rộng

- `GameSession`: tách chế độ local, host và guest khỏi phần vẽ.
- `MapSource`: nguồn map bất đồng bộ trên browser, đồng bộ trên desktop; test dùng nguồn giả.
- `CampaignPersistence`: core phát journal thao tác và kết quả; browser thực hiện I/O bất đồng bộ, callback xác nhận/lỗi. Session chờ xác nhận trước khi chuyển màn; desktop giữ đường chạy local.
- Snapshot: thêm trường ở model và mapper khi renderer cần dữ liệu mới; co-op còn phải bổ sung serialization và parser tương ứng.

## Test

Từ `game-client/`:

```powershell
.\gradlew.bat core:test
.\gradlew.bat core:test --tests "*LocalCampaignSessionTest"
```

JUnit nằm trong `src/test/java`; report ở `build/reports/tests/test/index.html`. Test gameplay nên tạo map/world nhỏ, seed cố định và kiểm tra tương tác quan sát được: sát thương, va chạm, mạng, vật phẩm hoặc thắng/thua. Test session cần kiểm tra callback về chậm, lỗi bootstrap, retry và callback cũ.

Không đưa JDK API chỉ có trên desktop vào core dù JVM test đạt; chạy `html:dist` để kiểm tra transpilation. Cách thêm màn/vật phẩm và các sửa lỗi và giới hạn nằm trong [DEVELOPMENT](../../docs/DEVELOPMENT.md) và [CODE-REVIEW](../../docs/CODE-REVIEW.md).
