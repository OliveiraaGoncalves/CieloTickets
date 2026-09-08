plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
}

dependencies {
    implementation(projects.core.coreLocalStorage)
    implementation(projects.core.coreNetwork)
}