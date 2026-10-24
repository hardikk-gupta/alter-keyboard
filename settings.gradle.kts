pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // FUTO's prebuilt AARs (MOZC, RIME, futo-swipe, VAD).
    flatDir { dirs("app/libs") }
  }
}

rootProject.name = "ALTER Keyboard"

// app: FUTO Keyboard with ALTER's changes. alter-voice: ALTER's voice pack. voiceinput-shared: FUTO's voice module,
// still needed by FUTO code.
include(":app")
include(":alter-voice")
include(":voiceinput-shared")

include(":alter-agent")
