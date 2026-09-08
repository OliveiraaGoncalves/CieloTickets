package br.com.cielotickets.feature.history.domain

import br.com.cielotickets.core.common.AppResult
import javax.inject.Inject

class GetPurchaseHistoryUseCaseImpl @Inject constructor(
    private val repository: PurchaseHistoryRepository
) : GetPurchaseHistoryUseCase {
    override suspend fun invoke(): AppResult<List<PurchaseHistoryItem>> = repository.getHistory()
}
