// game-client/desktop/build.gradle.kts
// Desktop launcher module (LWJGL3). Phase 1 không enable Gradle
// Application Plugin -x chỉ khai báo dependency. Phase 3 sẽ build .jar.

dependencies {
    implementation(project(":core"))
    // Phase 3:
    // implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.1")
    // implementation("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-desktop")
}
