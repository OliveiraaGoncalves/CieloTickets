package br.com.cielotickets.feature.receipt.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.PurchaseReceiptModel

interface ReceiptRepository {
    suspend fun getReceipt(idempotencyKey: String): AppResult<PurchaseReceiptModel>
}
