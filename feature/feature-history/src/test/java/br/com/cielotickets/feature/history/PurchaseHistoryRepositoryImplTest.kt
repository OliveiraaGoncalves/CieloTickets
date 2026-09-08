package br.com.cielotickets.feature.history

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.history.data.PurchaseHistoryRepositoryImpl
import br.com.cielotickets.feature.history.domain.PurchaseHistoryItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Cobre o mapeamento Entity->Model que `GetPurchaseHistoryUseCaseImpl` nunca
 * exercita diretamente (só mocka `PurchaseHistoryRepository` pela interface)
 * — achado real do relatório de cobertura (Kover), ver
 * docs/ARCHITECTURE.md#cobertura.
 */
class PurchaseHistoryRepositoryImplTest {

    private val dao: PurchaseAttemptDao = mockk()
    private val repository = PurchaseHistoryRepositoryImpl(dao)

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
    fun `historico mapeia cada Entity para PurchaseHistoryItem`() = runTest {
        coEvery { dao.history() } returns listOf(entity)

        val result = repository.getHistory()

        val expected = PurchaseHistoryItem(
            idempotencyKey = "key-1",
            eventTitle = "Show X",
            ticketQuantity = 2,
            totalAmountCents = 30000,
            status = PurchaseStatus.APPROVED,
            createdAtEpochMs = 1000
        )
        assertEquals(AppResult.Success(listOf(expected)), result)
    }

    @Test
    fun `historico vazio vira Success com lista vazia`() = runTest {
        coEvery { dao.history() } returns emptyList()

        val result = repository.getHistory()

        assertEquals(AppResult.Success(emptyList<PurchaseHistoryItem>()), result)
    }

    @Test
    fun `erro inesperado do dao vira Failure generico`() = runTest {
        coEvery { dao.history() } throws IllegalStateException("boom")

        val result = repository.getHistory() as AppResult.Failure

        val error = result.error as DomainError.Unknown
        assertEquals("Não foi possível carregar o histórico de compras", error.message)
    }

    @Test
    fun `cancelamento de coroutine nao e engolido`() = runTest {
        coEvery { dao.history() } throws CancellationException("cancelado")

        var caught: CancellationException? = null
        try {
            repository.getHistory()
        } catch (e: CancellationException) {
            caught = e
        }

        assertEquals("cancelado", caught?.message)
    }
}
