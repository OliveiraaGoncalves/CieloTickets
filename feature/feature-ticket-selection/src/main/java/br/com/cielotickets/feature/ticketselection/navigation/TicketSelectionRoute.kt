package br.com.cielotickets.feature.ticketselection.navigation

import kotlinx.serialization.Serializable

/** Só o `eventId` — o [TicketSelectionViewModel][br.com.cielotickets.feature.ticketselection.presentation.TicketSelectionViewModel] recarrega o [br.com.cielotickets.feature.home.domain.Event] a partir dele. */
@Serializable
data class TicketSelectionRoute(val eventId: String)
