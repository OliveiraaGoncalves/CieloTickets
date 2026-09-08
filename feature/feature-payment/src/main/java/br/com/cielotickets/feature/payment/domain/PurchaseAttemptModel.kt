package br.com.cielotickets.feature.payment.domain

import br.com.cielotickets.core.common.PurchaseStatus

data class PurchaseAttemptModel(
    val idempotencyKey: String,
    val eventId: String,
    val eventTitle: String,
    val ticketQuantity: Int,
    val totalAmountCents: Long,
    val status: PurchaseStatus,
    val cieloTransactionId: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
