package br.com.cielotickets.feature.receipt.navigation

import kotlinx.serialization.Serializable

/** Só a `idempotencyKey` — o `ReceiptViewModel` reconstrói o comprovante inteiro a partir dela. */
@Serializable
data class ReceiptRoute(val idempotencyKey: String)
