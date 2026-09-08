package br.com.cielotickets.feature.receipt

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseReceiptModel
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.receipt.data.ReceiptRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Cobre o mapeamento Entity->Model que `GetReceiptUseCaseImpl` nunca
 * exercita diretamente (só mocka `ReceiptRepository` pela interface) —
 * achado real do relatório de cobertura (Kover), ver
 * docs/ARCHITECTURE.md#cobertura.
 */
class ReceiptRepositoryImplTest {

    private val dao: PurchaseAttemptDao = mockk()
    private val repository = ReceiptRepositoryImpl(dao)

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

    @Test
    fun `comprovante encontrado mapeia Entity para PurchaseReceiptModel`() = runTest {
        coEvery { dao.findByKey("key-1") } returns entity

        val result = repository.getReceipt("key-1")

        val expected = PurchaseReceiptModel(
            idempotencyKey = "key-1",
            order = PurchaseOrderModel("evt-1", "Show X", 2, 30000),
            status = PurchaseStatus.APPROVED,
            cieloTransactionId = "tx-1"
        )
        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun `comprovante nao encontrado vira Failure com mensagem especifica`() = runTest {
        coEvery { dao.findByKey("key-inexistente") } returns null

        val result = repository.getReceipt("key-inexistente")

        val expected = AppResult.Failure(DomainError.Unknown(message = "Comprovante não encontrado: key-inexistente"))
        assertEquals(expected, result)
    }

    @Test
    fun `erro inesperado do dao vira Failure generico`() = runTest {
        coEvery { dao.findByKey("key-1") } throws IllegalStateException("boom")

        val result = repository.getReceipt("key-1") as AppResult.Failure

        val error = result.error as DomainError.Unknown
        assertEquals("Não foi possível carregar o comprovante", error.message)
    }

    @Test
    fun `cancelamento de coroutine nao e engolido`() = runTest {
        coEvery { dao.findByKey("key-1") } throws CancellationException("cancelado")

        var caught: CancellationException? = null
        try {
            repository.getReceipt("key-1")
        } catch (e: CancellationException) {
            caught = e
        }

        assertEquals("cancelado", caught?.message)
    }
}
