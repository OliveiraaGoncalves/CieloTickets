package br.com.cielotickets.feature.payment

import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.core.paymentcielo.CieloChargeResult
import br.com.cielotickets.core.paymentcielo.CieloPaymentGateway
import br.com.cielotickets.feature.payment.domain.ProcessPaymentUseCase
import br.com.cielotickets.feature.payment.domain.PurchaseOrder
import br.com.cielotickets.feature.payment.domain.PurchaseStatus
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
    private val dao: PurchaseAttemptDao = mockk(relaxed = true)
    private val order = PurchaseOrder("evt-1", "Show X", 2, 30000)

    @Test
    fun `reenvio com a mesma idempotencyKey nao dispara nova cobranca`() = runTest {
        val key = "same-key-123"
        coEvery { dao.findByKey(key) } returns PurchaseAttempt(
            idempotencyKey = key,
            eventId = order.eventId,
            ticketQuantity = order.ticketQuantity,
            totalAmountCents = order.totalAmountCents,
            status = PurchaseStatus.APPROVED.name,
            cieloTransactionId = "tx-1",
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )

        val useCase = ProcessPaymentUseCase(gateway, dao)
        useCase(order, key)

        coVerify(exactly = 0) { gateway.charge(any()) }
    }

    @Test
    fun `pagamento aprovado registra status APPROVED`() = runTest {
        val key = "new-key-456"
        coEvery { dao.findByKey(key) } returns null
        coEvery { gateway.charge(any()) } returns CieloChargeResult.Approved("tx-2", "123456")

        val useCase = ProcessPaymentUseCase(gateway, dao)
        useCase(order, key)

        coVerify { dao.update(match { it.status == PurchaseStatus.APPROVED.name && it.cieloTransactionId == "tx-2" }) }
    }

    @Test
    fun `tentar novamente apos negado ou cancelado chama a Cielo de novo`() = runTest {
        val key = "retry-key-789"
        coEvery { dao.findByKey(key) } returns PurchaseAttempt(
            idempotencyKey = key,
            eventId = order.eventId,
            ticketQuantity = order.ticketQuantity,
            totalAmountCents = order.totalAmountCents,
            status = PurchaseStatus.DENIED.name,
            cieloTransactionId = null,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )
        coEvery { gateway.charge(any()) } returns CieloChargeResult.Approved("tx-3", "654321")

        val useCase = ProcessPaymentUseCase(gateway, dao)
        useCase(order, key)

        coVerify(exactly = 1) { gateway.charge(any()) }
        coVerify { dao.update(match { it.status == PurchaseStatus.APPROVED.name }) }
    }
}
