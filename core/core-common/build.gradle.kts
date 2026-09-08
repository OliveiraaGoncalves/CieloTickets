plugins {
    alias(libs.plugins.cielotickets.android.library)
    alias(libs.plugins.cielotickets.android.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)
    implementation(libs.javax.inject)
}
