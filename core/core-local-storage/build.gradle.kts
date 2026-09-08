plugins {
    alias(libs.plugins.cielotickets.android.library)
    alias(libs.plugins.cielotickets.android.hilt)
}

dependencies {
    implementation(projects.core.coreCommon)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}