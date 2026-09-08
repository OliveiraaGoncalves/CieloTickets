package br.com.cielotickets.feature.receipt.data

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseReceiptModel
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.receipt.domain.ReceiptRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val purchaseAttemptDao: PurchaseAttemptDao
) : ReceiptRepository {

    companion object {
        private const val ERROR_RECEIPT_NOT_FOUND = "Comprovante não encontrado"
        private const val ERROR_LOADING_RECEIPT = "Não foi possível carregar o comprovante"
    }

    override suspend fun getReceipt(idempotencyKey: String): AppResult<PurchaseReceiptModel> = try {
        val attempt = purchaseAttemptDao.findByKey(idempotencyKey)
        if (attempt == null) {
            AppResult.Failure(DomainError.Unknown(message = "$ERROR_RECEIPT_NOT_FOUND: $idempotencyKey"))
        } else {
            val order = PurchaseOrderModel(
                eventId = attempt.eventId,
                eventTitle = attempt.eventTitle,
                ticketQuantity = attempt.ticketQuantity,
                totalAmountCents = attempt.totalAmountCents
            )
            AppResult.Success(
                PurchaseReceiptModel(
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
        AppResult.Failure(DomainError.Unknown(e, ERROR_LOADING_RECEIPT))
    }
}