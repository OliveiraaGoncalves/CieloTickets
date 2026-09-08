import java.util.Properties

plugins {
    alias(libs.plugins.cielotickets.android.library)
    alias(libs.plugins.cielotickets.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Credenciais da Cielo (Client-Id/Access-Token) NUNCA hardcoded no fonte —
// lidas de `local.properties` (gitignorado) ou de variável de ambiente
// (CI), com um placeholder óbvio se nenhum dos dois existir, pra o projeto
// continuar compilando/rodando contra o emulador sem credencial real.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun cieloSecret(key: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: "SEU_${key.removePrefix("CIELO_")}_AQUI"

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "CIELO_CLIENT_ID", "\"${cieloSecret("CIELO_CLIENT_ID")}\"")
        buildConfigField("String", "CIELO_ACCESS_TOKEN", "\"${cieloSecret("CIELO_ACCESS_TOKEN")}\"")
    }
}

dependencies {
    implementation(projects.core.coreCommon)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    // Integração via DEEPLINK (recomendada pela Cielo desde a descontinuação
    // do SDK) — NÃO depende de nenhum .aar/.m2 local. Ver docs/ARCHITECTURE.md
    // e https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local
}
