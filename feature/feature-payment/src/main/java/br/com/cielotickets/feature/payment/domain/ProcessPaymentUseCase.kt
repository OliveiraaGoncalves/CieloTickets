package br.com.cielotickets.feature.payment.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseReceiptModel
import java.util.UUID

interface ProcessPaymentUseCase {
    suspend operator fun invoke(
        order: PurchaseOrderModel,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): AppResult<PurchaseReceiptModel>
}
