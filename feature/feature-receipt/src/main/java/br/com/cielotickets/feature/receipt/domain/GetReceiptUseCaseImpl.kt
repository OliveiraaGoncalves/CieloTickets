package br.com.cielotickets.feature.receipt.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.PurchaseReceiptModel
import javax.inject.Inject

class GetReceiptUseCaseImpl @Inject constructor(
    private val repository: ReceiptRepository
) : GetReceiptUseCase {
    override suspend fun invoke(params: String): AppResult<PurchaseReceiptModel> = repository.getReceipt(params)
}
