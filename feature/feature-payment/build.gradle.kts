plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.coreLocalStorage)
    implementation(projects.core.corePaymentCielo)
    // `PaymentRoute` (navigation/) é `@Serializable` — mesma razão do
    // feature-ticket-selection. `Event`/`PurchaseOrder`/etc. vêm de
    // core-common — nenhuma feature depende de outra feature.
    implementation(libs.kotlinx.serialization.json)
}
