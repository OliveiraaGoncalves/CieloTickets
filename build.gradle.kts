plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}

// Regra de arquitetura (documentada em docs/ARCHITECTURE.md):
// - Módulos "core:*" NUNCA declaram dependência de módulos "feature:*".
// - Módulos "feature:*" podem depender de qualquer módulo "core:*".
// - O módulo "app" é o único que conhece todos os módulos (orquestração + DI + NavHost).
