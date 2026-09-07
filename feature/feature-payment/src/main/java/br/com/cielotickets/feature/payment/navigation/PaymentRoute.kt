package br.com.cielotickets.feature.payment.navigation

import kotlinx.serialization.Serializable

/** `eventId`+`quantity` — o suficiente pro `PaymentViewModel` reconstruir o `PurchaseOrder`. */
@Serializable
data class PaymentRoute(val eventId: String, val quantity: Int)
