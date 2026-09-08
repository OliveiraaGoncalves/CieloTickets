package br.com.cielotickets.feature.payment.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseReceiptModel
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.paymentcielo.CieloChargeRequest
import br.com.cielotickets.core.paymentcielo.CieloChargeResult
import br.com.cielotickets.core.paymentcielo.CieloPaymentGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProcessPaymentUseCaseImpl @Inject constructor(
    private val gateway: CieloPaymentGateway,
    private val repository: PurchaseRepository
) : ProcessPaymentUseCase {
    companion object {
        private const val ORDER_DESCRIPTION_PREFIX = "Ingresso(s) - "
        private const val ERROR_CIELO_TIMEOUT = "Tempo esgotado aguardando retorno da Cielo Smart"
    }

    override suspend fun invoke(
        order: PurchaseOrderModel,
        idempotencyKey: String
    ): AppResult<PurchaseReceiptModel> {
        val existing = repository.findAttempt(idempotencyKey)
        val alreadyResolved = existing?.status == PurchaseStatus.APPROVED || existing?.status == PurchaseStatus.PENDING
        if (existing != null && alreadyResolved) {
            return AppResult.Success(existing.toReceipt(order))
        }

        val now = System.currentTimeMillis()
        repository.insertAttempt(
            PurchaseAttemptModel(
                idempotencyKey = idempotencyKey,
                eventId = order.eventId,
                eventTitle = order.eventTitle,
                ticketQuantity = order.ticketQuantity,
                totalAmountCents = order.totalAmountCents,
                status = PurchaseStatus.PENDING,
                cieloTransactionId = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        val chargeResult = try {
            gateway.charge(
                CieloChargeRequest(
                    idempotencyKey = idempotencyKey,
                    amountCents = order.totalAmountCents,
                    orderDescription = "$ORDER_DESCRIPTION_PREFIX${order.eventTitle}"
                )
            )
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) {
                updateStatus(idempotencyKey, order, PurchaseStatus.CANCELLED, txId = null, createdAtEpochMs = now)
            }
            throw cancellation
        }

        val (status, txId, error) = when (chargeResult) {
            is CieloChargeResult.Approved -> Triple(PurchaseStatus.APPROVED, chargeResult.cieloTransactionId, null)
            is CieloChargeResult.Denied -> Triple(PurchaseStatus.DENIED, null, DomainError.PaymentDenied(chargeResult.reasonCode))
            is CieloChargeResult.CancelledByUser -> Triple(PurchaseStatus.CANCELLED, null, DomainError.PaymentCancelled(true))
            CieloChargeResult.Timeout -> Triple(
                PurchaseStatus.CANCELLED,
                null,
                DomainError.Timeout(IllegalStateException(ERROR_CIELO_TIMEOUT))
            )
            is CieloChargeResult.IntegrationError -> Triple(PurchaseStatus.ERROR, null, DomainError.Network(chargeResult.cause))
        }

        updateStatus(idempotencyKey, order, status, txId, now)

        return if (error == null) {
            AppResult.Success(PurchaseReceiptModel(idempotencyKey, order, status, txId))
        } else {
            AppResult.Failure(error)
        }
    }

    private suspend fun updateStatus(
        idempotencyKey: String,
        order: PurchaseOrderModel,
        status: PurchaseStatus,
        txId: String?,
        createdAtEpochMs: Long
    ) {
        repository.updateAttempt(
            PurchaseAttemptModel(
                idempotencyKey = idempotencyKey,
                eventId = order.eventId,
                eventTitle = order.eventTitle,
                ticketQuantity = order.ticketQuantity,
                totalAmountCents = order.totalAmountCents,
                status = status,
                cieloTransactionId = txId,
                createdAtEpochMs = createdAtEpochMs,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }
}

private fun PurchaseAttemptModel.toReceipt(order: PurchaseOrderModel) = PurchaseReceiptModel(
    idempotencyKey = idempotencyKey,
    order = order,
    status = status,
    cieloTransactionId = cieloTransactionId
)
