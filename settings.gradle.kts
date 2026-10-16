pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Vosk 离线语音识别库托管在 JitPack
        maven("https://jitpack.io")
    }
}

rootProject.name = "ParkingLogger"
include(":app")
