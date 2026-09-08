package br.com.cielotickets.core.network.events

import br.com.cielotickets.core.common.EventModel

fun EventResponse.toDomain() = EventModel(
    id = id,
    title = title,
    venue = venue,
    dateTimeIso = dateTimeIso,
    priceCents = priceCents,
    availableTickets = availableTickets,
    imageUrl = imageUrl
)