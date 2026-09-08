package br.com.cielotickets.core.network.events

import kotlinx.serialization.Serializable

@Serializable
data class EventResponse(
    val id: String,
    val title: String,
    val venue: String,
    val dateTimeIso: String,
    val priceCents: Long,
    val availableTickets: Int,
    val imageUrl: String? = null
)