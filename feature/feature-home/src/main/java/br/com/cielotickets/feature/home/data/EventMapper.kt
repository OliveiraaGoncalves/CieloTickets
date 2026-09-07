package br.com.cielotickets.feature.home.data

import br.com.cielotickets.core.localstorage.db.EventEntity
import br.com.cielotickets.feature.home.domain.Event

fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    venue = venue,
    dateTimeIso = dateTimeIso,
    priceCents = priceCents,
    availableTickets = availableTickets,
    imageUrl = imageUrl
)
