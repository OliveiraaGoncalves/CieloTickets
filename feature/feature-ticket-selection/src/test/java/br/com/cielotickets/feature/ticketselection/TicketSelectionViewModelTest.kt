package br.com.cielotickets.feature.ticketselection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.common.GetEventByIdUseCase
import br.com.cielotickets.feature.ticketselection.presentation.TicketSelectionViewModel
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

/**
 * Requisito funcional 2 do case: "Selecionar quantidade (ex: 2 unidades).
 * Recalcula o valor total de forma precisa no ViewModel."
 *
 * O ViewModel recebe só o `eventId` via [SavedStateHandle] (rota tipada) e
 * recarrega o [EventModel] sozinho — por isso todo teste aqui mocka
 * [GetEventByIdUseCase] em vez de chamar um `setEvent()` (que não existe
 * mais: ver docstring do ViewModel pra o porquê da mudança).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TicketSelectionViewModelTest {

    private val event = EventModel(
        id = "evt-1",
        title = "Show X",
        venue = "Arena Y",
        dateTimeIso = "2026-10-10T20:00:00",
        priceCents = 15000,
        availableTickets = 3,
        imageUrl = null
    )

    private val getEventById: GetEventByIdUseCase = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { getEventById(event.id) } returns AppResult.Success(event)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(eventId: String = event.id) =
        TicketSelectionViewModel(SavedStateHandle(mapOf("eventId" to eventId)), getEventById)

    @Test
    fun `comeca com quantidade 1 e carrega o evento a partir do eventId da rota`() = runTest {
        viewModel().state.test {
            assertEquals(1, awaitItem().quantity) // estado inicial, evento ainda carregando
            val loaded = awaitItem()
            assertEquals(event, loaded.event)
            assertEquals(false, loaded.loadFailed)
        }
    }

    @Test
    fun `evento nao encontrado marca loadFailed`() = runTest {
        coEvery { getEventById("evt-inexistente") } returns AppResult.Failure(
            br.com.cielotickets.core.common.DomainError.Unknown(message = "não encontrado")
        )

        viewModel(eventId = "evt-inexistente").state.test {
            skipItems(1) // estado inicial
            val result = awaitItem()
            assertEquals(true, result.loadFailed)
        }
    }

    @Test
    fun `incrementar duas vezes recalcula o total corretamente`() = runTest {
        val vm = viewModel()
        vm.state.test { skipItems(2) } // espera o evento carregar

        vm.increment()
        vm.increment()

        assertEquals(3, vm.state.value.quantity)
        assertEquals(45000L, vm.state.value.totalCents)
    }

    @Test
    fun `nao incrementa alem do estoque disponivel`() = runTest {
        val vm = viewModel() // availableTickets = 3
        vm.state.test { skipItems(2) }

        repeat(5) { vm.increment() }

        assertEquals(3, vm.state.value.quantity)
    }

    @Test
    fun `nao decrementa abaixo de 1`() = runTest {
        val vm = viewModel()
        vm.state.test { skipItems(2) }

        repeat(5) { vm.decrement() }

        assertEquals(1, vm.state.value.quantity)
    }
}
