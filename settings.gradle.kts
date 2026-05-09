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

rootProject.name = "2Com"

include(":app")
include(":core:crypto")
include(":core:transport")
include(":core:database")
include(":core:common")
include(":feature:onboarding")
include(":feature:chat")
include(":feature:contacts")
