plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.coreLocalStorage)
    implementation(libs.kotlinx.serialization.json)
}
