# 06. Cửa hàng, túi đồ và đồng bộ ví

**Tóm tắt:** Ví (Xu, Sao), túi đồ và cây nghiên cứu nằm trong đối tượng `PlayerShopState` ở client. Mọi phép mua, bán, nghiên cứu đều tính ở client, rồi toàn bộ trạng thái được gửi lên `POST /api/game/shop/sync` để lưu. Server chỉ ghi lại, không tính giá. Bootstrap trả dữ liệu này về khi tải lại game.

**File chính:** [PlayerShopState.java](../../game-client/core/src/main/java/com/tankzors/client/shop/PlayerShopState.java), [ShopOverlay.java](../../game-client/core/src/main/java/com/tankzors/client/ui/ShopOverlay.java), [ShopCatalog.java](../../game-client/core/src/main/java/com/tankzors/client/ui/ShopCatalog.java), [ShopItem.java](../../game-client/core/src/main/java/com/tankzors/client/shop/ShopItem.java), [ResearchTech.java](../../game-client/core/src/main/java/com/tankzors/client/shop/ResearchTech.java), [PlayerShopService.java](../../server-spring/src/main/java/com/tankzors/application/PlayerShopService.java)

**Dễ nhầm:** có hai class cùng tên `PlayerShopState`. [Bản ở client](../../game-client/core/src/main/java/com/tankzors/client/shop/PlayerShopState.java) chứa logic mua bán. [Bản ở server](../../server-spring/src/main/java/com/tankzors/progress/PlayerShopState.java) chỉ là entity của bảng `player_shop_state`. Trong tài liệu này, `PlayerShopState` là bản ở client.

## Sơ đồ

```text
NẠP (lúc vào game)
  bootstrap trả totalCoins, totalStars, researchJson, inventoryJson
  └─ loadState()                         PlayerShopState.java:411

MỞ CỬA HÀNG (phím B)
  └─ onToggleShop()                      GameClient.java:216
       canOpenShop()?                    LocalCampaignSession.java:668
       └─ toggle()                       ShopOverlay.java:75
            show() → setShopOpen(true) → mô phỏng dừng

MUA (phím →)
  └─ change()                            ShopOverlay.java:111
       ├─ buy()                          PlayerShopState.java:311      trừ Xu, cộng đồ
       ├─ onWorkshopItemBought()         LocalCampaignSession.java:675 áp lên xe đang chạy
       └─ changed() → onShopStateChanged() → syncShopState()

ĐỒNG BỘ
  └─ syncShopState()                     LocalCampaignSession.java:105
       └─ syncShop()                     GwtAppBridge.java:29          POST /api/game/shop/sync
            └─ syncShop()                GameApiController.java:133
                 └─ syncShopData()       PlayerShopService.java:47     ghi đè DB

DÙNG VẬT PHẨM (phím Q)
  └─ openInventoryMenu()                 LocalCampaignSession.java:367
       chọn → confirmInventorySelection() LocalCampaignSession.java:405
            └─ useInventoryItem()        trong GameSimulation
```

## Bước 1: Khi nào được mở cửa hàng

[LocalCampaignSession.java:668-673](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L668-L673)
```java
@Override public boolean canOpenShop() {
    if (phase != Phase.PLAYING || sim == null || briefingOpen) return false;
    // Cửa hàng giữa hai màn vẫn mở từ báo cáo chiến thắng.
    return sim.getWorld().getStatus() == GameWorld.Status.VICTORY
            || sim.isLocalPlayerAtSpawnBase();
}
```

Trong trận, xe phải đứng trên ô hồi sinh màu xanh lá. Tâm xe nằm trong ô `SPAWN_PLAYER` mới được tính:

[GameSimulation.java:144-152](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L144-L152)
```java
public boolean isLocalPlayerAtSpawnBase() {
    IMap map = world.getCurrentMap();
    ITank player = world.getTank(localPlayerId);
    if (!world.isPlaying() || map == null || player == null || !player.isAlive()) return false;
    int col = cellOf(player.getPosition().getX(), map.getTileSize());
    int row = cellOf(player.getPosition().getY(), map.getTileSize());
    return col >= 0 && row >= 0 && col < map.getWidth() && row < map.getHeight()
            && map.getTileAt(col, row) == TileType.SPAWN_PLAYER;
}
```

Cửa hàng mở thì mô phỏng dừng, vì `update` kiểm tra [`shopOpen`](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L278).

## Bước 2: Mua và bán

Khi cửa hàng mở, [`ClientInputProcessor`](../../game-client/core/src/main/java/com/tankzors/client/input/ClientInputProcessor.java#L148-L150) chuyển mọi phím cho `ShopOverlay`. Phím → mua, ← bán, Enter xem mô tả.

[ShopOverlay.java:111-125](../../game-client/core/src/main/java/com/tankzors/client/ui/ShopOverlay.java#L111-L125)
```java
private void change(ShopCatalog.Row row,boolean buy) {
    if(row.kind==ShopCatalog.Kind.ITEM) {
        if(!buy && !sellEnabled(row)) return;
        if(buy ? state.buy(row.item):state.sell(row.item)) {
            if(listener!=null) { if(buy) listener.onItemBought(row.item); else listener.onItemSold(row.item); }
            changed();
        }
    } else if(buy && row.kind==ShopCatalog.Kind.RESEARCH) {
        if(state.upgradeResearch(row.tech)) changed();
    } else if(buy && listener!=null && row.kind==ShopCatalog.Kind.REPAIR_TANK) {
        if(listener.repairTank()) changed();
    } else if(buy && listener!=null && row.kind==ShopCatalog.Kind.REPAIR_ALLIES) {
        if(listener.repairAllies()) changed();
    }
}
```

Giá và điều kiện nằm trong `PlayerShopState`, dữ liệu vật phẩm nằm trong [ShopItem.java](../../game-client/core/src/main/java/com/tankzors/client/shop/ShopItem.java).

[PlayerShopState.java:350-357](../../game-client/core/src/main/java/com/tankzors/client/shop/PlayerShopState.java#L350-L357) nâng nghiên cứu, trả bằng Sao:

```java
public boolean upgradeResearch(ResearchTech tech) {
    if (!canUpgradeResearch(tech)) return false;
    int currentLevel = getResearchLevel(tech.getId());
    int price = tech.getPriceForLevel(currentLevel);
    stars -= price;
    research.put(tech.getId(), currentLevel + 1);
    return true;
}
```

Giáp mua giữa trận có tác dụng ngay qua [`applyWorkshopPurchase`](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1684-L1694).

## Bước 3: Đồng bộ lên server

Mỗi thay đổi đều gọi `syncShopState`, gửi **toàn bộ** ví và túi đồ:

[LocalCampaignSession.java:105-110](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L105-L110)
```java
public void syncShopState() {
    if (shopSyncHandler != null) {
        shopSyncHandler.onSyncShop(shopState.getCoins(), shopState.getStars(),
                shopState.serializeResearch(), shopState.serializeInventory());
    }
}
```

`shopSyncHandler` được gắn ở `HtmlLauncher`, dẫn tới `GwtAppBridge.syncShop`. Hàm này dùng chung `postJsonWithRetry` với kết quả trận ([04](04-luu-ket-qua-tran.md)).

Tên trường JSON phải khớp với record trên server, lệch tên thì server bỏ qua giá trị:

[GwtAppBridge.java:36-39](../../game-client/html/src/main/java/com/tankzors/client/html/GwtAppBridge.java#L36-L39)
```java
sb.append("\"coins\":").append(coins).append(',');
sb.append("\"stars\":").append(stars).append(',');
sb.append("\"researchJson\":").append(quote(researchJson)).append(',');
sb.append("\"inventoryJson\":").append(quote(inventoryJson));
```

Server ghi đè:

[PlayerShopService.java:53-69](../../server-spring/src/main/java/com/tankzors/application/PlayerShopService.java#L53-L69)
```java
if (req.coins != null && req.coins >= 0) {
    totals.setTotalCoins(req.coins);
}
if (req.stars != null && req.stars >= 0) {
    totals.setTotalStars(req.stars);
}
totals.setUpdatedAt(Instant.now());
totalsRepo.save(totals);

if (req.researchJson != null) {
    state.setResearchJson(req.researchJson);
}
if (req.inventoryJson != null) {
    state.setInventoryJson(req.inventoryJson);
}
state.setUpdatedAt(Instant.now());
shopStateRepo.save(state);
```

## Bước 4: Dùng vật phẩm trong trận

Phím Q mở menu túi đồ, mô phỏng tạm dừng. [`confirmInventorySelection`](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L405-L420) xét loại vật phẩm: vũ khí thì đổi vũ khí đang dùng, còn lại gọi `sim.useInventoryItem`.

Khi đồ bị tiêu hao trong trận, mô phỏng đặt cờ `shopStateDirty`, và session đồng bộ ở khung hình kế tiếp:

[LocalCampaignSession.java:279-286](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java#L279-L286)
```java
if (sim != null && sim.consumeShopStateDirty()) {
    // Consumables and installed Force Field state are account progress;
    // a restart must not recreate anything already spent or destroyed.
    if (levelStartInventory != null) {
        levelStartInventory = shopState.snapshotInventory();
    }
    syncShopState();
}
```

Chọn vật phẩm Radar sẽ [mở bản đồ tổng quan](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L1664-L1671), `RadarOverlay` vẽ, và mô phỏng [đứng yên trong lúc radar mở](../../game-client/core/src/main/java/com/tankzors/client/gameplay/GameSimulation.java#L867).

## Dữ liệu thay đổi

| Bảng | Cột | Khi nào |
|---|---|---|
| `player_totals` | `total_coins`, `total_stars` | Mỗi lần sync, và cộng thêm khi lưu trận thắng |
| `player_shop_state` | `research_json`, `inventory_json` | Mỗi lần sync |

## Hạn chế cần biết

- **Server tin toàn bộ số client gửi.** `syncShopData` nhận mọi giá trị không âm, không kiểm tra giá hay lịch sử. Ai gọi thẳng API đều đặt được ví tuỳ ý, và điều này làm mất tác dụng của việc kẹp thưởng ở [04](04-luu-ket-qua-tran.md).
- **Ví thực tế do client quyết định.** Client tự cộng thưởng rồi sync trước khi gửi kết quả. Sau mỗi lần sync, số trên server bằng số trong ví client.
- **Gửi lại muộn có thể ghi đè số mới hơn**, vì sync là ghi đè toàn bộ (phân tích từ code, chưa kiểm chứng).

Hướng sửa: chuyển giao dịch sang server, client chỉ gửi "mua món X", server tự tính giá và trừ tiền.

## Test kiểm chứng

- [PlayerShopStateTest.java](../../game-client/core/src/test/java/com/tankzors/client/shop/PlayerShopStateTest.java): mua bán, nghiên cứu, lưu và nạp lại
- [WorkshopBaseAccessTest.java](../../game-client/core/src/test/java/com/tankzors/client/gameplay/WorkshopBaseAccessTest.java): chỉ ô hồi sinh xanh mới mở được
- [ShopCatalogTest.java](../../game-client/core/src/test/java/com/tankzors/client/ui/ShopCatalogTest.java): danh mục cửa hàng
- [LocalCampaignSessionTest.java](../../game-client/core/src/test/java/com/tankzors/client/session/LocalCampaignSessionTest.java): `shopOpensAtGreenBaseAndPausesButCannotOpenAfterLeavingBase`, `bootstrapLoadsWalletForTheAuthenticatedAccount`
