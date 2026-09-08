package br.com.cielotickets.feature.home

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.EventEntity
import br.com.cielotickets.core.network.events.EventRemoteDataSource
import br.com.cielotickets.feature.home.data.EventRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cobre a orquestração rede+cache descrita em docs/ARCHITECTURE.md:
 * rede é fonte de verdade e atualiza o cache; falha de rede cai pro cache
 * (offline-first); `getEventById` nunca bate rede (é o que dá resiliência
 * de `process death`/rede instável nas telas de seleção/pagamento).
 */
class EventRepositoryImplTest {

    private val remoteDataSource: EventRemoteDataSource = mockk()
    private val eventDao: EventDao = mockk(relaxed = true)
    private val repository = EventRepositoryImpl(remoteDataSource, eventDao)

    private val event = EventModel("evt-1", "Show X", "Arena Y", "2026-10-10T20:00:00", 15000, 100, null)
    private val entity = EventEntity("evt-1", "Show X", "Arena Y", "2026-10-10T20:00:00", 15000, 100, null)

    @Test
    fun `sucesso de rede atualiza o cache local`() = runTest {
        coEvery { remoteDataSource.getEvents() } returns AppResult.Success(listOf(event))

        val result = repository.getAvailableEvents()

        assertEquals(AppResult.Success(listOf(event)), result)
        coVerify { eventDao.replaceAll(listOf(entity)) }
    }

    @Test
    fun `falha de rede com cache nao vazio devolve o cache (offline-first)`() = runTest {
        coEvery { remoteDataSource.getEvents() } returns AppResult.Failure(DomainError.Network(Exception("sem rede")))
        coEvery { eventDao.getAll() } returns listOf(entity)

        val result = repository.getAvailableEvents() as AppResult.Success

        assertEquals(listOf(event), result.data)
        coVerify(exactly = 0) { eventDao.replaceAll(any()) }
    }

    @Test
    fun `falha de rede com cache vazio propaga o erro`() = runTest {
        coEvery { remoteDataSource.getEvents() } returns AppResult.Failure(DomainError.Network(Exception("sem rede")))
        coEvery { eventDao.getAll() } returns emptyList()

        val result = repository.getAvailableEvents()

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `getEventById le so do cache, nunca bate rede`() = runTest {
        coEvery { eventDao.getById("evt-1") } returns entity

        val result = repository.getEventById("evt-1")

        assertEquals(AppResult.Success(event), result)
        coVerify(exactly = 0) { remoteDataSource.getEvents() }
    }
}
