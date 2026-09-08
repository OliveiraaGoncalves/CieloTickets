plugins {
    // Agora tem ViewModel via Hilt (ReceiptViewModel) — deixou de ser a
    // única feature "sem ViewModel" que justificava aplicar library+compose
    // direto (ver comentário no AndroidFeatureConventionPlugin).
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.coreLocalStorage)
    // `PurchaseOrder`/`PurchaseReceipt`/`PurchaseStatus` vêm de core-common
    // — nenhuma feature depende de outra feature.
    implementation(libs.zxing.core)
    // `ReceiptRoute` (navigation/) é `@Serializable`.
    implementation(libs.kotlinx.serialization.json)
}
