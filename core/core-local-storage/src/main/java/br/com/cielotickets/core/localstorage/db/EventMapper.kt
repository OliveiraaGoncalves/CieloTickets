package br.com.cielotickets.core.localstorage.db

import br.com.cielotickets.core.common.EventModel

fun EventEntity.toDomain() = EventModel(
    id = id,
    title = title,
    venue = venue,
    dateTimeIso = dateTimeIso,
    priceCents = priceCents,
    availableTickets = availableTickets,
    imageUrl = imageUrl
)

fun EventModel.toEntity() = EventEntity(
    id = id,
    title = title,
    venue = venue,
    dateTimeIso = dateTimeIso,
    priceCents = priceCents,
    availableTickets = availableTickets,
    imageUrl = imageUrl
)