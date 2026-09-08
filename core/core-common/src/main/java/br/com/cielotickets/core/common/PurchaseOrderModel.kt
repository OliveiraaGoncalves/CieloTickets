package br.com.cielotickets.core.common

data class PurchaseOrderModel(
    val eventId: String,
    val eventTitle: String,
    val ticketQuantity: Int,
    val totalAmountCents: Long
)