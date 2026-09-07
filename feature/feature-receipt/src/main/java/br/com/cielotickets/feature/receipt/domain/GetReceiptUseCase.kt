package br.com.cielotickets.feature.receipt.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.UseCase
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt
import javax.inject.Inject

class GetReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository
) : UseCase<String, PurchaseReceipt>() {
    override suspend fun invoke(params: String): AppResult<PurchaseReceipt> = repository.getReceipt(params)
}
