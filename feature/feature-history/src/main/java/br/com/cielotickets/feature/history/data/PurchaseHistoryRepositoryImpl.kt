package br.com.cielotickets.feature.history.data

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.history.domain.PurchaseHistoryItem
import br.com.cielotickets.feature.history.domain.PurchaseHistoryRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class PurchaseHistoryRepositoryImpl @Inject constructor(
    private val dao: PurchaseAttemptDao
) : PurchaseHistoryRepository {

    companion object {
        private const val ERROR_LOADING_HISTORY = "Não foi possível carregar o histórico de compras"
    }

    override suspend fun getHistory(): AppResult<List<PurchaseHistoryItem>> = try {
        AppResult.Success(dao.history().map { it.toHistoryItem() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e, ERROR_LOADING_HISTORY))
    }
}

private fun PurchaseAttempt.toHistoryItem() = PurchaseHistoryItem(
    idempotencyKey = idempotencyKey,
    eventTitle = eventTitle,
    ticketQuantity = ticketQuantity,
    totalAmountCents = totalAmountCents,
    status = PurchaseStatus.valueOf(status),
    createdAtEpochMs = createdAtEpochMs
)