plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.kover)
}

// Regra de arquitetura (documentada em docs/ARCHITECTURE.md):
// - Módulos "core:*" NUNCA declaram dependência de módulos "feature:*".
// - Módulos "feature:*" podem depender de qualquer módulo "core:*".
// - O módulo "app" é o único que conhece todos os módulos (orquestração + DI + NavHost).

// Cobertura de teste agregada (Kover) — só a raiz aplica o plugin; cada
// módulo entra via `kover(project(...))`, sem precisar tocar no
// build.gradle.kts de cada um. Rodar: `./gradlew koverHtmlReport` (relatório
// em build/reports/kover/html/index.html) ou `./gradlew koverXmlReport`.
dependencies {
    kover(projects.app)
    kover(projects.core.coreCommon)
    kover(projects.core.coreNetwork)
    kover(projects.core.coreLocalStorage)
    kover(projects.core.corePaymentCielo)
    kover(projects.core.coreDesignsystem)
    kover(projects.feature.featureHome)
    kover(projects.feature.featureTicketSelection)
    kover(projects.feature.featurePayment)
    kover(projects.feature.featureReceipt)
    kover(projects.feature.featureHistory)
}

kover {
    reports {
        filters {
            excludes {
                // Código gerado (Hilt/Dagger, Room, BuildConfig) e módulos de
                // DI (@Binds não tem corpo pra cobrir; @Provides só roda com
                // o grafo do Hilt de pé, não em teste unitário puro) — sem
                // isso a métrica fica diluída por código que não é nosso e
                // nunca teria como ser "coberto" por teste unitário.
                classes(
                    "*.BuildConfig",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*.Hilt_*",
                    "*_HiltModules*",
                    "*_HiltComponents*",
                    "*_Impl",
                    "*_Impl\$*",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.internal.*"
                )
                packages("*.di")
            }
        }
    }
}
