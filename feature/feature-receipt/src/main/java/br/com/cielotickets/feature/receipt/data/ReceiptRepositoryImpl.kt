package br.com.cielotickets.feature.receipt.data

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.payment.domain.PurchaseOrder
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt
import br.com.cielotickets.feature.payment.domain.PurchaseStatus
import br.com.cielotickets.feature.receipt.domain.ReceiptRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Reconstrói o comprovante a partir só da `idempotencyKey` (rota
 * [br.com.cielotickets.feature.receipt.navigation.ReceiptRoute]) — a
 * `PurchaseAttempt` já tem tudo (status, valor, transactionId), só falta o
 * título do evento, que vem de uma segunda consulta ao [EventDao].
 */
class ReceiptRepositoryImpl @Inject constructor(
    private val purchaseAttemptDao: PurchaseAttemptDao,
    private val eventDao: EventDao
) : ReceiptRepository {
    override suspend fun getReceipt(idempotencyKey: String): AppResult<PurchaseReceipt> = try {
        val attempt = purchaseAttemptDao.findByKey(idempotencyKey)
        if (attempt == null) {
            AppResult.Failure(DomainError.Unknown(message = "Comprovante não encontrado: $idempotencyKey"))
        } else {
            val event = eventDao.getById(attempt.eventId)
            val order = PurchaseOrder(
                eventId = attempt.eventId,
                eventTitle = event?.title ?: attempt.eventId,
                ticketQuantity = attempt.ticketQuantity,
                totalAmountCents = attempt.totalAmountCents
            )
            AppResult.Success(
                PurchaseReceipt(
                    idempotencyKey = attempt.idempotencyKey,
                    order = order,
                    status = PurchaseStatus.valueOf(attempt.status),
                    cieloTransactionId = attempt.cieloTransactionId
                )
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e, "Não foi possível carregar o comprovante"))
    }
}
