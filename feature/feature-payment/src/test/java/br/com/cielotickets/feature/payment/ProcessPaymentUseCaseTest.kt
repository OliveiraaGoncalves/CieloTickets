package br.com.cielotickets.feature.payment

import br.com.cielotickets.core.paymentcielo.CieloChargeResult
import br.com.cielotickets.core.paymentcielo.CieloPaymentGateway
import br.com.cielotickets.feature.payment.domain.ProcessPaymentUseCaseImpl
import br.com.cielotickets.feature.payment.domain.PurchaseAttemptModel
import br.com.cielotickets.feature.payment.domain.PurchaseRepository
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Teste crítico exigido pelo case: garante que reenviar a MESMA
 * idempotencyKey (ex.: usuário toca "pagar" duas vezes após um APPROVED
 * já registrado) NÃO gera uma segunda chamada ao gateway da Cielo.
 */
class ProcessPaymentUseCaseTest {

    private val gateway: CieloPaymentGateway = mockk()
    private val repository: PurchaseRepository = mockk(relaxed = true)
    private val order = PurchaseOrderModel("evt-1", "Show X", 2, 30000)

    @Test
    fun `reenvio com a mesma idempotencyKey nao dispara nova cobranca`() = runTest {
        val key = "same-key-123"
        coEvery { repository.findAttempt(key) } returns PurchaseAttemptModel(
            idempotencyKey = key,
            eventId = order.eventId,
            eventTitle = order.eventTitle,
            ticketQuantity = order.ticketQuantity,
            totalAmountCents = order.totalAmountCents,
            status = PurchaseStatus.APPROVED,
            cieloTransactionId = "tx-1",
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )

        val useCase = ProcessPaymentUseCaseImpl(gateway, repository)
        useCase(order, key)

        coVerify(exactly = 0) { gateway.charge(any()) }
    }

    @Test
    fun `pagamento aprovado registra status APPROVED`() = runTest {
        val key = "new-key-456"
        coEvery { repository.findAttempt(key) } returns null
        coEvery { gateway.charge(any()) } returns CieloChargeResult.Approved("tx-2", "123456")

        val useCase = ProcessPaymentUseCaseImpl(gateway, repository)
        useCase(order, key)

        coVerify { repository.updateAttempt(match { it.status == PurchaseStatus.APPROVED && it.cieloTransactionId == "tx-2" }) }
    }

    @Test
    fun `tentar novamente apos negado ou cancelado chama a Cielo de novo`() = runTest {
        val key = "retry-key-789"
        coEvery { repository.findAttempt(key) } returns PurchaseAttemptModel(
            idempotencyKey = key,
            eventId = order.eventId,
            eventTitle = order.eventTitle,
            ticketQuantity = order.ticketQuantity,
            totalAmountCents = order.totalAmountCents,
            status = PurchaseStatus.DENIED,
            cieloTransactionId = null,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )
        coEvery { gateway.charge(any()) } returns CieloChargeResult.Approved("tx-3", "654321")

        val useCase = ProcessPaymentUseCaseImpl(gateway, repository)
        useCase(order, key)

        coVerify(exactly = 1) { gateway.charge(any()) }
        coVerify { repository.updateAttempt(match { it.status == PurchaseStatus.APPROVED }) }
    }
}
