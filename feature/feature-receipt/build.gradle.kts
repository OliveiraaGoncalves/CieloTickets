plugins {
    // Agora tem ViewModel via Hilt (ReceiptViewModel) — deixou de ser a
    // única feature "sem ViewModel" que justificava aplicar library+compose
    // direto (ver comentário no AndroidFeatureConventionPlugin).
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.com.cielotickets.feature.receipt"
}

// `libs.xxx.yyy` tipado não fica disponível num módulo que também aplica
// um plugin de convenção vindo do `build-logic` (includeBuild) — o método
// genérico `findLibrary("nome-no-toml")` do mesmo `libs` continua ok.
dependencies {
    implementation(projects.core.coreLocalStorage)
    implementation(projects.feature.featurePayment)
    implementation(libs.findLibrary("zxing-core").get())
    // `ReceiptRoute` (navigation/) é `@Serializable`.
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
}
