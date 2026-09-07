plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
}

android {
    namespace = "br.com.cielotickets.feature.home"
}

dependencies {
    implementation(projects.core.coreLocalStorage)
}
