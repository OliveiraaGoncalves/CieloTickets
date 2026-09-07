package br.com.cielotickets.feature.payment.domain

data class PurchaseOrder(
    val eventId: String,
    val eventTitle: String,
    val ticketQuantity: Int,
    val totalAmountCents: Long
)

enum class PurchaseStatus { PENDING, APPROVED, DENIED, CANCELLED, ERROR }

data class PurchaseReceipt(
    val idempotencyKey: String,
    val order: PurchaseOrder,
    val status: PurchaseStatus,
    val cieloTransactionId: String?
)
