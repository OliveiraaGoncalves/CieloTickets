package br.com.cielotickets.feature.payment.navigation

import kotlinx.serialization.Serializable

/** `eventId`+`quantity` — o suficiente pro `PaymentViewModel` reconstruir o `PurchaseOrderModel`. */
@Serializable
data class PaymentRoute(val eventId: String, val quantity: Int)
