package br.com.cielotickets.core.common

data class EventModel(
    val id: String,
    val title: String,
    val venue: String,
    val dateTimeIso: String,
    val priceCents: Long,
    val availableTickets: Int,
    val imageUrl: String?
)