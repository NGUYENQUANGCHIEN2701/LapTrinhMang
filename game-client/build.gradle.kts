// game-client/build.gradle.kts - root multi-project build cho LibGDX Phase 1
//
// Phase 1 chỉ tạo skeleton Gradle multi-project chuẩn LibGDX:
//   - core   : shared Java game logic + render (chỉ interface trống)
//   - desktop: LWJGL3 launcher (chưa chạy được, README trước)
//   - html   : GWT backend (build script, chưa generate war)
//
// Phase 3 sẽ:
//   - Thêm dependency vào shared-protocol (~gwt-incompatible nếu Gson, sẽ
//     cần proguard hoặc DTO riêng cho client).
//   - Thêm GameRenderer + InputProcessor + WebSocket bridge tương đương
//     Renderer.ts/InputHandler.ts/WebSocketClient.ts của React.

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

allprojects {
    group = "com.tankzors"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
