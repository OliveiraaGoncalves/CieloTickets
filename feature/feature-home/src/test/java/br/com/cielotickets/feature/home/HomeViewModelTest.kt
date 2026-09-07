package br.com.cielotickets.feature.home

import app.cash.turbine.test
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.feature.home.domain.Event
import br.com.cielotickets.feature.home.domain.EventRepository
import br.com.cielotickets.feature.home.domain.GetAvailableEventsUseCase
import br.com.cielotickets.feature.home.presentation.HomeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repository: EventRepository = mockk()
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quando carregar eventos com sucesso, uiState reflete a lista`() = runTest {
        val fakeEvent = Event("1", "Show X", "Arena Y", "2026-10-10T20:00", 15000, 100, null)
        coEvery { repository.getAvailableEvents() } returns AppResult.Success(listOf(fakeEvent))

        viewModel = HomeViewModel(GetAvailableEventsUseCase(repository))

        viewModel.uiState.test {
            assert(awaitItem() is UiState.Loading)
            val loaded = awaitItem() as UiState.Success
            assert(loaded.data == listOf(fakeEvent))
        }
    }

    @Test
    fun `catalogo vazio vira Success com lista vazia, nao Error`() = runTest {
        // Branch real e distinto de "falhou ao carregar": lista vazia é uma
        // resposta bem-sucedida do repositório, a Home é quem decide mostrar
        // a mensagem de "nenhum evento disponível" (ver HomeScreen).
        coEvery { repository.getAvailableEvents() } returns AppResult.Success(emptyList())

        viewModel = HomeViewModel(GetAvailableEventsUseCase(repository))

        viewModel.uiState.test {
            assert(awaitItem() is UiState.Loading)
            val loaded = awaitItem() as UiState.Success
            assert(loaded.data.isEmpty())
        }
    }

    @Test
    fun `quando falhar ao carregar, uiState reflete o erro`() = runTest {
        coEvery { repository.getAvailableEvents() } returns AppResult.Failure(
            DomainError.Unknown(message = "falha")
        )

        viewModel = HomeViewModel(GetAvailableEventsUseCase(repository))

        viewModel.uiState.test {
            assert(awaitItem() is UiState.Loading)
            assert(awaitItem() is UiState.Error)
        }
    }
}
