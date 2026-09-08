pluginManagement {
    includeBuild("build-logic")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "CieloTickets"

include(":app")

include(":core:core-common")
include(":core:core-local-storage")
include(":core:core-payment-cielo")
include(":core:core-designsystem")
include(":core:core-network")

include(":feature:feature-home")
include(":feature:feature-ticket-selection")
include(":feature:feature-payment")
include(":feature:feature-receipt")
include(":feature:feature-history")
