package br.com.cielotickets.feature.receipt.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt

/** Porta de domínio — reconstrói o comprovante a partir só da idempotencyKey. */
interface ReceiptRepository {
    suspend fun getReceipt(idempotencyKey: String): AppResult<PurchaseReceipt>
}
