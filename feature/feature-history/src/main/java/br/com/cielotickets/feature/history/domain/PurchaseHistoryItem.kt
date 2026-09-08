package br.com.cielotickets.feature.history.domain

import br.com.cielotickets.core.common.PurchaseStatus

data class PurchaseHistoryItem(
    val idempotencyKey: String,
    val eventTitle: String,
    val ticketQuantity: Int,
    val totalAmountCents: Long,
    val status: PurchaseStatus,
    val createdAtEpochMs: Long
)
