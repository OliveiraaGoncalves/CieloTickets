import java.util.Properties

plugins {
    alias(libs.plugins.cielotickets.android.library)
    alias(libs.plugins.cielotickets.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.cielotickets.android.test.junit5)
}

// URL do mockapi.io NÃO é segredo (não é credencial), mas segue o mesmo
// mecanismo de configuração do `CIELO_CLIENT_ID`/`CIELO_ACCESS_TOKEN`
// (core-payment-cielo/build.gradle.kts) por consistência: lida de
// `local.properties`/variável de ambiente, com um placeholder óbvio se
// nenhum dos dois existir — o projeto continua compilando sem travar
// esperando a URL real.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun networkConfig(key: String, default: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: default

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Base do projeto no mockapi.io — cada endpoint (ex.: "events") vira
        // um caminho relativo na interface Retrofit correspondente
        // (`events/EventApiService.kt`), não faz parte desta URL.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${networkConfig("API_BASE_URL", "https://SEU_PROJETO_AQUI.mockapi.io/api/v1/")}\""
        )
    }
}

dependencies {
    implementation(projects.core.coreCommon)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
}
