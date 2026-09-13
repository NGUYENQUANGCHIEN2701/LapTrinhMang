// game-client/html/build.gradle.kts
// HTML/GWT backend module. Phase 1 không enable GWT Plugin -
// chỉ khai báo dependency. Phase 3 sẽ setup war + dev mode.

dependencies {
    implementation(project(":core"))
    // Phase 3:
    // implementation("com.badlogicgames.gdx:gdx-backend-gwt:1.13.1")
    // implementation("com.badlogicgames.gdx:gdx:1.13.1:sources")
    // implementation("com.badlogicgames.gdx:gdx-backend-gwt:1.13.1:sources")
}
