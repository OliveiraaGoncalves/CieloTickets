package br.com.cielotickets.feature.payment

import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.payment.data.PurchaseRepositoryImpl
import br.com.cielotickets.feature.payment.domain.PurchaseAttemptModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Cobre o mapeamento Entity<->Model que `ProcessPaymentUseCaseImpl` nunca
 * exercita diretamente (só mocka `PurchaseRepository` pela interface) —
 * achado real do relatório de cobertura (Kover), ver
 * docs/ARCHITECTURE.md#cobertura.
 */
class PurchaseRepositoryImplTest {

    private val dao: PurchaseAttemptDao = mockk(relaxed = true)
    private val repository = PurchaseRepositoryImpl(dao)

    private val entity = PurchaseAttempt(
        idempotencyKey = "key-1",
        eventId = "evt-1",
        eventTitle = "Show X",
        ticketQuantity = 2,
        totalAmountCents = 30000,
        status = "APPROVED",
        cieloTransactionId = "tx-1",
        createdAtEpochMs = 1000,
        updatedAtEpochMs = 2000
    )

    private val model = PurchaseAttemptModel(
        idempotencyKey = "key-1",
        eventId = "evt-1",
        eventTitle = "Show X",
        ticketQuantity = 2,
        totalAmountCents = 30000,
        status = PurchaseStatus.APPROVED,
        cieloTransactionId = "tx-1",
        createdAtEpochMs = 1000,
        updatedAtEpochMs = 2000
    )

    @Test
    fun `findAttempt retorna null quando nao existe tentativa`() = runTest {
        coEvery { dao.findByKey("key-inexistente") } returns null

        val result = repository.findAttempt("key-inexistente")

        assertNull(result)
    }

    @Test
    fun `findAttempt mapeia Entity para Model preservando todos os campos`() = runTest {
        coEvery { dao.findByKey("key-1") } returns entity

        val result = repository.findAttempt("key-1")

        assertEquals(model, result)
    }

    @Test
    fun `insertAttempt mapeia Model para Entity antes de persistir`() = runTest {
        repository.insertAttempt(model)

        coVerify { dao.insert(entity) }
    }

    @Test
    fun `updateAttempt mapeia Model para Entity antes de persistir`() = runTest {
        repository.updateAttempt(model)

        coVerify { dao.update(entity) }
    }
}
