pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SignKorean"

include(":app")
include(":domain")
include(":data")
include(":platform")
include(":designsystem")
include(":feature-onboarding")
include(":feature-learn")
include(":feature-progress")
include(":feature-settings")
include(":test-support")
