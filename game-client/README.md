# game-client Module - README

LibGDX target client cho migration Phase 3+. Phase 1 chỉ là skeleton.

## Mục đích

- Phase 1: tạo cấu trúc Gradle multi-project chuẩn LibGDX. Các file
  source chỉ là placeholder để build pass. Không render gì.
- Phase 3: migrate `Renderer.ts` / `GameLoop.ts` / `InputHandler.ts` /
  `WebSocketClient.ts` từ React Canvas sang LibGDX `core`/`html`.
- Phase 4: nhúng output HTML5/GWT vào trang play Thymeleaf.

## Cấu trúc sub-projects

```
game-client/
├── build.gradle.kts              # root build
├── settings.gradle.kts           # include core/desktop/html
├── gradle/wrapper/
│   └── gradle-wrapper.properties # Gradle 8.10.1 (chưa commit binary)
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/com/tankzors/client/
│       └── GameClient.java       # placeholder
├── desktop/
│   ├── build.gradle.kts
│   └── src/main/java/com/tankzors/client/desktop/
│       └── DesktopLauncher.java  # placeholder
└── html/
    ├── build.gradle.kts
    └── src/main/java/com/tankzors/client/html/
        └── HtmlLauncher.java     # placeholder
```

## Build (sau khi cài Gradle)

```bash
cd game-client
gradle :core:compileJava         # Verify skeleton compile
gradle :desktop:compileJava
gradle :html:compileJava
# hoặc:
gradle build                     # build tất cả sub-projects
```

> Phase 1 chưa chạy được `:desktop:run` vì chưa enable Gradle
> `application` plugin và chưa có LibGDX dependency. Mục tiêu Phase 1
> chỉ là structure đúng, không phải runtime.

## Phase 3 kế hoạch

1. Uncomment dependency `com.badlogicgames.gdx:gdx:1.13.1` trong 3
   `build.gradle.kts`.
2. Implement `GameClient implements ApplicationListener` trong `core`:
   - `create()` -> load sprites qua AssetManager (tương đương
     `SpriteManager.ts`).
   - `render()` -> SpriteBatch draw map/tank/bullet từ local snapshot
     model (tương đương `Renderer.ts`).
3. Implement `ClientInputProcessor implements InputProcessor` để map
   WASD/Arrow/Space thành `MOVE_INPUT` / `SHOOT_INPUT` packet.
4. Implement `WebSocketBridge` (hiện `core` chỉ compile được, GWT
   reflection hạn chế nên cần special-case cho `html` module).
5. Desktop backend (`desktop/DesktopLauncher`) chạy được 1 màn để dev
   nhanh. HTML backend (`html/HtmlLauncher`) build war, nhúng vào
   `web/templates/play.html` (Phase 4).

## Lưu ý quan trọng về shared-protocol + GWT

GWT không support Gson reflection fully. Phase 3 có 2 option:

1. **JSON parsing thủ công trong `core`** (không dùng Gson) → single
   source code cả desktop lẫn HTML. Khó hơn nhưng fewer runtime deps.
2. **REST-style serialiser riêng cho HTML module** (giữ shared-protocol
   cho desktop). Code trùng lặp.

Quyết định chốt tại Phase 3 sau khi thử cả 2 cách với 1 packet đầu tiên.
