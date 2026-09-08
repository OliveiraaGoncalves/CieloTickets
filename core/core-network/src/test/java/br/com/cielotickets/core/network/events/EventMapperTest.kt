package br.com.cielotickets.core.network.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventMapperTest {
    @Test
    fun `EventResponse vira EventModel preservando todos os campos`() {
        val dto = EventResponse(
            id = "evt-1",
            title = "Show X",
            venue = "Arena Y",
            dateTimeIso = "2026-10-10T20:00:00",
            priceCents = 15000,
            availableTickets = 100,
            imageUrl = "https://exemplo.com/img.png"
        )

        val event = dto.toDomain()

        assertEquals(dto.id, event.id)
        assertEquals(dto.title, event.title)
        assertEquals(dto.venue, event.venue)
        assertEquals(dto.dateTimeIso, event.dateTimeIso)
        assertEquals(dto.priceCents, event.priceCents)
        assertEquals(dto.availableTickets, event.availableTickets)
        assertEquals(dto.imageUrl, event.imageUrl)
    }

    @Test
    fun `imageUrl nulo no JSON vira null no domain, nao quebra`() {
        val dto = EventResponse("evt-2", "Show Y", "Arena Z", "2026-11-01T20:00:00", 8000, 50, imageUrl = null)

        assertEquals(null, dto.toDomain().imageUrl)
    }
}