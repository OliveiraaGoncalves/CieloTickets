package br.com.cielotickets.feature.payment.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.core.paymentcielo.CieloChargeRequest
import br.com.cielotickets.core.paymentcielo.CieloChargeResult
import br.com.cielotickets.core.paymentcielo.CieloPaymentGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Orquestra o requisito funcional 3 ("iniciar e concluir o pagamento via
 * integração com a Cielo") e o requisito não-funcional de anti-duplicidade:
 *
 * 1. Gera (ou reaproveita) uma idempotencyKey por PEDIDO, persistida ANTES
 *    da chamada à Cielo.
 * 2. Se já existe uma tentativa PENDING (pode estar em andamento) ou
 *    APPROVED (já foi cobrada) para essa key, não dispara nova cobrança —
 *    apenas retorna o estado já registrado. DENIED/CANCELLED/ERROR não
 *    chegaram a cobrar nada, então "tentar de novo" deve chamar a Cielo
 *    de verdade — não só reexibir a tentativa antiga como se fosse final.
 * 3. Grava o resultado (aprovada/negada/cancelada) — requisito funcional 4.
 * 4. Se o usuário cancelar manualmente enquanto aguarda a Cielo Smart (ver
 *    `PaymentViewModel.cancelWaiting`), a coroutine é cancelada — o
 *    `catch (CancellationException)` grava CANCELLED antes de propagar,
 *    senão a linha ficaria presa em PENDING e travaria qualquer retry
 *    futuro com essa mesma idempotencyKey (o curto-circuito do item 2).
 */
class ProcessPaymentUseCase @Inject constructor(
    private val gateway: CieloPaymentGateway,
    private val dao: PurchaseAttemptDao
) {
    suspend operator fun invoke(
        order: PurchaseOrder,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): AppResult<PurchaseReceipt> {
        val existing = dao.findByKey(idempotencyKey)
        val alreadyResolved = existing?.status == PurchaseStatus.APPROVED.name ||
            existing?.status == PurchaseStatus.PENDING.name
        if (existing != null && alreadyResolved) {
            return AppResult.Success(existing.toReceipt(order))
        }

        val now = System.currentTimeMillis()
        dao.insert(
            PurchaseAttempt(
                idempotencyKey = idempotencyKey,
                eventId = order.eventId,
                ticketQuantity = order.ticketQuantity,
                totalAmountCents = order.totalAmountCents,
                status = PurchaseStatus.PENDING.name,
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
                    orderDescription = "Ingresso(s) - ${order.eventTitle}"
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
                DomainError.Timeout(IllegalStateException("Tempo esgotado aguardando retorno da Cielo Smart"))
            )
            is CieloChargeResult.IntegrationError -> Triple(PurchaseStatus.ERROR, null, DomainError.Network(chargeResult.cause))
        }

        updateStatus(idempotencyKey, order, status, txId, now)

        return if (error == null) {
            AppResult.Success(PurchaseReceipt(idempotencyKey, order, status, txId))
        } else {
            AppResult.Failure(error)
        }
    }

    private suspend fun updateStatus(
        idempotencyKey: String,
        order: PurchaseOrder,
        status: PurchaseStatus,
        txId: String?,
        createdAtEpochMs: Long
    ) {
        dao.update(
            PurchaseAttempt(
                idempotencyKey = idempotencyKey,
                eventId = order.eventId,
                ticketQuantity = order.ticketQuantity,
                totalAmountCents = order.totalAmountCents,
                status = status.name,
                cieloTransactionId = txId,
                createdAtEpochMs = createdAtEpochMs,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }
}

private fun PurchaseAttempt.toReceipt(order: PurchaseOrder) = PurchaseReceipt(
    idempotencyKey = idempotencyKey,
    order = order,
    status = PurchaseStatus.valueOf(status),
    cieloTransactionId = cieloTransactionId
)
