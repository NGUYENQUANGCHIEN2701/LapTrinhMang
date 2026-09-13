# Tankzors Pro — Danh sách công việc Trello

## Cấu trúc thư mục dự án

```
TankzorsPro/
├── README.md
├── .gitignore
│
├── server/                          # Java Backend (WebSocket Server)
│   ├── pom.xml                      # Cấu hình Maven build
│   └── src/
│       ├── main/
│       │   ├── java/com/tankzors/
│       │   │   ├── Main.java                        # Điểm khởi chạy server
│       │   │   │
│       │   │   ├── config/
│       │   │   │   └── ServerConfig.java            # Cấu hình server (port, tick rate...)
│       │   │   │
│       │   │   ├── network/                         # Tầng Mạng (Networking Layer)
│       │   │   │   ├── WebSocketServer.java         # Khởi tạo WebSocket server
│       │   │   │   ├── ConnectionManager.java       # Quản lý kết nối client
│       │   │   │   ├── MessageHandler.java          # Xử lý message từ client
│       │   │   │   ├── MessageSender.java           # Gửi message về client
│       │   │   │   └── protocol/
│       │   │   │       ├── GameMessage.java          # Lớp cơ sở cho mọi message
│       │   │   │       ├── InputMessage.java         # Message input từ client
│       │   │   │       ├── StateMessage.java         # Message trạng thái game gửi về client
│       │   │   │       └── ResultMessage.java        # Message kết quả trận đấu
│       │   │   │
│       │   │   ├── engine/                          # Lõi Game (Game Engine)
│       │   │   │   ├── GameEngine.java              # Vòng lặp game chính (game loop)
│       │   │   │   ├── GameState.java               # Trạng thái hiện tại của trận đấu
│       │   │   │   ├── CollisionManager.java        # Xử lý va chạm
│       │   │   │   ├── BulletManager.java           # Quản lý đạn (tạo, di chuyển, huỷ)
│       │   │   │   └── PhysicsEngine.java           # Tính toán vật lý (vị trí, vận tốc)
│       │   │   │
│       │   │   ├── entity/                          # Các thực thể trong game
│       │   │   │   ├── Tank.java                    # Lớp cơ sở xe tăng (abstract)
│       │   │   │   ├── PlayerTank.java              # Xe tăng người chơi
│       │   │   │   ├── AITank.java                  # Xe tăng AI
│       │   │   │   ├── Bullet.java                  # Viên đạn
│       │   │   │   ├── Wall.java                    # Tường
│       │   │   │   ├── Obstacle.java                # Chướng ngại vật
│       │   │   │   └── Item.java                    # Vật phẩm (sao, tiền, power-up)
│       │   │   │
│       │   │   ├── interfaces/                      # Các Interface (SOLID - DIP)
│       │   │   │   ├── ITank.java                   # Interface xe tăng
│       │   │   │   ├── IBullet.java                 # Interface đạn
│       │   │   │   ├── IMap.java                    # Interface bản đồ
│       │   │   │   ├── IWeapon.java                 # Interface vũ khí (OCP)
│       │   │   │   ├── ICollidable.java             # Interface va chạm
│       │   │   │   ├── IGameObserver.java           # Interface observer sự kiện game
│       │   │   │   └── IAIBehavior.java             # Interface hành vi AI (LSP)
│       │   │   │
│       │   │   ├── map/                             # Dữ liệu bản đồ
│       │   │   │   ├── MapLoader.java               # Đọc và tải bản đồ
│       │   │   │   ├── GameMap.java                 # Đối tượng bản đồ
│       │   │   │   ├── TileType.java                # Enum loại ô (tường, đất, nước...)
│       │   │   │   └── maps/                        # Thư mục chứa file bản đồ
│       │   │   │       ├── campaign_1.json          # Bản đồ Campaign màn 1
│       │   │   │       ├── campaign_2.json          # Bản đồ Campaign màn 2
│       │   │   │       ├── campaign_3.json          # Bản đồ Campaign màn 3
│       │   │   │       ├── online_1.json            # Bản đồ Online 1
│       │   │   │       ├── online_2.json            # Bản đồ Online 2
│       │   │   │       └── online_3.json            # Bản đồ Online 3
│       │   │   │
│       │   │   ├── ai/                              # Logic AI
│       │   │   │   ├── AIController.java            # Điều khiển AI chính
│       │   │   │   ├── PatrolBehavior.java          # Hành vi tuần tra
│       │   │   │   ├── ChaseBehavior.java           # Hành vi đuổi theo người chơi
│       │   │   │   ├── ShootBehavior.java           # Hành vi bắn
│       │   │   │   └── DifficultyLevel.java         # Enum độ khó (EASY, MEDIUM, HARD)
│       │   │   │
│       │   │   ├── mode/                            # Chế độ chơi
│       │   │   │   ├── GameMode.java                # Interface chế độ chơi
│       │   │   │   ├── CampaignMode.java            # Logic Campaign (tuần tự, unlock)
│       │   │   │   ├── OnlineMode.java              # Logic Online (độc lập)
│       │   │   │   └── ProgressManager.java         # Quản lý tiến trình (sao, tiền, vật phẩm)
│       │   │   │
│       │   │   ├── leaderboard/                     # Bảng xếp hạng
│       │   │   │   ├── LeaderboardManager.java      # Quản lý bảng xếp hạng
│       │   │   │   ├── ScoreEntry.java              # Một dòng điểm trong bảng
│       │   │   │   └── LeaderboardStorage.java      # Lưu trữ bảng xếp hạng (MySQL)
│       │   │   │
│       │   │   └── database/                        # Kết nối Database
│       │   │       ├── DatabaseConfig.java          # Cấu hình MySQL (host, port, user...)
│       │   │       └── DatabaseManager.java         # Quản lý connection pool (HikariCP)
│       │   │
│       │   └── resources/
│       │       ├── maps/                            # File JSON bản đồ
│       │       └── config.properties                # Cấu hình mặc định
│       │
│       └── test/java/com/tankzors/                  # Unit tests
│           ├── engine/
│           │   └── CollisionManagerTest.java
│           ├── entity/
│           │   └── TankTest.java
│           └── network/
│               └── MessageHandlerTest.java
│
├── client/                          # React Frontend (Canvas Game)
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.jsx                              # Điểm khởi chạy React
│       ├── App.jsx                               # Component gốc
│       │
│       ├── game/                                 # Logic game phía client
│       │   ├── GameCanvas.jsx                    # Component Canvas chính
│       │   ├── GameLoop.js                       # Vòng lặp render (requestAnimationFrame)
│       │   ├── Renderer.js                       # Vẽ tất cả lên Canvas
│       │   ├── InputHandler.js                   # Bắt phím điều khiển (WASD/Arrow)
│       │   └── SpriteManager.js                  # Quản lý hình ảnh/sprite
│       │
│       ├── network/                              # Giao tiếp mạng
│       │   ├── WebSocketClient.js                # Kết nối WebSocket đến server
│       │   ├── MessageParser.js                  # Parse message từ server
│       │   └── MessageBuilder.js                 # Tạo message gửi lên server
│       │
│       ├── ui/                                   # Giao diện người dùng
│       │   ├── screens/
│       │   │   ├── MainMenu.jsx                  # Màn hình menu chính
│       │   │   ├── CampaignSelect.jsx            # Màn hình chọn màn Campaign
│       │   │   ├── OnlineSelect.jsx              # Màn hình chọn map Online
│       │   │   ├── GameScreen.jsx                # Màn hình chơi game
│       │   │   ├── ResultScreen.jsx              # Màn hình kết quả (sao, tiền)
│       │   │   └── LeaderboardScreen.jsx         # Màn hình bảng xếp hạng
│       │   │
│       │   ├── components/
│       │   │   ├── HealthBar.jsx                 # Thanh máu HP
│       │   │   ├── StarRating.jsx                # Hiển thị sao
│       │   │   ├── LevelCard.jsx                 # Card 1 màn chơi (khoá/mở)
│       │   │   ├── ItemInventory.jsx             # Hiển thị kho vật phẩm
│       │   │   └── ScoreTable.jsx                # Bảng điểm xếp hạng
│       │   │
│       │   └── hooks/
│       │       ├── useWebSocket.js               # Hook quản lý WebSocket
│       │       ├── useGameState.js               # Hook trạng thái game
│       │       └── useInput.js                   # Hook bắt phím
│       │
│       └── assets/                               # Tài nguyên hình ảnh
│           ├── tanks/
│           │   ├── player-tank.png
│           │   └── enemy-tank.png
│           ├── bullets/
│           │   └── bullet.png
│           ├── tiles/
│           │   ├── wall.png
│           │   ├── ground.png
│           │   └── obstacle.png
│           └── ui/
│               ├── star.png
│               ├── coin.png
│               └── lock.png
│
└── docs/                            # Tài liệu dự án
    ├── ARCHITECTURE.md              # Sơ đồ kiến trúc SOLID
    ├── API-PROTOCOL.md              # Mô tả giao thức WebSocket message
    ├── GAME-DESIGN.md               # Thiết kế game (màn chơi, độ khó, vật phẩm)
    └── DEPLOYMENT.md                # Hướng dẫn chạy/deploy
```

---

## Mô tả chi tiết từng Task

---

## GIAI ĐOẠN 1: LÕI GAME + KHUNG CƠ BẢN

---

### [1.1] Khởi tạo project Maven (Java server) + cấu trúc thư mục SOLID

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Backend, Kiến trúc
**Độ khó:** Trung bình

**Mô tả:**
Tạo project Java sử dụng Maven làm build tool. Cấu trúc thư mục phải phản ánh rõ nguyên tắc SOLID — mỗi package chứa một nhóm trách nhiệm riêng biệt. Phải cài đặt được các dependency cần thiết và chạy được server rỗng (chưa có logic).

**Việc cần làm:**
- Tạo thư mục `server/` với `pom.xml`
- Thêm dependency: Javalin (WebSocket), Gson (JSON), SLF4J (log), MySQL Connector/J (JDBC), HikariCP (connection pool)
- Tạo các package: `network/`, `engine/`, `entity/`, `interfaces/`, `map/`, `ai/`, `mode/`, `leaderboard/`, `database/`
- Tạo `Main.java` khởi động server trên port 8080
- Tạo `ServerConfig.java` chứa các hằng số (PORT, TICK_RATE, MAP_SIZE)
- Tạo `config.properties` với cấu hình MySQL (host, port, db name, user, password)
- Viết `.gitignore` cho Java/Maven

**Tiêu chí hoàn thành:**
- [ ] `mvn clean compile` thành công, không lỗi
- [ ] `mvn exec:java` khởi động server, in ra "Tankzors Server started on port 8080"
- [ ] Cấu trúc thư mục đúng theo sơ đồ
- [ ] Tất cả package có ít nhất 1 file (có thể là placeholder)

---

### [1.2] Khởi tạo project Vite + React (client)

**Người phụ trách:** C (Frontend)
**Label:** Frontend
**Độ khó:** Dễ

**Mô tả:**
Tạo project React sử dụng Vite. Client sẽ dùng HTML5 Canvas để vẽ game và WebSocket để giao tiếp với server. Giao diện chia thành các màn hình (menu, chơi game, kết quả).

**Việc cần làm:**
- Chạy `npm create vite@latest client -- --template react`
- Tạo cấu trúc thư mục: `game/`, `network/`, `ui/screens/`, `ui/components/`, `ui/hooks/`, `assets/`
- Tạo `GameCanvas.jsx` với 1 thẻ `<canvas>` kích thước 800x600
- Tạo `MainMenu.jsx` với 3 nút: "Campaign", "Online", "Bảng xếp hạng"
- Cài thêm `react-router-dom` để chuyển trang
- Viết `.gitignore` cho Node/React

**Tiêu chí hoàn thành:**
- [ ] `npm run dev` chạy thành công
- [ ] Mở trình duyệt thấy menu chính với 3 nút bấm
- [ ] Bấm "Campaign" hoặc "Online" chuyển sang trang có Canvas
- [ ] Cấu trúc thư mục đúng theo sơ đồ

---

### [1.3] Thiết kế interfaces SOLID: ITank, IBullet, IMap, IWeapon

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Kiến trúc, SOLID
**Độ khó:** Trung bình

**Mô tả:**
Tạo các interface Java — đây là xương sống của kiến trúc SOLID. Tất cả class cụ thể (PlayerTank, AITank, NormalBullet...) sẽ implement các interface này. GameEngine chỉ làm việc với interface, KHÔNG import class cụ thể.

**Các interface cần tạo:**

```java
// ITank.java
public interface ITank {
    String getId();
    void move(Direction direction, double deltaTime);
    void rotate(double angle);
    Position getPosition();
    double getRotation();
    int getHP();
    int getMaxHP();
    void takeDamage(int damage);
    boolean isAlive();
    IBullet shoot();
}

// IBullet.java
public interface IBullet {
    void update(double deltaTime);
    Position getPosition();
    double getDirection();
    int getDamage();
    double getSpeed();
    boolean isActive();
    void deactivate();
    String getOwnerId(); // ID của tank đã bắn viên đạn này
}

// IMap.java
public interface IMap {
    int getWidth();
    int getHeight();
    TileType getTileAt(int x, int y);
    List<Wall> getWalls();
    List<Position> getSpawnPoints();
    boolean isWalkable(int x, int y);
}

// IWeapon.java (OCP - thêm vũ khí mới không sửa code cũ)
public interface IWeapon {
    String getName();
    int getDamage();
    double getFireRate();     // số giây giữa 2 lần bắn
    double getBulletSpeed();
    IBullet createBullet(Position pos, double direction);
}

// ICollidable.java
public interface ICollidable {
    Rectangle getBoundingBox();
    boolean collidesWith(ICollidable other);
}

// IAIBehavior.java (LSP - các behavior thay thế nhau được)
public interface IAIBehavior {
    void update(AITank self, GameState state, double deltaTime);
    String getBehaviorName();
}

// IGameObserver.java (thông báo sự kiện game)
public interface IGameObserver {
    void onTankHit(ITank target, IBullet bullet);
    void onTankDestroyed(ITank tank);
    void onMatchEnd(String winnerId);
    void onItemCollected(ITank tank, Item item);
}
```

**Tiêu chí hoàn thành:**
- [ ] Tất cả 7 interface được tạo trong package `interfaces/`
- [ ] Mỗi interface có Javadoc giải thích mục đích
- [ ] Không có class nào trong `engine/` import trực tiếp từ `entity/` (chỉ import từ `interfaces/`)

---

### [1.4] Tạo WebSocket server (Javalin) - kết nối cơ bản

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Mạng, Backend
**Độ khó:** Trung bình

**Mô tả:**
Cài đặt WebSocket server sử dụng Javalin. Server phải có khả năng: chấp nhận kết nối từ client, nhận message, gửi message trả lại. Đây là tầng mạng (Network Layer) — chỉ xử lý truyền nhận dữ liệu, KHÔNG xử lý logic game (SRP).

**Việc cần làm:**
- Tạo `WebSocketServer.java`: khởi tạo Javalin app với WebSocket endpoint tại `/game`
- Tạo `ConnectionManager.java`: lưu danh sách client đang kết nối (Map<sessionId, WsContext>)
- Tạo `MessageHandler.java`: nhận message JSON từ client, parse thành object
- Tạo `MessageSender.java`: gửi message JSON về 1 client hoặc broadcast tất cả
- Tạo các lớp protocol: `GameMessage.java`, `InputMessage.java`, `StateMessage.java`

**Giao thức message (JSON):**
```json
// Client gửi lên server (input)
{"type": "INPUT", "action": "MOVE", "direction": "UP", "timestamp": 1234567890}
{"type": "INPUT", "action": "SHOOT", "timestamp": 1234567890}

// Server gửi về client (trạng thái)
{"type": "STATE", "tanks": [...], "bullets": [...], "timestamp": 1234567890}
{"type": "RESULT", "winner": "player1", "stars": 3, "reward": 100}
```

**Tiêu chí hoàn thành:**
- [ ] Server chạy, client kết nối được qua `ws://localhost:8080/game`
- [ ] Gửi 1 message từ browser console → server in ra log nhận được
- [ ] Server gửi 1 message trả lại → browser nhận được
- [ ] ConnectionManager theo dõi đúng số lượng client (kết nối/ngắt kết nối)

---

### [1.5] Tạo WebSocket client (React) - kết nối đến server

**Người phụ trách:** C (Frontend)
**Label:** Mạng, Frontend
**Độ khó:** Trung bình

**Mô tả:**
Tạo module WebSocket phía client để kết nối đến Java server. Phải xử lý các trạng thái: đang kết nối, đã kết nối, mất kết nối, lỗi. Client gửi input (phím bấm) lên server và nhận trạng thái game từ server.

**Việc cần làm:**
- Tạo `WebSocketClient.js`:
  - Kết nối đến `ws://localhost:8080/game`
  - Tự động kết nối lại khi mất kết nối (retry 3 lần)
  - Phát sự kiện: onOpen, onMessage, onClose, onError
- Tạo `MessageParser.js`: parse JSON message từ server thành object JavaScript
- Tạo `MessageBuilder.js`: tạo JSON message gửi lên server
- Tạo hook `useWebSocket.js`:
  - Trả về trạng thái kết nối (connecting/connected/disconnected/error)
  - Trả về hàm `sendMessage(type, data)`
  - Trả về message mới nhất nhận được

**Tiêu chí hoàn thành:**
- [ ] Mở trang web, góc trái hiện "Đang kết nối..." rồi chuyển thành "Đã kết nối"
- [ ] Tắt server → hiện "Mất kết nối", tự động thử kết nối lại
- [ ] Bật server lại → tự động kết nối lại thành công
- [ ] Console log hiện đầy đủ message gửi/nhận

---

### [1.6] GameEngine: vòng lặp game chính (game loop 30Hz)

**Người phụ trách:** B (Backend)
**Label:** Backend, Engine
**Độ khó:** Khó

**Mô tả:**
Tạo vòng lặp game (game loop) chạy 30 lần/giây (30Hz = 33ms mỗi tick). Mỗi tick, engine cập nhật toàn bộ trạng thái: di chuyển tank, di chuyển đạn, kiểm tra va chạm, rồi gửi trạng thái mới về client. Đây là "trái tim" của game.

**Việc cần làm:**
- Tạo `GameEngine.java`:
  - Dùng `ScheduledExecutorService` để chạy loop 30Hz
  - Mỗi tick gọi: `processInputs()` → `updateEntities()` → `checkCollisions()` → `broadcastState()`
  - Tính deltaTime (thời gian giữa 2 tick) để di chuyển mượt
- Tạo `GameState.java`:
  - Chứa: danh sách tanks, danh sách bullets, thông tin map hiện tại
  - Phương thức `toJSON()` để serialize gửi cho client

**Quy trình 1 tick:**
```
1. Nhận input từ hàng đợi (người chơi bấm phím gì)
2. Áp dụng input: di chuyển tank theo hướng đã bấm
3. Cập nhật đạn: mỗi viên đạn tiến thêm 1 đoạn
4. Kiểm tra va chạm: đạn-tường, đạn-tank, tank-tường
5. Xử lý kết quả: giảm HP nếu trúng, huỷ đạn nếu chạm tường
6. Gửi trạng thái mới về tất cả client
```

**Tiêu chí hoàn thành:**
- [ ] Game loop chạy ổn định 30Hz (không bị lag/skip frame)
- [ ] Log ra số tick/giây, dao động quanh 30 (+/- 2)
- [ ] deltaTime được tính đúng (khoảng 0.033s)
- [ ] Tắt server bằng Ctrl+C, loop dừng sạch (không bị treo)

---

### [1.7] TankEntity: di chuyển lên/xuống/trái/phải + xoay

**Người phụ trách:** B (Backend)
**Label:** Backend, Entity
**Độ khó:** Trung bình

**Mô tả:**
Tạo lớp Tank (abstract) và PlayerTank (concrete). Tank có thể di chuyển 4 hướng và xoay theo hướng di chuyển. Vị trí được cập nhật mỗi tick dựa trên tốc độ và deltaTime.

**Việc cần làm:**
- Tạo `Tank.java` (abstract, implements ITank, ICollidable):
  - Thuộc tính: id, position (x, y), rotation, speed, hp, maxHp, width, height
  - Di chuyển: `move(direction, deltaTime)` → position += speed * deltaTime
  - Xoay: khi di chuyển lên, rotation = 0; phải = 90; xuống = 180; trái = 270
  - Va chạm: `getBoundingBox()` trả về hình chữ nhật bao quanh tank
- Tạo `PlayerTank.java` (extends Tank):
  - Speed = 150 pixels/giây
  - HP = 100, MaxHP = 100
  - Kích thước: 40x40 pixels
- Tạo `Position.java`: class đơn giản chứa x, y (double)
- Tạo `Direction.java`: enum UP, DOWN, LEFT, RIGHT

**Tiêu chí hoàn thành:**
- [ ] Tạo PlayerTank tại vị trí (100, 100), gọi move(RIGHT, 0.033) → vị trí mới là (104.95, 100)
- [ ] Gọi move(UP, 0.033) → rotation = 0 (hướng lên)
- [ ] getBoundingBox() trả về Rectangle đúng kích thước
- [ ] Unit test kiểm tra di chuyển 4 hướng đều đúng

---

### [1.8] MapLoader: đọc dữ liệu bản đồ (tường, chướng ngại vật)

**Người phụ trách:** B (Backend)
**Label:** Backend, Map
**Độ khó:** Trung bình

**Mô tả:**
Tạo hệ thống đọc bản đồ từ file JSON. Mỗi bản đồ là 1 lưới ô (grid), mỗi ô có loại (đất, tường, nước, spawn point). MapLoader đọc file và tạo object GameMap để các thành phần khác sử dụng.

**Việc cần làm:**
- Tạo `GameMap.java` (implements IMap):
  - Lưu lưới ô 2 chiều (int[][]) — mỗi giá trị là 1 TileType
  - `getWalls()`: trả về danh sách Rectangle của tất cả ô tường
  - `isWalkable(x, y)`: true nếu ô đó không phải tường
  - `getSpawnPoints()`: trả về vị trí xuất phát của player và AI
- Tạo `TileType.java` (enum):
  - GROUND = 0 (đi được)
  - WALL = 1 (không đi được, chặn đạn)
  - WATER = 2 (không đi được)
  - SPAWN_PLAYER = 3
  - SPAWN_ENEMY = 4
- Tạo `MapLoader.java`:
  - Đọc file JSON từ resources/maps/
  - Parse thành GameMap object
- Tạo 1 file map mẫu `campaign_1.json`:

```json
{
  "name": "Màn 1 - Khu vực tập luyện",
  "width": 20,
  "height": 15,
  "tileSize": 40,
  "grid": [
    [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,1,1,0,0,0,0,0,1,1,0,0,0,0,0,1],
    [1,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,1,0,1,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,1],
    [1,0,0,0,0,1,1,0,0,0,0,0,1,1,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
    [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1]
  ]
}
```

**Tiêu chí hoàn thành:**
- [ ] `MapLoader.load("campaign_1")` trả về GameMap không lỗi
- [ ] `map.getTileAt(0, 0)` trả về WALL
- [ ] `map.isWalkable(1, 1)` trả về true
- [ ] `map.getSpawnPoints()` trả về 2 vị trí (player và enemy)
- [ ] `map.getWalls()` trả về danh sách Rectangle của tất cả ô tường

---

### [1.9] CollisionManager: va chạm tank-tường

**Người phụ trách:** B (Backend)
**Label:** Backend, Engine
**Độ khó:** Khó

**Mô tả:**
Xử lý va chạm giữa xe tăng và tường. Khi tank di chuyển, nếu vị trí mới trùng với tường thì không cho di chuyển (tank dừng lại trước tường). Dùng phương pháp AABB (Axis-Aligned Bounding Box).

**Việc cần làm:**
- Tạo `CollisionManager.java`:
  - `checkTankWallCollision(ITank tank, IMap map, Position newPos)`: trả về true nếu vị trí mới bị trùng tường
  - `checkBulletWallCollision(IBullet bullet, IMap map)`: trả về true nếu đạn chạm tường
  - `checkBulletTankCollision(IBullet bullet, ITank tank)`: trả về true nếu đạn trúng tank
  - `resolveCollision(ITank tank, Position newPos, IMap map)`: trả về vị trí hợp lệ gần nhất (trượt dọc theo tường nếu có thể)

**Thuật toán AABB:**
```
// 2 hình chữ nhật A và B va chạm khi:
// A.left < B.right AND A.right > B.left AND
// A.top < B.bottom AND A.bottom > B.top

boolean collides(Rectangle a, Rectangle b) {
    return a.x < b.x + b.width &&
           a.x + a.width > b.x &&
           a.y < b.y + b.height &&
           a.y + a.height > b.y;
}
```

**Trường hợp đặc biệt cần xử lý:**
- Tank di chuyển chéo (vừa lên vừa phải) va chạm góc tường → cho trượt 1 chiều
- Đạn bay nhanh có thể "xuyên" qua tường mỏng → kiểm tra theo đường thẳng, không chỉ kiểm tra điểm

**Tiêu chí hoàn thành:**
- [ ] Tank di chuyển vào tường → dừng lại, không xuyên qua
- [ ] Tank di chuyển dọc theo tường → trượt mượt, không bị kẹt
- [ ] Đạn bay vào tường → bị huỷ (isActive = false)
- [ ] Đạn bay vào tank → trả về trúng (collision detected)
- [ ] Unit test cho 4 trường hợp trên

---

### [1.10] BulletManager: tạo đạn, di chuyển, va chạm đạn-tường

**Người phụ trách:** B (Backend)
**Label:** Backend, Engine
**Độ khó:** Trung bình

**Mô tả:**
Quản lý vòng đời của tất cả viên đạn: tạo mới khi tank bắn, cập nhật vị trí mỗi tick, huỷ khi chạm tường hoặc chạm tank. Mỗi tick, tất cả đạn được cập nhật vị trí rồi kiểm tra va chạm.

**Việc cần làm:**
- Tạo `Bullet.java` (implements IBullet, ICollidable):
  - Thuộc tính: id, position, direction (góc độ), speed (300 px/s), damage (25), active, ownerId
  - `update(deltaTime)`: position += speed * deltaTime * cos/sin(direction)
  - `deactivate()`: active = false (sẽ bị xoá tick sau)
- Tạo `BulletManager.java`:
  - `createBullet(tankId, position, direction)`: tạo viên đạn mới, thêm vào danh sách
  - `updateAll(deltaTime)`: cập nhật vị trí tất cả đạn đang active
  - `removeInactive()`: xoá các đạn đã deactivate
  - `getBullets()`: trả về danh sách đạn đang active (để gửi cho client)

**Giới hạn:**
- Tối đa 5 viên đạn trên màn hình cùng lúc (mỗi tank)
- Cooldown 0.5 giây giữa 2 lần bắn

**Tiêu chí hoàn thành:**
- [ ] Bắn 1 viên đạn → đạn xuất hiện và bay thẳng theo hướng tank
- [ ] Đạn chạm tường → biến mất
- [ ] Đạn chạm tank đối phương → tank mất 25 HP, đạn biến mất
- [ ] Đạn của bạn không trúng chính bạn (kiểm tra ownerId)
- [ ] Không bắn được nếu chưa hết cooldown
- [ ] Không bắn được nếu đã có 5 viên đạn trên màn hình

---

### [1.11] Canvas render: vẽ bản đồ + xe tăng + đạn

**Người phụ trách:** C (Frontend)
**Label:** Frontend, Game
**Độ khó:** Khó

**Mô tả:**
Vẽ toàn bộ game lên HTML5 Canvas dựa trên trạng thái nhận từ server. Mỗi frame (60fps), đọc trạng thái mới nhất và vẽ lại: bản đồ (tường, đất), các xe tăng (vị trí, hướng), các viên đạn, thanh máu HP.

**Việc cần làm:**
- Tạo `Renderer.js`:
  - `drawMap(ctx, mapData)`: vẽ lưới bản đồ — tường màu xám, đất màu vàng nhạt
  - `drawTank(ctx, tank)`: vẽ xe tăng tại vị trí (x,y) xoay theo rotation
    - Tank người chơi: màu xanh lá
    - Tank AI: màu đỏ
  - `drawBullet(ctx, bullet)`: vẽ viên đạn (hình tròn nhỏ màu vàng)
  - `drawHUD(ctx, playerHP, maxHP)`: vẽ thanh máu HP phía trên màn hình
- Tạo `GameLoop.js`:
  - Dùng `requestAnimationFrame` chạy 60fps
  - Mỗi frame: xoá canvas → vẽ map → vẽ tanks → vẽ bullets → vẽ HUD
- Tạo `SpriteManager.js` (tuỳ chọn):
  - Tải trước tất cả hình ảnh (tank, tường, đạn)
  - Hoặc dùng hình học đơn giản (hình chữ nhật, hình tròn) cho giai đoạn đầu

**Kích thước Canvas:** 800x600 pixels
**Kích thước 1 ô:** 40x40 pixels → bản đồ 20x15 ô

**Tiêu chí hoàn thành:**
- [ ] Mở trang web thấy bản đồ vẽ đúng (tường bao quanh, ô trống giữa)
- [ ] Xe tăng hiển thị đúng vị trí, xoay đúng hướng
- [ ] Đạn bay thấy rõ trên màn hình (không quá nhỏ, không quá nhanh)
- [ ] Thanh HP hiển thị phía trên, giảm khi bị bắn
- [ ] Không bị giật/lag (60fps ổn định)

---

### [1.12] Bắt phím điều khiển + gửi input qua WebSocket

**Người phụ trách:** C (Frontend)
**Label:** Frontend, Mạng
**Độ khó:** Trung bình

**Mô tả:**
Bắt phím (WASD hoặc phím mũi tên) để điều khiển xe tăng. Mỗi khi người chơi bấm/nhả phím, gửi message input lên server. KHÔNG di chuyển tank trên client — chỉ gửi lệnh, đợi server trả về vị trí mới.

**Việc cần làm:**
- Tạo `InputHandler.js`:
  - Lắng nghe sự kiện `keydown` và `keyup`
  - Theo dõi phím đang bấm (Set chứa các phím đang giữ)
  - Phím điều khiển:
    - W hoặc ArrowUp = di chuyển lên
    - S hoặc ArrowDown = di chuyển xuống
    - A hoặc ArrowLeft = di chuyển trái
    - D hoặc ArrowRight = di chuyển phải
    - Space = bắn
  - Khi bấm phím → gửi message `{"type":"INPUT","action":"MOVE","direction":"UP"}`
  - Khi nhả phím → gửi message `{"type":"INPUT","action":"STOP"}`
- Tạo hook `useInput.js`:
  - Kết hợp với useWebSocket để gửi input
  - Tránh gửi trùng lặp (nếu đang giữ phím, không gửi liên tục mỗi frame)

**Lưu ý quan trọng:**
- Chỉ gửi khi THAY ĐỔI trạng thái (bấm mới hoặc nhả ra), không gửi mỗi frame
- Nếu người chơi bấm 2 phím cùng lúc (vd: W + D) → gửi cả 2 direction
- Phân biệt "đang giữ phím" và "vừa bấm phím"

**Tiêu chí hoàn thành:**
- [ ] Bấm W → server nhận message MOVE UP
- [ ] Nhả W → server nhận message STOP
- [ ] Bấm Space → server nhận message SHOOT
- [ ] Bấm giữ W liên tục → chỉ gửi 1 lần MOVE UP (không spam)
- [ ] Bấm W + D cùng lúc → tank di chuyển chéo (lên + phải)

---

### [1.13] Hiển thị HP xe tăng + xử lý đạn trúng giảm HP

**Người phụ trách:** B + C (Backend + Frontend)
**Label:** Fullstack
**Độ khó:** Trung bình

**Mô tả:**
Khi đạn trúng xe tăng, server giảm HP của tank bị trúng và gửi trạng thái mới. Client hiển thị thanh máu HP thay đổi. Khi HP = 0, tank bị phá huỷ (biến mất khỏi màn hình).

**Việc cần làm (B - Backend):**
- Trong `GameEngine`, mỗi tick sau khi kiểm tra va chạm đạn-tank:
  - Nếu trúng: gọi `tank.takeDamage(bullet.getDamage())`
  - Nếu HP <= 0: gọi `tank.destroy()`, xoá tank khỏi danh sách active
  - Deactivate viên đạn đã trúng
- Thêm trường HP vào StateMessage gửi về client:
  ```json
  {"type":"STATE","tanks":[{"id":"player1","x":100,"y":200,"hp":75,"maxHp":100,"rotation":0}]}
  ```

**Việc cần làm (C - Frontend):**
- Tạo `HealthBar.jsx`:
  - Thanh ngang phía trên màn hình hoặc phía trên đầu tank
  - Màu xanh khi HP > 60%, vàng khi 30-60%, đỏ khi < 30%
  - Hiển thị số: "75/100"
- Cập nhật `Renderer.js`:
  - Vẽ thanh HP nhỏ phía trên mỗi tank
  - Khi tank bị phá huỷ: vẽ hiệu ứng nổ (tuỳ chọn) hoặc đơn giản biến mất

**Tiêu chí hoàn thành:**
- [ ] Bắn trúng tank đối phương → HP giảm 25 (100 → 75 → 50 → 25 → 0)
- [ ] Thanh máu cập nhật tức thì trên màn hình
- [ ] HP = 0 → tank biến mất
- [ ] Đạn của mình không trúng chính mình

---

### [1.14] Tích hợp end-to-end: client gửi lệnh → server xử lý → client hiển thị

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Tích hợp, Testing
**Độ khó:** Khó

**Mô tả:**
Ghép nối tất cả thành phần lại: client bắt phím → gửi WebSocket → server nhận và xử lý trong game loop → server gửi trạng thái mới → client vẽ lên Canvas. Đây là task cuối cùng của Giai đoạn 1, kiểm tra toàn bộ hoạt động đúng.

**Việc cần làm:**
- Kết nối MessageHandler với GameEngine (input vào hàng đợi)
- Kết nối GameEngine với MessageSender (sau mỗi tick, broadcast state)
- Đảm bảo đồng bộ: client gửi input → server xử lý → client nhận kết quả < 100ms
- Xử lý trường hợp mất kết nối giữa chừng: client hiện "Mất kết nối", game tạm dừng
- Test với 1 tab trình duyệt chơi thật

**Kịch bản test:**
1. Mở trình duyệt → thấy menu → chọn "Campaign" → thấy bản đồ với 1 tank xanh và 1 tank đỏ
2. Bấm W → tank xanh đi lên, không xuyên tường
3. Bấm Space → đạn bay ra, trúng tank đỏ → tank đỏ mất máu
4. Bắn cho đến khi tank đỏ hết máu → tank đỏ biến mất
5. Tắt server → trình duyệt hiện "Mất kết nối"

**Tiêu chí hoàn thành:**
- [ ] Toàn bộ kịch bản test trên chạy đúng
- [ ] Độ trễ input → hiển thị < 100ms (cảm giác mượt)
- [ ] Không bị lỗi crash khi chơi liên tục 2 phút
- [ ] Console không có lỗi JavaScript hoặc Java exception

---

## GIAI ĐOẠN 2: CHẾ ĐỘ CAMPAIGN + AI

---

### [2.1] AITank: logic di chuyển cơ bản (đuổi theo player)

**Người phụ trách:** B (Backend)
**Label:** AI, Backend
**Độ khó:** Khó

**Mô tả:**
Tạo logic AI để xe tăng máy di chuyển thông minh. AI cần có khả năng: phát hiện người chơi, di chuyển về phía người chơi, tránh tường.

**Việc cần làm:**
- Tạo `AIController.java`:
  - Mỗi tick, quyết định AI sẽ làm gì (di chuyển/bắn/đứng yên)
  - Dùng IAIBehavior interface để chuyển đổi hành vi
- Tạo `ChaseBehavior.java` (implements IAIBehavior):
  - Tính khoảng cách đến player
  - Nếu xa (> 200px): đi thẳng về phía player
  - Nếu có tường chặn: thử đi vòng (chọn hướng khác)
  - Nếu gần (< 100px): dừng lại và bắn
- Tạo `PatrolBehavior.java` (implements IAIBehavior):
  - Khi không thấy player: đi tuần tra (đi thẳng, gặp tường thì rẽ)
  - Khi phát hiện player (trong tầm nhìn): chuyển sang ChaseBehavior

**Tầm nhìn AI:**
- AI "thấy" player khi khoảng cách < 300px và không có tường chặn giữa
- Dùng ray-casting đơn giản (kiểm tra các ô giữa AI và player có tường không)

**Độ khó ảnh hưởng:**
- EASY: speed = 80px/s, tầm nhìn = 200px, phản ứng chậm (0.5s delay)
- MEDIUM: speed = 120px/s, tầm nhìn = 300px, phản ứng nhanh hơn (0.3s)
- HARD: speed = 140px/s, tầm nhìn = 400px, phản ứng nhanh (0.1s)

**Tiêu chí hoàn thành:**
- [ ] AI đứng yên khi player ở xa (ngoài tầm nhìn)
- [ ] AI bắt đầu đi về phía player khi vào tầm nhìn
- [ ] AI không bị kẹt ở tường (biết đi vòng)
- [ ] AI dừng lại khi đến gần player
- [ ] Thay đổi DifficultyLevel → AI nhanh/chậm khác nhau rõ ràng

---

### [2.2] AITank: logic bắn (nhắm và bắn player)

**Người phụ trách:** B (Backend)
**Label:** AI, Backend
**Độ khó:** Trung bình

**Mô tả:**
AI cần có khả năng bắn đạn về phía người chơi. AI sẽ bắn khi: đang ở trong tầm bắn, có đường bắn thẳng (không bị tường chặn), và đã hết cooldown.

**Việc cần làm:**
- Tạo `ShootBehavior.java` (implements IAIBehavior):
  - Kiểm tra: player trong tầm bắn? (khoảng cách < 250px)
  - Kiểm tra: có tường chặn giữa AI và player không?
  - Nếu cả 2 điều kiện thoả: xoay súng về phía player và bắn
  - Cooldown bắn: phụ thuộc độ khó
    - EASY: 2 giây/lần
    - MEDIUM: 1.5 giây/lần
    - HARD: 1 giây/lần
- Tính góc bắn: `atan2(player.y - ai.y, player.x - ai.x)`
- Thêm "độ chính xác": AI không bắn chính xác 100%
  - EASY: sai lệch +/- 15 độ
  - MEDIUM: sai lệch +/- 8 độ
  - HARD: sai lệch +/- 3 độ

**Tiêu chí hoàn thành:**
- [ ] AI bắn đạn về phía player khi trong tầm
- [ ] AI không bắn khi có tường chặn
- [ ] Đạn AI có thể trúng hoặc trượt (tuỳ độ chính xác)
- [ ] AI EASY bắn chậm và sai nhiều, AI HARD bắn nhanh và chính xác
- [ ] AI không bắn liên tục (có cooldown)

---

### [2.3] Thiết kế 3 bản đồ Campaign (Màn 1 dễ, Màn 2 TB, Màn 3 khó)

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Game Design
**Độ khó:** Trung bình

**Mô tả:**
Thiết kế 3 bản đồ cho chế độ Campaign. Mỗi bản đồ phải có độ khó tăng dần, bố trí tường/chướng ngại vật khác nhau, vị trí spawn player và AI hợp lý.

**Việc cần làm:**
- Tạo file `campaign_1.json` (Màn 1 - Dễ):
  - Bản đồ đơn giản, ít tường, nhiều không gian trống
  - 1 AI enemy (EASY)
  - Nhiều chỗ ẩn nấp cho người chơi mới
- Tạo file `campaign_2.json` (Màn 2 - Trung bình):
  - Bản đồ phức tạp hơn, có nhiều tường tạo hành lang
  - 2 AI enemies (MEDIUM)
  - Có các ngõ cụt và góc kẹm
- Tạo file `campaign_3.json` (Màn 3 - Khó):
  - Bản đồ chật hẹp, nhiều chướng ngại vật
  - 2-3 AI enemies (HARD)
  - Ít chỗ ẩn nấp, nhiều đường đi ngắn
  - AI bắt đầu ở các vị trí chiến lược

**Mỗi file JSON gồm:**
```json
{
  "name": "Tên màn chơi",
  "difficulty": "EASY/MEDIUM/HARD",
  "enemies": [
    {"type": "AI", "difficulty": "EASY", "spawnX": 17, "spawnY": 11}
  ],
  "rewards": {"stars": 3, "coins": 100},
  "grid": [[...]]
}
```

**Tiêu chí hoàn thành:**
- [ ] 3 file JSON tạo xong, MapLoader đọc được cả 3
- [ ] Mỗi bản đồ chơi được (player và AI không bị kẹt)
- [ ] Độ khó tăng rõ ràng: Màn 1 dễ thắng, Màn 3 khó thắng
- [ ] Không có ô bị cô lập (mọi ô trống đều tiếp cận được)

---

### [2.4] Hệ thống mở khoá màn (thắng N → mở N+1)

**Người phụ trách:** B (Backend)
**Label:** Backend, Game Logic
**Độ khó:** Trung bình

**Mô tả:**
Quản lý tiến trình Campaign: người chơi bắt đầu chỉ mở Màn 1, thắng Màn 1 thì mở Màn 2, thắng Màn 2 thì mở Màn 3. Tiến trình lưu trên server (trong bộ nhớ hoặc file).

**Việc cần làm:**
- Tạo `CampaignMode.java` (implements GameMode):
  - `getUnlockedLevels()`: trả về danh sách màn đã mở
  - `completeLevel(levelId)`: đánh dấu màn đã thắng, mở màn tiếp theo
  - `isLevelUnlocked(levelId)`: kiểm tra màn có chơi được không
  - `getLevelReward(levelId)`: trả về phần thưởng của màn đó
- Tạo `ProgressManager.java`:
  - Lưu tiến trình: màn đã hoàn thành, số sao, số tiền, vật phẩm
  - `addReward(stars, coins, items)`: cộng phần thưởng vào tổng
  - `getProgress()`: trả về toàn bộ tiến trình hiện tại
  - Lưu vào bảng `player_progress` trong MySQL (dùng DatabaseManager từ task [3.3])
  - Cột `items_json` lưu vật phẩm dạng JSON: `{"shield": 1, "speedBoost": 2}`

**Tiêu chí hoàn thành:**
- [ ] Ban đầu chỉ Màn 1 chơi được
- [ ] Thắng Màn 1 → Màn 2 mở khoá
- [ ] Thử chọn Màn 3 khi chưa thắng Màn 2 → bị từ chối
- [ ] Restart server → tiến trình vẫn còn (đọc từ MySQL)

---

### [2.5] Màn hình chọn màn Campaign (hiển thị khoá/mở)

**Người phụ trách:** C (Frontend)
**Label:** Frontend, UI
**Độ khó:** Trung bình

**Mô tả:**
Tạo màn hình chọn màn cho chế độ Campaign. Hiển thị 3 card màn chơi, màn đã mở thì bấm được, màn chưa mở thì hiện khoá xám. Hiển thị số sao đã đạt được.

**Việc cần làm:**
- Tạo `CampaignSelect.jsx`:
  - Hiển thị 3 card xếp hàng ngang (hoặc dọc)
  - Mỗi card có: tên màn, hình ảnh thu nhỏ (hoặc icon), số sao (0-3), trạng thái khoá/mở
  - Bấm vào màn đã mở → bắt đầu chơi
  - Bấm vào màn khoá → hiện thông báo "Hoàn thành màn trước để mở khoá"
- Tạo `LevelCard.jsx`:
  - Props: levelId, name, stars, isUnlocked, onClick
  - Nếu isUnlocked = false: hiện icon khoá, làm mờ card
  - Nếu isUnlocked = true: hiện bình thường, có số sao vàng
- Lấy dữ liệu tiến trình từ server khi mở màn hình (gửi request qua WebSocket)

**Giao diện:**
```
+---------------------+---------------------+---------------------+
|  MÀN 1              |  MÀN 2              |  MÀN 3              |
|  Khu vực tập luyện  |  Hành lang tối      |  Pháo đài cuối      |
|  ★★★ (3 sao)        |  ★★ (2 sao)         |  [KHOÁ]             |
|  [CHƠI]             |  [CHƠI]             |                     |
+---------------------+---------------------+---------------------+
```

**Tiêu chí hoàn thành:**
- [ ] Hiển thị 3 card đúng thông tin từ server
- [ ] Màn chưa mở hiện icon khoá và không bấm được
- [ ] Màn đã thắng hiện số sao tương ứng
- [ ] Bấm "CHƠI" → chuyển sang màn hình game với bản đồ tương ứng

---

### [2.6] Màn hình kết quả: 3 sao + tiền thưởng + vật phẩm

**Người phụ trách:** C (Frontend)
**Label:** Frontend, UI
**Độ khó:** Dễ

**Mô tả:**
Khi người chơi thắng (hoặc thua) 1 màn, hiển thị màn hình kết quả. Thắng: hiện 3 sao vàng + tiền thưởng + vật phẩm đã nhặt. Thua: hiện thông báo và nút "Thử lại".

**Việc cần làm:**
- Tạo `ResultScreen.jsx`:
  - Nhận props: isWin, stars, coins, items, levelName
  - Nếu thắng:
    - Hiện "CHIẾN THẮNG!" (chữ to, màu vàng)
    - 3 ngôi sao vàng (animation xuất hiện từng cái)
    - "+100 xu" (hoặc số tiền tương ứng)
    - Danh sách vật phẩm nhặt được (nếu có)
    - Nút "Tiếp tục" → quay về màn hình chọn màn
    - Nút "Chơi lại" → chơi lại màn đó
  - Nếu thua:
    - Hiện "THẤT BẠI" (chữ to, màu đỏ)
    - Nút "Thử lại" → chơi lại màn đó
    - Nút "Quay về" → về menu

**Tiêu chí hoàn thành:**
- [ ] Thắng → hiện đúng kết quả (sao, tiền, vật phẩm)
- [ ] Thua → hiện thông báo và nút thử lại
- [ ] Bấm "Tiếp tục" → về màn hình chọn màn, màn tiếp theo đã mở khoá
- [ ] Bấm "Chơi lại" → chơi lại màn vừa chơi

---

### [2.7] Hệ thống vật phẩm: cộng dồn + giới hạn tối đa

**Người phụ trách:** B (Backend)
**Label:** Backend, Game Logic
**Độ khó:** Trung bình

**Mô tả:**
Quản lý kho vật phẩm của người chơi. Vật phẩm nhặt được trong các màn sẽ cộng dồn lại. Mỗi loại vật phẩm có giới hạn tối đa (vd: tối đa 3 shield, 5 speed boost).

**Việc cần làm:**
- Tạo `Item.java` (enum hoặc class):
  - Các loại: SHIELD (chống 1 phát đạn), SPEED_BOOST (tăng tốc 5s), EXTRA_DAMAGE (x2 dame 5s), HEAL (hồi 50 HP)
  - Mỗi loại có: name, description, maxStack, icon
- Cập nhật `ProgressManager.java`:
  - `addItem(ItemType type, int quantity)`: thêm vật phẩm, không vượt max
  - `useItem(ItemType type)`: sử dụng 1 vật phẩm, giảm số lượng
  - `getItemCount(ItemType type)`: lấy số lượng hiện tại
  - Giới hạn:
    - SHIELD: tối đa 3
    - SPEED_BOOST: tối đa 5
    - EXTRA_DAMAGE: tối đa 3
    - HEAL: tối đa 5
- Cập nhật `GameEngine`: khi tank đi qua vị trí có item trên map → thu thập và thêm vào kho

**Tiêu chí hoàn thành:**
- [ ] Nhặt shield khi đang có 3 → không tăng (đã tối đa)
- [ ] Nhặt heal khi có 4 → tăng lên 5 (tối đa)
- [ ] Qua màn mới, vật phẩm từ màn trước vẫn còn
- [ ] Hiển thị số lượng vật phẩm đúng trên UI

---

### [2.8] Điều chỉnh độ khó: số AI, tốc độ AI theo từng màn

**Người phụ trách:** A + B (Trưởng nhóm + Backend)
**Label:** Game Balance
**Độ khó:** Trung bình

**Mô tả:**
Cân chỉnh độ khó 3 màn Campaign để trải nghiệm chơi hợp lý: Màn 1 dễ thắng cho người mới, Màn 2 cần có chiến thuật, Màn 3 thách thức. Test chơi thử và điều chỉnh thông số.

**Việc cần làm:**
- Điều chỉnh thông số AI theo từng màn:

| Thông số | Màn 1 | Màn 2 | Màn 3 |
|----------|-------|-------|-------|
| Số AI | 1 | 2 | 3 |
| Tốc độ AI | 80 px/s | 120 px/s | 140 px/s |
| Cooldown bắn | 2s | 1.5s | 1s |
| Độ chính xác | +/-15° | +/-8° | +/-3° |
| Tầm nhìn | 200px | 300px | 400px |
| HP của AI | 50 | 75 | 100 |

- Chơi thử mỗi màn ít nhất 3 lần, điều chỉnh nếu quá dễ/khó
- Đảm bảo Màn 1 thắng được trong lần đầu chơi
- Đảm bảo Màn 3 cần ít nhất 2-3 lần thử mới thắng

**Tiêu chí hoàn thành:**
- [ ] Chơi thử Màn 1: thắng dễ dàng (dưới 1 phút)
- [ ] Chơi thử Màn 2: cần tập trung, có thể thua 1-2 lần đầu
- [ ] Chơi thử Màn 3: khó, cần chiến thuật và sử dụng vật phẩm
- [ ] Độ khó cảm giác công bằng, không quá bất lực

---

## GIAI ĐOẠN 3: CHẾ ĐỘ ONLINE + BẢNG XẾP HẠNG

---

### [3.1] Màn hình chọn bản đồ Online (3 map, không khoá)

**Người phụ trách:** C (Frontend)
**Label:** Frontend, UI
**Độ khó:** Dễ

**Mô tả:**
Tạo màn hình chọn bản đồ cho chế độ Online. Khác với Campaign, tất cả 3 map đều mở sẵn, không cần unlock. Hiển thị tên map, độ khó, và điểm cao nhất của người chơi.

**Việc cần làm:**
- Tạo `OnlineSelect.jsx`:
  - Hiển thị 3 card bản đồ
  - Mỗi card có: tên map, độ khó (Dễ/TB/Khó), điểm cao nhất (từ server)
  - Tất cả đều bấm được (không có khoá)
  - Bấm → bắt đầu chơi map đó
- Lấy điểm cao từ server khi mở màn hình (request qua WebSocket)
- Sau khi chơi xong → quay về màn hình này với điểm cập nhật

**Giao diện:**
```
+---------------------+---------------------+---------------------+
|  MAP 1              |  MAP 2              |  MAP 3              |
|  Đồng cỏ xanh      |  Thành phố đổ nát   |  Sa mạc lửa        |
|  Độ khó: DỄ        |  Độ khó: TB         |  Độ khó: KHÓ       |
|  Điểm cao: 1500    |  Điểm cao: 800     |  Điểm cao: --      |
|  [CHƠI]            |  [CHƠI]            |  [CHƠI]            |
+---------------------+---------------------+---------------------+
```

**Tiêu chí hoàn thành:**
- [ ] 3 map đều hiển thị và bấm được
- [ ] Điểm cao lấy từ server (hiện "--" nếu chưa chơi)
- [ ] Bấm CHƠI → vào game với map tương ứng
- [ ] Chơi xong → điểm cập nhật khi quay lại màn hình này

---

### [3.2] API lưu kết quả trận đấu lên server

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Mạng, Backend
**Độ khó:** Trung bình

**Mô tả:**
Sau mỗi trận đấu (Online mode), client gửi kết quả lên server để lưu lại. Server tính điểm dựa trên: thời gian hoàn thành, HP còn lại, số AI hạ được.

**Việc cần làm:**
- Thêm message type mới trong protocol:
  ```json
  // Client gửi sau khi thắng
  {"type": "SUBMIT_SCORE", "mapId": "online_1", "score": 1500, "time": 45, "hpRemain": 75}
  
  // Server trả lại xác nhận
  {"type": "SCORE_SAVED", "rank": 3, "isNewHighScore": true}
  ```
- Công thức tính điểm:
  ```
  điểm = (HP còn lại * 10) + (Số AI hạ * 200) + (Bonus thời gian: 1000 - time * 10)
  ```
  - VD: còn 75HP, hạ 2 AI, hết 45 giây → 75*10 + 2*200 + (1000 - 450) = 750 + 400 + 550 = 1700 điểm
- Server lưu điểm vào MySQL (bảng `leaderboard`)

**Tiêu chí hoàn thành:**
- [ ] Thắng trận Online → điểm được gửi lên và lưu vào MySQL
- [ ] Thua → không gửi điểm (chỉ người thắng mới có điểm)
- [ ] Restart server → điểm vẫn còn (đọc từ MySQL)
- [ ] Nếu điểm mới cao hơn điểm cũ của cùng người chơi trên cùng map → ghi đè

---

### [3.3] Database MySQL lưu điểm số trên server

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Backend, Data, Database
**Độ khó:** Trung bình

**Mô tả:**
Lưu trữ điểm số và bảng xếp hạng trên server bằng MySQL. Dùng JDBC kết nối MySQL, HikariCP làm connection pool. Phải xử lý đọc/ghi an toàn khi nhiều request cùng lúc (MySQL tự xử lý concurrent access).

**Thiết kế Database:**

```sql
CREATE DATABASE tankzors;
USE tankzors;

-- Bảng lưu điểm bảng xếp hạng
CREATE TABLE leaderboard (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) NOT NULL,
    map_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    time_seconds INT NOT NULL,
    hp_remain INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_map_score (map_id, score DESC),
    INDEX idx_player_map (player_name, map_id)
);

-- Bảng lưu tiến trình người chơi
CREATE TABLE player_progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) NOT NULL UNIQUE,
    unlocked_level INT DEFAULT 1,
    total_stars INT DEFAULT 0,
    total_coins INT DEFAULT 0,
    items_json JSON DEFAULT '{}',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Việc cần làm:**
- Thêm dependency vào `pom.xml`:
  - `mysql-connector-j` (MySQL JDBC driver)
  - `com.zaxxer:HikariCP` (connection pool)
- Tạo `DatabaseConfig.java`:
  - Cấu hình kết nối MySQL (host, port, database, user, password)
  - Khởi tạo HikariCP DataSource
  - Đọc cấu hình từ `config.properties` hoặc environment variables
- Tạo `DatabaseManager.java`:
  - Singleton quản lý DataSource
  - `getConnection()`: lấy connection từ pool
  - `initSchema()`: tạo bảng nếu chưa tồn tại (chạy khi khởi động server)
- Tạo `LeaderboardStorage.java`:
  - `saveScore(mapId, playerName, score, time, hpRemain)`: INSERT vào bảng leaderboard
  - `getTopScores(mapId, limit)`: SELECT top N điểm cao nhất, ORDER BY score DESC
  - `getPlayerBest(mapId, playerName)`: SELECT điểm cao nhất của 1 người chơi trên 1 map
  - Giữ tối đa 50 dòng điểm mỗi map (DELETE điểm thấp nhất khi vượt quá)

**Cấu hình kết nối (`config.properties`):**
```properties
db.host=localhost
db.port=3306
db.name=tankzors
db.user=root
db.password=
db.pool.size=5
```

**Tiêu chí hoàn thành:**
- [ ] Server khởi động → kết nối MySQL thành công, tạo bảng nếu chưa có
- [ ] Lưu điểm thành công vào database
- [ ] Restart server → điểm vẫn còn trong MySQL
- [ ] 2 client gửi điểm cùng lúc → không bị lỗi, cả 2 đều lưu được (MySQL xử lý concurrent)
- [ ] `getTopScores("online_1", 10)` trả về đúng 10 điểm cao nhất, đã sắp xếp giảm dần

---

### [3.4] API lấy bảng xếp hạng (top scores)

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Mạng, Backend
**Độ khó:** Dễ

**Mô tả:**
Client có thể yêu cầu bảng xếp hạng từ server bất kỳ lúc nào. Server trả về danh sách top điểm cho từng map hoặc tổng hợp.

**Việc cần làm:**
- Thêm message type:
  ```json
  // Client yêu cầu
  {"type": "GET_LEADERBOARD", "mapId": "online_1"}
  // hoặc lấy tất cả
  {"type": "GET_LEADERBOARD", "mapId": "all"}
  
  // Server trả về
  {
    "type": "LEADERBOARD_DATA",
    "mapId": "online_1",
    "entries": [
      {"rank": 1, "playerName": "Player1", "score": 1700, "time": 45},
      {"rank": 2, "playerName": "Player2", "score": 1200, "time": 60}
    ]
  }
  ```
- Tạo `LeaderboardManager.java`:
  - Nhận request từ MessageHandler
  - Gọi LeaderboardStorage để lấy dữ liệu
  - Gửi kết quả về client qua MessageSender

**Tiêu chí hoàn thành:**
- [ ] Client gửi GET_LEADERBOARD → nhận được danh sách điểm
- [ ] Danh sách sắp xếp giảm dần theo điểm
- [ ] Hiển thị đúng rank (1, 2, 3...)
- [ ] Gửi mapId = "all" → nhận được bảng tổng hợp cả 3 map

---

### [3.5] Trang Bảng xếp hạng (hiển thị từ server)

**Người phụ trách:** C (Frontend)
**Label:** Frontend, UI
**Độ khó:** Trung bình

**Mô tả:**
Tạo trang hiển thị bảng xếp hạng lấy từ server. Người chơi có thể xem top điểm của từng map hoặc tổng hợp. Hiển thị dạng bảng với rank, tên, điểm, thời gian.

**Việc cần làm:**
- Tạo `LeaderboardScreen.jsx`:
  - Tabs chọn map: "Tất cả" | "Map 1" | "Map 2" | "Map 3"
  - Bảng điểm: Rank | Tên người chơi | Điểm | Thời gian
  - Highlight dòng của chính người chơi (nếu có)
  - Nút "Làm mới" để lấy dữ liệu mới nhất
- Tạo `ScoreTable.jsx`:
  - Component bảng có thể tái sử dụng
  - Props: entries (mảng điểm), highlightPlayer (tên người chơi hiện tại)
  - Rank 1-3 có màu vàng/bạc/đồng
- Khi mở trang: tự động gửi GET_LEADERBOARD lên server
- Khi chuyển tab: gửi lại request với mapId tương ứng

**Giao diện:**
```
TẤT CẢ | MAP 1 | MAP 2 | MAP 3
+------+------------------+-------+----------+
| Rank | Tên              | Điểm  | Thời gian|
+------+------------------+-------+----------+
| 1    | ProGamer99       | 2100  | 32s      |
| 2    | TankMaster       | 1850  | 38s      |
| 3    | >> Bạn (Player1) | 1700  | 45s      | ← highlight
| 4    | NoobPlayer       | 900   | 72s      |
+------+------------------+-------+----------+
              [LÀM MỚI]
```

**Tiêu chí hoàn thành:**
- [ ] Mở trang → hiện bảng điểm lấy từ server
- [ ] Chuyển tab → hiện điểm của map tương ứng
- [ ] Bấm "Làm mới" → cập nhật dữ liệu mới
- [ ] Dòng của chính người chơi được highlight (màu khác)
- [ ] Bảng rỗng (chưa có ai chơi) → hiện "Chưa có dữ liệu"

---

### [3.6] Đồng bộ: nhiều client xem cùng bảng xếp hạng

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Mạng, Testing
**Độ khó:** Trung bình

**Mô tả:**
Đảm bảo nhiều client cùng xem bảng xếp hạng thấy cùng dữ liệu. Khi 1 người chơi đạt điểm mới, người khác refresh sẽ thấy được điểm đó. MySQL đảm bảo tính nhất quán dữ liệu (ACID) — không cần tự xử lý file locking như trước.

**Việc cần làm:**
- LeaderboardStorage query trực tiếp từ MySQL mỗi lần có request (luôn trả về dữ liệu mới nhất)
- Tuỳ chọn: cache kết quả trong 3-5 giây để giảm tải DB (dùng ConcurrentHashMap + TTL)
- Test với 2 trình duyệt:
  1. Trình duyệt A chơi và đạt điểm
  2. Trình duyệt B mở bảng xếp hạng → phải thấy điểm của A
- MySQL tự xử lý concurrent INSERT (không cần synchronized/ReentrantLock)

**Tiêu chí hoàn thành:**
- [ ] Trình duyệt A đạt điểm → Trình duyệt B bấm "Làm mới" → thấy điểm của A
- [ ] 2 client gửi điểm cùng thời điểm → cả 2 đều lưu thành công, không mất
- [ ] Không bị lỗi khi nhiều client truy cập đồng thời (HikariCP quản lý connection pool)

---

### [3.7] Kiểm thử tích hợp toàn bộ 2 chế độ

**Người phụ trách:** A + B + C (Cả nhóm)
**Label:** Testing
**Độ khó:** Trung bình

**Mô tả:**
Test toàn bộ game từ đầu đến cuối: Campaign 3 màn + Online 3 map + Bảng xếp hạng. Đảm bảo mọi tính năng hoạt động đúng và không có lỗi.

**Danh sách kiểm thử:**

**Campaign:**
- [ ] Mở game → chỉ Màn 1 chơi được, Màn 2 và 3 bị khoá
- [ ] Chơi Màn 1, thắng → nhận 3 sao + tiền, Màn 2 mở khoá
- [ ] Chơi Màn 2, thắng → Màn 3 mở khoá
- [ ] Chơi Màn 3, thắng → hiện chúc mừng hoàn thành Campaign
- [ ] Chơi Màn 2, thua → bấm "Thử lại" → chơi lại Màn 2
- [ ] Vật phẩm từ Màn 1 còn khi chơi Màn 2
- [ ] Restart server → tiến trình vẫn còn

**Online:**
- [ ] Tất cả 3 map chọn được ngay (không khoá)
- [ ] Chơi Map 1 xong → điểm hiện trên màn hình kết quả
- [ ] Quay lại màn hình chọn map → điểm cao cập nhật
- [ ] Chơi Map 2 → điểm lưu đúng (không ghi đè điểm Map 1)

**Bảng xếp hạng:**
- [ ] Mở bảng xếp hạng → hiện dữ liệu từ server
- [ ] Chơi xong 1 trận → bấm Làm mới → điểm mới xuất hiện
- [ ] Mở 2 trình duyệt, cả 2 thấy cùng bảng xếp hạng

**Tiêu chí hoàn thành:**
- [ ] Tất cả mục trên đều PASS
- [ ] Không có lỗi crash trong quá trình test
- [ ] Game chơi mượt, không lag
- [ ] WebSocket kết nối ổn định suốt quá trình chơi

---

## TASK BỔ SUNG (XUYÊN SUỐT)

---

### [X.1] Viết README (mô tả, hướng dẫn chạy, kế hoạch v2)

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Tài liệu
**Độ khó:** Dễ

**Mô tả:**
Viết file README.md cho project. Gồm: giới thiệu game, công nghệ sử dụng, hướng dẫn cài đặt và chạy, ảnh chụp màn hình, kế hoạch phát triển tương lai.

**Nội dung README:**
```
1. Giới thiệu game (Tankzors Pro là gì, 2 chế độ chơi)
2. Công nghệ sử dụng (Java, React, WebSocket, MySQL, SOLID)
3. Cấu trúc thư mục (sơ đồ ngắn gọn)
4. Hướng dẫn cài đặt:
   - Yêu cầu: Java 17+, Node 18+, Maven, MySQL 8.0+
   - Clone repo
   - Tạo database: mysql -u root -e "CREATE DATABASE tankzors"
   - Chạy server: cd server && mvn exec:java
   - Chạy client: cd client && npm install && npm run dev
   - Mở trình duyệt: http://localhost:5173
5. Ảnh chụp màn hình gameplay
6. SOLID Principles (giải thích ngắn cách áp dụng)
7. Kế hoạch phát triển v2 (thêm màn, thêm tank, PvP...)
8. Thông tin nhóm (tên, MSSV)
```

**Tiêu chí hoàn thành:**
- [ ] README đầy đủ 8 mục
- [ ] Đọc README → biết cách chạy project trong 5 phút
- [ ] Có ít nhất 2-3 ảnh chụp màn hình

---

### [X.2] Vẽ sơ đồ kiến trúc SOLID (nộp báo cáo)

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Tài liệu, SOLID
**Độ khó:** Trung bình

**Mô tả:**
Vẽ sơ đồ lớp (class diagram) thể hiện kiến trúc SOLID của Java backend. Chỉ rõ từng nguyên tắc được áp dụng ở đâu. Dùng để nộp báo cáo môn học.

**Nội dung:**
- Sơ đồ lớp chính: GameEngine, Tank, Bullet, Map, AI, Network
- Chỉ rõ từng nguyên tắc SOLID:
  - S: "GameEngine chỉ quản lý game loop, NetworkHandler chỉ quản lý kết nối"
  - O: "Thêm loại vũ khí mới bằng cách tạo class mới implement IWeapon, không sửa GameEngine"
  - L: "PlayerTank và AITank đều thay thế được Tank ở bất kỳ đâu trong GameEngine"
  - I: "ITank chỉ có method liên quan đến tank, không ép client implement AI logic"
  - D: "GameEngine phụ thuộc vào ITank/IBullet/IMap, không phụ thuộc PlayerTank/AITank"
- Vẽ bằng công cụ: draw.io, PlantUML, hoặc Lucidchart

**Tiêu chí hoàn thành:**
- [ ] Sơ đồ lớp rõ ràng, đọc được
- [ ] 5 nguyên tắc SOLID đều được chỉ ra cụ thể trong sơ đồ
- [ ] File export PNG/PDF để dán vào báo cáo

---

### [X.3] Viết báo cáo phần Lập trình mạng (WebSocket protocol)

**Người phụ trách:** A (Trưởng nhóm)
**Label:** Tài liệu, Mạng
**Độ khó:** Trung bình

**Mô tả:**
Viết phần báo cáo giải thích cách game sử dụng lập trình mạng. Mô tả giao thức WebSocket, các loại message, quy trình giao tiếp client-server.

**Nội dung:**
```
1. Tổng quan kiến trúc Client-Server
   - Client (React) ↔ WebSocket ↔ Server (Java)
   - Server-authoritative: server là "nguồn sự thật duy nhất"

2. Giao thức WebSocket
   - Vì sao chọn WebSocket thay vì HTTP polling
   - Full-duplex, low latency, phù hợp game real-time

3. Các loại Message
   - Client → Server: INPUT (MOVE, SHOOT, STOP)
   - Server → Client: STATE (trạng thái game mỗi tick)
   - Server → Client: RESULT (kết quả trận đấu)
   - Client ↔ Server: LEADERBOARD (yêu cầu/trả về bảng xếp hạng)

4. Quy trình giao tiếp
   - Sơ đồ sequence: kết nối → gửi input → nhận state → kết thúc
   - Xử lý mất kết nối: retry, timeout

5. Đồng bộ dữ liệu
   - Bảng xếp hạng lưu MySQL, nhiều client đọc cùng lúc
   - Connection pool (HikariCP) quản lý kết nối hiệu quả
```

**Tiêu chí hoàn thành:**
- [ ] Báo cáo đầy đủ 5 phần
- [ ] Có sơ đồ (sequence diagram hoặc flow chart)
- [ ] Giải thích được tại sao chọn WebSocket
- [ ] Code minh hoạ cho mỗi loại message
# LEGACY NOTICE

Tài liệu task cũ này được giữ để tham chiếu lịch sử. Stack và thứ tự triển khai mới nằm trong `ROADMAP.md`, `REFACTOR-PLAN.md` và `FEATURE-MATRIX.md`. Không dùng các task còn ghi React/Javalin/MySQL làm nguồn kế hoạch mới; chúng sẽ được thay thế dần bằng Spring Boot + Thymeleaf + LibGDX HTML5 + WebSocket + PostgreSQL.
