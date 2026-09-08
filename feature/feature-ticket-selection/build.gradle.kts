plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // `Event`/`GetEventByIdUseCase` moraram pra core-common (ver
    // docs/ARCHITECTURE.md) — nenhuma feature depende de outra feature.
    // `TicketSelectionRoute` (navigation/) é `@Serializable` — usada pela
    // Navigation-Compose pra codificar o eventId no SavedStateHandle.
    implementation(libs.kotlinx.serialization.json)
}
