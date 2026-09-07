package br.com.cielotickets.feature.home.domain

data class Event(
    val id: String,
    val title: String,
    val venue: String,
    val dateTimeIso: String,
    val priceCents: Long,
    val availableTickets: Int,
    val imageUrl: String?
)
