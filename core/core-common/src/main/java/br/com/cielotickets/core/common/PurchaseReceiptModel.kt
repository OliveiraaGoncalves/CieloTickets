package br.com.cielotickets.core.common

data class PurchaseReceiptModel(
    val idempotencyKey: String,
    val order: PurchaseOrderModel,
    val status: PurchaseStatus,
    val cieloTransactionId: String?
)