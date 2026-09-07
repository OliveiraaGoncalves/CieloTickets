package br.com.cielotickets.core.localstorage.db

/**
 * Catálogo fixo inserido na criação do banco (ver [LocalStorageModule]).
 * Substitui o antigo `EventApi` (Retrofit) — o case não exige backend
 * próprio e pede explicitamente eventos locais (docs/desafio.md, CT-01).
 */
object SeedEvents {
    val all = listOf(
        EventEntity(
            id = "evt-1",
            title = "Show Nacional Tour 2026",
            venue = "Arena Anhembi, São Paulo",
            dateTimeIso = "2026-10-10T20:00:00",
            priceCents = 15000,
            availableTickets = 120,
            imageUrl = null
        ),
        EventEntity(
            id = "evt-2",
            title = "Festival de Inverno",
            venue = "Parque Ibirapuera, São Paulo",
            dateTimeIso = "2026-10-24T18:30:00",
            priceCents = 8000,
            availableTickets = 300,
            imageUrl = null
        ),
        EventEntity(
            id = "evt-3",
            title = "Stand-up Comedy Night",
            venue = "Teatro Bradesco, São Paulo",
            dateTimeIso = "2026-11-05T21:00:00",
            priceCents = 12000,
            availableTickets = 60,
            imageUrl = null
        ),
        EventEntity(
            id = "evt-4",
            title = "Peça Teatral: O Alienista",
            venue = "Teatro Municipal, Rio de Janeiro",
            dateTimeIso = "2026-11-20T19:00:00",
            priceCents = 9500,
            availableTickets = 80,
            imageUrl = null
        ),
        EventEntity(
            id = "evt-5",
            title = "Final de Copa Regional",
            venue = "Estádio Municipal, Curitiba",
            dateTimeIso = "2026-12-06T16:00:00",
            priceCents = 20000,
            availableTickets = 500,
            imageUrl = null
        )
    )
}
