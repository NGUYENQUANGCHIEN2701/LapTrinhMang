# Thymeleaf UI Implementation Plan

## 1. Mục tiêu

Implement website UI bằng Spring Boot + Thymeleaf, thay thế phần menu/page của React nhưng chưa xóa React client và chưa tích hợp LibGDX HTML5.

Phase này chỉ tạo website shell và `play` placeholder.

```text
Website UI:
  Spring Boot + Thymeleaf + HTML + CSS

Gameplay:
  Chưa migrate trong phase này
  /play chỉ có placeholder cho LibGDX HTML5 sau này

Không làm:
  React UI mới
  Shop/research
  Save system nâng cao
  LibGDX HTML5
  Gameplay logic mới
```

## 2. Phạm vi chức năng

### Bắt buộc

- [ ] Main Menu
- [ ] Game Menu
- [ ] Campaign Selection
- [ ] Load Game placeholder
- [ ] Settings page
- [ ] Leaderboard page đọc dữ liệu hiện có
- [ ] Play page có level parameter và game placeholder
- [ ] Navigation giữa các page
- [ ] CSS layout dùng chung
- [ ] Error page cơ bản

### Để sau

- [ ] Nhúng LibGDX HTML5
- [ ] Mission Briefing trong gameplay
- [ ] Pause menu trong gameplay
- [ ] Shop
- [ ] Research tree
- [ ] Save slot thật
- [ ] Online Levels UI đầy đủ
- [ ] Practice UI đầy đủ
- [ ] Authentication
- [ ] React removal hoàn toàn

## 3. Cấu trúc thư mục đích

```text
server-spring/
└── src/
    └── main/
        ├── java/com/tankzors/
        │   └── web/
        │       ├── PageController.java
        │       ├── PageModelFactory.java       # optional
        │       └── WebExceptionHandler.java    # optional
        │
        └── resources/
            ├── templates/
            │   ├── fragments/
            │   │   ├── head.html
            │   │   ├── navbar.html
            │   │   ├── flash-message.html
            │   │   └── footer.html
            │   ├── error/
            │   │   └── 404.html
            │   ├── layout.html
            │   ├── menu.html
            │   ├── game-menu.html
            │   ├── campaigns.html
            │   ├── load-game.html
            │   ├── settings.html
            │   ├── leaderboard.html
            │   └── play.html
            │
            └── static/
                ├── css/
                │   ├── site.css
                │   ├── menu.css
                │   ├── game-menu.css
                │   ├── campaigns.css
                │   ├── settings.css
                │   ├── leaderboard.css
                │   └── play.css
                ├── images/
                └── game/
                    └── libgdx/       # empty placeholder for future HTML5 output
```

Không đặt game entity, GameLoop hoặc WebSocket state vào Thymeleaf model mỗi frame.

## 4. Dependency setup

### File

```text
server-spring/pom.xml
```

### Dependency cần có

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Giữ nguyên các dependency hiện có của WebSocket, JPA, PostgreSQL, Gson và test.

### Cấu hình đề xuất

```yaml
spring:
  thymeleaf:
    cache: false
    prefix: classpath:/templates/
    suffix: .html
```

`cache: false` chỉ dùng trong profile development. Production có thể bật cache.

## 5. Route contract

| Method | Route | Template | Mục đích |
|---|---|---|---|
| GET | `/` | `menu` | Main menu |
| GET | `/menu` | `menu` | Alias main menu |
| GET | `/game-menu` | `game-menu` | Chọn chế độ chơi |
| GET | `/campaigns` | `campaigns` | Chọn campaign/level |
| GET | `/load-game` | `load-game` | Hiển thị save placeholder |
| GET | `/settings` | `settings` | Hiển thị cài đặt |
| POST | `/settings` | redirect | Lưu settings tối thiểu |
| GET | `/leaderboard` | `leaderboard` | Hiển thị leaderboard |
| GET | `/play` | `play` | Trang mount game |
| GET | `/play?levelId=campaign_1` | `play` | Chọn level cho game |
| GET | `/error/404` | `error/404` | Trang not found |

Không dùng các route React cũ làm route chính sau khi Thymeleaf UI hoạt động.

## 6. PageController

### Trách nhiệm

`PageController` chỉ:

- Nhận HTTP request.
- Đọc query/form parameter.
- Gọi application service khi cần.
- Đưa dữ liệu đơn giản vào `Model`.
- Trả về template hoặc redirect.

`PageController` không được:

- Điều khiển GameLoop.
- Xử lý collision/AI/bullet.
- Gửi WebSocket snapshot.
- Truy cập trực tiếp JPA repository nếu đã có service.

### Model attributes chuẩn

```text
pageTitle
activePage
levels
selectedLevelId
settings
leaderboardEntries
errorMessage
```

## 7. Chi tiết từng page

### 7.1 Main Menu - `menu.html`

Route:

```text
GET /
GET /menu
```

Hiển thị:

- Tên game Tankzors.
- Nút `Game`.
- Nút `Settings`.
- Nút `Leaderboard`.
- Nút `About` placeholder hoặc ẩn nếu chưa có nội dung.

Navigation:

```text
Game          -> /game-menu
Settings      -> /settings
Leaderboard   -> /leaderboard
```

Không implement `Exit` vì browser không cho website tự đóng tab ổn định. Có thể dùng nút `Back to Home` thay thế.

### 7.2 Game Menu - `game-menu.html`

Route:

```text
GET /game-menu
```

Hiển thị:

- Continue.
- Campaigns.
- Online Levels placeholder.
- Load Game.
- Practice placeholder.
- Back.

Trong phase này:

- `Campaigns` phải hoạt động.
- `Load Game` mở page placeholder.
- `Online Levels` và `Practice` có thể disabled với label `Coming soon`.

### 7.3 Campaign Selection - `campaigns.html`

Route:

```text
GET /campaigns
```

Hiển thị danh sách map đã tồn tại:

```text
campaign_1
campaign_2
campaign_3
```

Mỗi card cần có:

- Level id.
- Tên level.
- Difficulty nếu có metadata.
- Nút `Play`.

Nút Play dùng link:

```text
/play?levelId=campaign_1
```

Không load trực tiếp `GameState` vào page. Chỉ load metadata level/map.

### 7.4 Load Game - `load-game.html`

Route:

```text
GET /load-game
```

Trong phase này chỉ cần UI placeholder:

- Hiển thị `No save system configured` nếu chưa có save service.
- Có 3 slot disabled hoặc trạng thái empty.
- Nút Back.

Không giả lập save bằng cách ghi bừa vào database.

### 7.5 Settings - `settings.html`

Route:

```text
GET /settings
POST /settings
```

Field tối thiểu:

```text
musicVolume
soundVolume
language
showFps
gameScale
```

Giai đoạn đầu có thể lưu bằng session hoặc cookie. Không cần tạo bảng database settings.

POST phải:

1. Validate input.
2. Clamp volume/scale về range hợp lệ.
3. Redirect về `/settings`.
4. Hiển thị flash message thành công/lỗi.

### 7.6 Leaderboard - `leaderboard.html`

Route:

```text
GET /leaderboard
GET /leaderboard?mapId=campaign_1
```

Dùng `LeaderboardService`, không gọi repository trực tiếp từ template/controller nếu service đã có.

Hiển thị:

- Rank.
- Player name.
- Map.
- Score.
- Time.
- HP remaining.
- Created time nếu có.

Có thể thêm filter map bằng query parameter `mapId`.

Nếu database chưa chạy:

- Hiển thị error message thân thiện.
- Không làm crash toàn bộ website.

### 7.7 Play - `play.html`

Route:

```text
GET /play
GET /play?levelId=campaign_1
```

Hiển thị:

- `selectedLevelId`.
- Vùng `#game-container`.
- Placeholder `LibGDX HTML5 will be mounted here`.
- Nút Back về `/campaigns`.

Template phải để sẵn:

```html
<div id="game-container"
     th:attr="data-level-id=${selectedLevelId}">
</div>
```

Không nhúng React bundle.

Không implement gameplay bằng Thymeleaf. LibGDX HTML5 sẽ được nhúng sau.

## 8. Template fragments

### `layout.html`

Layout chung cần có:

- HTML5 doctype.
- Meta charset.
- Viewport.
- Title động.
- Link `site.css`.
- Main content fragment.
- Footer.

### `fragments/navbar.html`

Navbar tối giản:

```text
Home | Game | Campaigns | Leaderboard | Settings
```

Highlight page hiện tại bằng `activePage`.

### `fragments/flash-message.html`

Hiển thị:

- Success message.
- Error message.
- Warning message.

Không in raw exception stack trace ra UI.

## 9. CSS rules

### `site.css`

Định nghĩa:

- Color variables.
- Font stack.
- Body/background.
- Container.
- Button base.
- Button variants.
- Card.
- Form controls.
- Responsive breakpoint.

### UI style direction

- Dark game UI.
- Accent xanh lá/vàng.
- Button rõ ràng, dễ click.
- Layout responsive cho màn hình nhỏ.
- Không phụ thuộc asset gameplay.

Không dùng inline style trừ data attribute hoặc giá trị động thật sự cần thiết.

## 10. Service integration

### Campaign metadata

Nếu chưa có service, tạo abstraction tối thiểu:

```java
public interface CampaignQueryService {
    List<CampaignLevelView> getAvailableLevels();
}
```

Service được phép đọc map metadata, nhưng không khởi động game loop.

### Leaderboard

Dùng flow:

```text
LeaderboardController/PageController
        ↓
LeaderboardService
        ↓
LeaderboardRepository
        ↓
PostgreSQL/H2 test
```

Nếu `LeaderboardController` REST hiện tại đã tồn tại, không phá REST API. Có thể tạo `PageController` riêng cho HTML page.

## 11. Testing plan

### Unit test

- [ ] Campaign metadata service trả đúng 3 level.
- [ ] Settings validation clamp đúng range.
- [ ] Query parameter level mặc định là `campaign_1`.
- [ ] Unknown level xử lý bằng error/redirect.

### MVC test

Sử dụng `@WebMvcTest` hoặc `MockMvc`:

- [ ] `GET /` trả HTTP 200 và view `menu`.
- [ ] `GET /game-menu` trả view đúng.
- [ ] `GET /campaigns` có 3 level.
- [ ] `GET /play?levelId=campaign_2` trả selected level đúng.
- [ ] `GET /settings` trả form.
- [ ] `POST /settings` redirect đúng.
- [ ] `GET /leaderboard` render entries.
- [ ] Unknown route trả error page.

### Manual browser test

```text
1. Mở /
2. Bấm Game
3. Bấm Campaigns
4. Chọn campaign_1
5. Xác nhận /play?levelId=campaign_1 mở được
6. Quay lại menu
7. Mở Settings và submit form
8. Mở Leaderboard
9. Refresh từng page
```

## 12. Build và chạy

### Development

```cmd
cd /d D:\BTLLapTrinhMang\LapTrinhMang
java -jar server-spring\target\tankzors-server-spring-1.0-SNAPSHOT.jar
```

Mở:

```text
http://localhost:8081/
```

### Khi cần build lại

```cmd
mvn -f server-spring\pom.xml clean package
```

Nếu máy không có Maven trong PATH, dùng Maven Wrapper phù hợp với shell hoặc chạy Maven từ IDE.

### PostgreSQL

```cmd
docker compose -f infra\docker-compose.yml up -d
```

PostgreSQL chỉ bắt buộc khi test leaderboard persistence. Menu/play placeholder có thể test độc lập.

## 13. Không được làm

- Không xóa `client/` React trong phase này.
- Không xóa `server/` Javalin trong phase này.
- Không nhúng JavaScript gameplay mới.
- Không đưa GameLoop vào controller.
- Không gọi WebSocket từ Thymeleaf mỗi frame.
- Không gọi repository trực tiếp từ HTML template.
- Không tạo save/shop/research giả để đánh dấu hoàn thành.
- Không đổi endpoint `/game`.
- Không đổi protocol realtime.
- Không sửa collision/AI/movement.

## 14. Definition of Done

- [ ] Thymeleaf dependency đã thêm.
- [ ] `/` mở được Main Menu.
- [ ] Có navigation tới Game Menu, Campaigns, Settings, Leaderboard.
- [ ] Campaign page hiển thị 3 level hiện có.
- [ ] `/play` nhận và hiển thị `levelId`.
- [ ] Play page có placeholder cho LibGDX.
- [ ] Leaderboard page đọc được service hiện tại.
- [ ] Settings page submit được.
- [ ] CSS dùng chung, không lạm dụng inline style.
- [ ] MVC tests pass.
- [ ] Server tests hiện tại vẫn pass.
- [ ] React/Javalin legacy chưa bị xóa.
- [ ] Không có gameplay mới bị trộn vào.

## 15. Prompt giao cho AI implement

```text
Đọc trước các file:
- .planning/PROJECT.md
- .planning/ROADMAP.md
- .planning/REFACTOR-PLAN.md
- .planning/AI-IMPLEMENTATION-GUIDE.md
- .planning/THYMELEAF-UI-IMPLEMENTATION.md
- server-spring/README.md
- docs/protocol-v1.md

Nhiệm vụ: implement Thymeleaf website shell trên server-spring.

Phạm vi:
- Main Menu
- Game Menu
- Campaign Selection
- Load Game placeholder
- Settings
- Leaderboard
- Play placeholder
- Shared CSS/layout/fragments

Không làm:
- Không migrate LibGDX HTML5
- Không xóa React client
- Không xóa Javalin server
- Không thêm gameplay mới
- Không sửa GameLoop/AI/collision/movement

Trước khi code:
1. Kiểm tra server-spring hiện có controller/service nào.
2. Liệt kê file sẽ tạo/sửa.
3. Xác định các route không được phá.

Sau khi code:
1. Chạy MVC tests và server tests.
2. Kiểm tra browser thủ công các route.
3. Báo cáo file thay đổi, test đã chạy và phần chưa làm.
```
