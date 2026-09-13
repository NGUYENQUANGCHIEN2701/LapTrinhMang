// game-client/settings.gradle.kts
rootProject.name = "tankzors-game-client"

include(":core")
include(":desktop")
include(":html")

project(":core").projectDir = file("core")
project(":desktop").projectDir = file("desktop")
project(":html").projectDir = file("html")
