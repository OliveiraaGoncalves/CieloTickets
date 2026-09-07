plugins {
    alias(libs.plugins.cielotickets.android.library)
    alias(libs.plugins.cielotickets.android.hilt)
}

android {
    namespace = "br.com.cielotickets.core.localstorage"
}

dependencies {
    implementation(projects.core.coreCommon)
    implementation(libs.findLibrary("room-runtime").get())
    implementation(libs.findLibrary("room-ktx").get())
    ksp(libs.findLibrary("room-compiler").get())
}
