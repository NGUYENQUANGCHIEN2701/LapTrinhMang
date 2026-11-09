# Desktop: thử gameplay local

Desktop dùng LWJGL3 để chạy cùng core với bản HTML5. Đây là công cụ debug gameplay; không xác minh login, cookie, REST hoặc lưu dữ liệu PostgreSQL.

## Chạy

Từ `game-client/` với JDK 17:

```powershell
.\gradlew.bat desktop:run --args="--level campaign_1"
.\gradlew.bat desktop:run --args="--level campaign_2"
.\gradlew.bat desktop:fatJar
```

Fat JAR nằm trong `desktop/build/libs/`, có tên theo version Gradle, hiện là `tankzors-desktop-1.0-SNAPSHOT.jar`:

```powershell
java -jar desktop/build/libs/tankzors-desktop-1.0-SNAPSHOT.jar --level campaign_3
```

`DesktopLauncher` tạo `LocalCampaignSession` với `desktop-player`. `DesktopMapSource` đọc `maps/<levelId>.json` từ assets/classpath, cấp run ID local mới cho mỗi lượt. Callback kết quả chỉ in ra console; ví và inventory không được lưu lên tài khoản server.

## Debug và giới hạn

- Đặt breakpoint trong simulation/session và dùng map nhỏ để kiểm tra tương tác.
- Thêm asset ở `game-client/assets`; Gradle đưa vào resources/classpath khi chạy.
- Thiếu map: kiểm tra ID sau `--level`, tên JSON và nội dung assets.
- Lỗi tạo cửa sổ/OpenGL cần kiểm tra môi trường đồ họa. Chạy JUnit core để kiểm tra logic khi không có GUI.
- Trước khi bàn giao tính năng vẫn cần `core:test`, build GWT và thử browser; desktop không kiểm tra đường lưu kết quả hoặc relay thực tế.
