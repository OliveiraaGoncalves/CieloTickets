package br.com.cielotickets.feature.history

import app.cash.turbine.test
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.feature.history.domain.GetPurchaseHistoryUseCaseImpl
import br.com.cielotickets.feature.history.domain.PurchaseHistoryItem
import br.com.cielotickets.feature.history.domain.PurchaseHistoryRepository
import br.com.cielotickets.feature.history.presentation.HistoryViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val repository: PurchaseHistoryRepository = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso reflete a lista de compras`() = runTest {
        val item = PurchaseHistoryItem("key-1", "Show X", 2, 30000, PurchaseStatus.APPROVED, 0L)
        coEvery { repository.getHistory() } returns AppResult.Success(listOf(item))

        val viewModel = HistoryViewModel(GetPurchaseHistoryUseCaseImpl(repository))

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            val success = awaitItem() as UiState.Success
            assertEquals(listOf(item), success.data)
        }
    }

    @Test
    fun `sem compras vira Success com lista vazia, nao Error`() = runTest {
        coEvery { repository.getHistory() } returns AppResult.Success(emptyList())

        val viewModel = HistoryViewModel(GetPurchaseHistoryUseCaseImpl(repository))

        viewModel.uiState.test {
            skipItems(1) // Loading
            val success = awaitItem() as UiState.Success
            assertEquals(emptyList<PurchaseHistoryItem>(), success.data)
        }
    }

    @Test
    fun `falha ao carregar vira UiState Error`() = runTest {
        coEvery { repository.getHistory() } returns AppResult.Failure(DomainError.Unknown(message = "falha"))

        val viewModel = HistoryViewModel(GetPurchaseHistoryUseCaseImpl(repository))

        viewModel.uiState.test {
            skipItems(1)
            assert(awaitItem() is UiState.Error)
        }
    }
}
