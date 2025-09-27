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
        // CRITICAL FIX: Add JitPack repository to resolve 'glide-integration' dependency
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "LonyiTrade"
include(":app")
