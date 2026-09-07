plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.com.cielotickets.feature.payment"
}

dependencies {
    implementation(projects.core.coreLocalStorage)
    implementation(projects.core.corePaymentCielo)
    implementation(projects.feature.featureHome)
    // `PaymentRoute` (navigation/) é `@Serializable` — mesma razão do
    // feature-ticket-selection.
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
}
