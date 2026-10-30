# 03. Vòng lặp game: mỗi khung hình chạy gì

**Tóm tắt:** Trình duyệt gọi `GameClient.render()` theo tần số màn hình, thường khoảng 60 lần mỗi giây. Mỗi lần gọi, game đọc thời gian đã trôi qua, chạy một số bước mô phỏng cố định 1/30 giây, chụp ảnh trạng thái rồi vẽ. Phím bấm không tác động thẳng vào xe mà được xếp vào hàng đợi lệnh. Toàn bộ phần này chạy trong trình duyệt, không có lời gọi mạng.

**File chính:** [GameClient.java](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java), [ClientInputProcessor.java](../../game-client/core/src/main/java/com/tankzors/client/input/ClientInputProcessor.java), [LocalCampaignSession.java](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java), [GameSimulation.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java), [WorldSnapshotMapper.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/WorldSnapshotMapper.java), [GameRenderer.java](../../game-client/core/src/main/java/com/tankzors/client/render/GameRenderer.java)

## Sơ đồ

```text
Phím W (bất kỳ lúc nào)
  └─ keyDown()                           ClientInputProcessor.java:146
       └─ onMove()                       LocalCampaignSession.java:243
            └─ enqueueMove()             GameSimulation.java:155      → xếp vào commandQueue

Mỗi khung hình:
render()                                 GameClient.java:289
  ├─ update()                            LocalCampaignSession.java:277
  │    └─ advance()                      GameSimulation.java:866      chia thời gian thành các step
  │         └─ step()                    GameSimulation.java:894      lặp 0 đến 5 lần
  │              ├─ processCommands()    GameSimulation.java:914      áp lệnh trong hàng đợi
  │              ├─ updateEntities()     GameSimulation.java:1096     xe, nhiệm vụ, AI, đạn
  │              ├─ checkCollisions()    GameSimulation.java:1247     đạn trúng tường, trúng xe
  │              └─ cleanupDeadTanks()   GameSimulation.java:1447     dọn xe chết, xét thắng/thua
  ├─ snapshot()                          LocalCampaignSession.java:289  chụp trạng thái
  └─ render()                            GameRenderer.java:111        vẽ theo ảnh chụp
```

## Bước 1: Phím bấm thành lệnh

`keyDown` xét các màn hình đang mở trước (cửa hàng, kết quả, radar, túi đồ), rồi mới tới phím di chuyển:

[ClientInputProcessor.java:260-269](../../game-client/core/src/main/java/com/tankzors/client/input/ClientInputProcessor.java#L260-L269)
```java
Direction direction = mapKeyToDirection(keycode);
if (direction != null) {
    if (!movementKeys.contains(keycode)) {
        Direction previous = latestDirection();
        movementKeys.add(keycode);
        activeDirections.add(direction);
        if (previous != direction) callbacks.onMove(direction, true);
    }
    return true;
}
```

`callbacks` là các hàm khai báo trong [GameClient.java:151](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java#L151), chuyển tiếp vào phiên chơi:

[LocalCampaignSession.java:243-246](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L243-L246)
```java
@Override public void onMove(Direction direction, boolean moving) {
    if (phase != Phase.PLAYING || inventoryMenuOpen || isRadarOpen()) return;
    sim.enqueueMove(toGameplayDirection(direction), moving);
}
```

**Vì sao cần hàng đợi?** Phím có thể đến giữa hai bước mô phỏng. Xếp vào hàng đợi rồi xử lý ở đầu step giúp mọi thay đổi trạng thái chỉ xảy ra trong step, nhờ đó mô phỏng ổn định và test được.

Space giữ để bắn liên tục được xử lý ở [`ClientInputProcessor.update`](../../game-client/core/src/main/java/com/tankzors/client/input/ClientInputProcessor.java#L132-L143), gửi lệnh bắn lặp lại. Tốc độ bắn thật do thời gian hồi của xe quyết định.

## Bước 2: Mỗi khung hình

[GameClient.java:289-303](../../game-client/core/src/main/java/com/tankzors/client/GameClient.java#L289-L303)
```java
public void render() {
    float dt = Gdx.graphics.getDeltaTime();

    if (input != null) input.update(dt);

    if (session != null) {
        session.update(dt);
        setEnabledByStatus(session.status());
        if (renderer != null) {
            com.tankzors.client.model.GameStateSnapshot frame = session.snapshot();
            frame.radarMenuSelection = input.radarMenuSelection();
            frame.radarSettingsOpen = input.radarSettingsOpen();
            renderer.render(frame);
            renderer.renderShop(shopOverlay);
        }
```

Mô phỏng bị tạm dừng khi đang mở túi đồ, cửa hàng hoặc briefing:

[LocalCampaignSession.java:277-278](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L277-L278)
```java
@Override public void update(float rawDeltaTime) {
    if (phase == Phase.PLAYING && !inventoryMenuOpen && !shopOpen && !briefingOpen) sim.advance(rawDeltaTime);
```

## Bước 3: Chia thời gian thành các bước cố định

Màn hình có thể chạy 60 hay 144 Hz, nhưng mô phỏng luôn chạy đúng 30 bước mỗi giây. Thời gian trôi qua được cộng vào `accumulator`, đủ 1/30 giây thì chạy một step:

[GameSimulation.java:866-880](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L866-L880)
```java
public void advance(float rawDeltaTime) {
    if (radar.isOpen()) { radar.update(rawDeltaTime); return; }
    double dt = Math.min(rawDeltaTime, GameplayConfig.MAX_FRAME_DELTA);
    accumulator += dt;
    int steps = 0;
    while (accumulator >= GameplayConfig.STEP_SECONDS && steps < GameplayConfig.MAX_STEPS_PER_FRAME) {
        step(GameplayConfig.STEP_SECONDS);
        accumulator -= GameplayConfig.STEP_SECONDS;
        steps++;
    }
    // Bỏ backlog dư có chủ đích: nếu còn dư > 1 step, reset để không chạy bù vô hạn.
    if (accumulator >= GameplayConfig.STEP_SECONDS) {
        accumulator = 0;
    }
}
```

Các hằng số nằm ở [GameplayConfig.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameplayConfig.java): 30 Hz, nhận tối đa 0,25 giây mỗi khung hình, tối đa 5 step mỗi khung hình. Các giới hạn này tránh game "chạy bù" dồn dập khi tab bị treo. Phần thời gian dư được dùng để [nội suy vị trí khi vẽ](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L888-L891), nhờ đó hình mượt dù mô phỏng chỉ 30 Hz.

## Bước 4: Một step làm gì

[GameSimulation.java:894-912](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L894-L912)
```java
public void step(double dt) {
    if (!Double.isFinite(dt) || dt <= 0) return;
    if (radar.isOpen()) { radar.update(dt); return; }
    for (ITank tank : world.getAllTanks()) {
        if (tank instanceof Tank) ((Tank) tank).capturePreviousPosition();
    }
    processCommands();
    if (world.isPlaying()) {
        // Tích lũy simulation time chỉ khi đang chơi (pause không tăng).
        matchElapsedSeconds += dt;
        updateEntities(dt);
        if (!checkCaptureFlagGoal()) {
            checkCollisions();
            cleanupDeadTanks();
        }
    }
    // Terminal: không cập nhật physics nữa. Renderer vẫn vẽ state cuối.
    world.incrementTick();
}
```

`updateEntities` chạy theo thứ tự cố định ([GameSimulation.java:1096-1145](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1096-L1145)): hiệu ứng theo thời gian → xe người chơi → nhặt vật phẩm → nhiệm vụ và sinh xe theo đợt → AI → đạn → dọn đạn hết hạn.

### Xe di chuyển

Xe đi theo từng ô. Trước khi bước sang ô mới, [`beginTileMovement`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/entity/PlayerTank.java#L137-L160) kiểm tra ô đích có tường hay có xe khác không:

[PlayerTank.java:153-159](../../game-client/core/src/main/java/com/tankzors/client/gameplay/entity/PlayerTank.java#L153-L159)
```java
if (Collision.checkTankWallCollision(this, map, new Position(tileTargetX, tileTargetY))) return false;
int ts = map.getTileSize();
if (blocker != null && blocker.isBlocked(this,
        (int) Math.floor(tileTargetX / ts), (int) Math.floor(tileTargetY / ts))) return false;
movingDirection = next;
remainingTileDistance = map.getTileSize();
return true;
```

`blocker` chính là [`isTileOccupiedByOtherTank`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L370-L385): một ô bị chiếm nếu có xe đứng đó hoặc xe khác đã giữ chỗ để đi tới.

### AI

Mỗi xe AI gọi [`AITank.update`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/entity/AITank.java#L113) → [`AIController.update`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/ai/AIController.java#L33-L43) → tháp pháo dùng `ShootBehavior`, xe tăng dùng `BuildsBotBrain`. Phần mô tả đầu [BuildsBotBrain.java](../../game-client/core/src/main/java/com/tankzors/client/gameplay/ai/BuildsBotBrain.java#L20-L33) tóm tắt hành vi: quan sát theo 4 tia thẳng, tuần tra, tìm đường Dijkstra khi địch ở gần, phản xạ khi bị bắn.

## Bước 5: Xét thắng và thua

Xét ở hai nơi.

**Thắng do cướp cờ** (bản đồ có ô cờ): [`checkCaptureFlagGoal`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1549-L1561) kiểm tra xe người chơi đứng trên ô cờ.

**Thắng hoặc thua do xe bị hạ:** ở cuối [`cleanupDeadTanks`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1447-L1511):

[GameSimulation.java:1489-1510](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1489-L1510)
```java
boolean hasPlayer = false, hasAI = false;
boolean turretsCount = mission == null || mission.getObjective() == MissionConfig.Objective.DESTROY_ALL;
for (ITank t : world.getAllTanks()) {
    if (!t.isAlive()) continue;
    if (t instanceof PlayerTank) hasPlayer = true;
    if (isEnemySide(t) && (turretsCount || !((AITank) t).isTurret())) hasAI = true;
}
// Địch còn chờ ở base cũng tính là chưa hạ xong.
if (enemySpawner != null && enemySpawner.pending() > 0) hasAI = true;

// Tiebreak: player chết ưu tiên DEFEAT (cùng tick).
if (!hasPlayer) {
    world.setStatus(GameWorld.Status.DEFEAT);
    fireMatchEnd(GameWorld.Status.DEFEAT, 0);
} else if (!hasAI) {
    world.setStatus(GameWorld.Status.VICTORY);
    int hpRemain = 0;
    for (ITank t : world.getAllTanks()) {
        if (t instanceof PlayerTank) { hpRemain = t.getHP(); break; }
    }
    fireMatchEnd(GameWorld.Status.VICTORY, hpRemain);
}
```

**Mạng sống:** mỗi màn có [4 mạng](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameplayConfig.java#L20). Khi xe người chơi bị hạ, [`respawnCampaignPlayer`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1514-L1536) trừ một mạng, mất giáp tạm và khiên, dùng vật phẩm Mạng bổ sung nếu đã hết mạng thường, rồi hồi sinh ở điểm xuất phát. Hết hẳn mạng thì [thua](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1449-L1455).

`fireMatchEnd` có cờ để [chỉ chạy đúng một lần](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1577-L1592), rồi gọi callback đã đăng ký ở bước vào trận. Từ đây chuyển sang [04](04-luu-ket-qua-tran.md).

## Bước 6: Chụp ảnh và vẽ

[`LocalCampaignSession.snapshot`](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L289-L363) gọi [`WorldSnapshotMapper.snapshot`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/WorldSnapshotMapper.java#L34-L35) để sao dữ liệu thế giới sang `GameStateSnapshot`, rồi thêm thông tin HUD, ví, túi đồ.

[`GameRenderer.render`](../../game-client/core/src/main/java/com/tankzors/client/render/GameRenderer.java#L111-L200) chỉ đọc ảnh chụp này và vẽ theo thứ tự: bản đồ → vết xích → xe → đạn → hiệu ứng → thanh máu → HUD → menu và màn hình kết quả.

Đây là quy tắc kiến trúc của dự án: **renderer không sửa thế giới**. Muốn đổi luật chơi thì sửa `gameplay/`, không sửa `render/`.

## Dữ liệu thay đổi

Không có. Toàn bộ trạng thái trận nằm trong bộ nhớ trình duyệt. Tải lại trang thì mất lượt đang chơi.

## Test kiểm chứng

- [GameSimulationTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/GameSimulationTest.java): chạy mô phỏng không cần đồ hoạ
- [PlayerTankMovementTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/entity/PlayerTankMovementTest.java): di chuyển theo ô, không đi xuyên xe
- [BuildsBotBrainTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/ai/BuildsBotBrainTest.java): hành vi AI
- [CollisionTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/collision/CollisionTest.java): va chạm
- [CampaignLivesTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/CampaignLivesTest.java): 4 mạng, mạng bổ sung
- [ClientInputProcessorTest.java](../../game-client/core/src/test/java/com/tankzors/client/input/ClientInputProcessorTest.java): xử lý phím
- [WorldSnapshotInterpolationTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/WorldSnapshotInterpolationTest.java): nội suy khi vẽ
