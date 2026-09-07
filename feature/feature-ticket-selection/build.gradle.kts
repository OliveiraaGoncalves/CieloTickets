plugins {
    alias(libs.plugins.cielotickets.android.feature)
    alias(libs.plugins.cielotickets.android.test.junit5)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "br.com.cielotickets.feature.ticketselection"
}

dependencies {
    implementation(projects.feature.featureHome) // reaproveita o domain Event
    // `TicketSelectionRoute` (navigation/) é `@Serializable` — usada pela
    // Navigation-Compose pra codificar o eventId no SavedStateHandle.
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
}
