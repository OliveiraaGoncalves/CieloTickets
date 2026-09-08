package br.com.cielotickets.feature.payment

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.common.GetEventByIdUseCase
import br.com.cielotickets.feature.payment.domain.ProcessPaymentUseCase
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseReceiptModel
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.feature.payment.presentation.PaymentUiState
import br.com.cielotickets.feature.payment.presentation.PaymentViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Requisito do case: "ViewModels (Apresentação): Auditoria de emissões de
 * StateFlow sequenciais (ex: Initial -> Loading -> Success/Error)" e
 * CT-05 ("Botão desabilitado no primeiro clique").
 *
 * O ViewModel recebe `eventId`+`quantity` via [SavedStateHandle] (rota
 * tipada) e reconstrói o [PurchaseOrderModel] via [GetEventByIdUseCase] — por
 * isso todo teste mocka esse use case em vez de passar um `PurchaseOrderModel`
 * direto pro `pay()` (que não recebe mais parâmetro nenhum).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val event = EventModel(
        id = "evt-1",
        title = "Show X",
        venue = "Arena Y",
        dateTimeIso = "2026-10-10T20:00:00",
        priceCents = 15000,
        availableTickets = 10,
        imageUrl = null
    )
    private val order = PurchaseOrderModel(event.id, event.title, 2, 30000)

    private val processPayment: ProcessPaymentUseCase = mockk()
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

    private fun viewModel() = PaymentViewModel(
        SavedStateHandle(mapOf("eventId" to event.id, "quantity" to 2)),
        getEventById,
        processPayment
    )

    @Test
    fun `pagamento aprovado emite Idle, Processing e Success em sequencia`() = runTest {
        val receipt = PurchaseReceiptModel("key-1", order, PurchaseStatus.APPROVED, "tx-1")
        coEvery { processPayment(any(), any()) } returns AppResult.Success(receipt)

        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(null, awaitItem().order) // estado inicial, pedido ainda carregando
            val loaded = awaitItem()
            assertEquals(order, loaded.order)
            assertEquals(PaymentUiState.Idle, loaded.paymentState)

            viewModel.pay()
            assertEquals(PaymentUiState.Processing, awaitItem().paymentState)
            val success = awaitItem().paymentState as PaymentUiState.Success
            assertEquals(receipt, success.receipt)
        }
    }

    @Test
    fun `pagamento negado exibe mensagem especifica de negado`() = runTest {
        coEvery { processPayment(any(), any()) } returns AppResult.Failure(DomainError.PaymentDenied("51"))

        val viewModel = viewModel()

        viewModel.state.test {
            skipItems(2) // carregando pedido + Idle
            viewModel.pay()
            skipItems(1) // Processing
            val failed = awaitItem().paymentState as PaymentUiState.Failed
            assertEquals(R.string.payment_error_denied, failed.messageRes)
        }
    }

    @Test
    fun `timeout volta pro resumo (Cancelled) em vez de ficar na tela como negado`() = runTest {
        coEvery { processPayment(any(), any()) } returns AppResult.Failure(
            DomainError.Timeout(IllegalStateException("timeout"))
        )

        val viewModel = viewModel()

        viewModel.state.test {
            skipItems(2)
            viewModel.pay()
            skipItems(1)
            val result = awaitItem().paymentState as PaymentUiState.Cancelled
            assertEquals(R.string.payment_error_timeout, result.messageRes)
        }
    }

    @Test
    fun `cancelamento vindo da Cielo Smart tambem volta pro resumo (Cancelled)`() = runTest {
        coEvery { processPayment(any(), any()) } returns AppResult.Failure(
            DomainError.PaymentCancelled(byUser = true)
        )

        val viewModel = viewModel()

        viewModel.state.test {
            skipItems(2)
            viewModel.pay()
            skipItems(1)
            val result = awaitItem().paymentState as PaymentUiState.Cancelled
            assertEquals(R.string.payment_error_cancelled, result.messageRes)
        }
    }

    @Test
    fun `clique duplo enquanto processando nao chama o use case duas vezes`() = runTest {
        coEvery { processPayment(any(), any()) } coAnswers {
            delay(1000)
            AppResult.Success(PurchaseReceiptModel("key-1", order, PurchaseStatus.APPROVED, "tx-1"))
        }

        val viewModel = viewModel()
        advanceUntilIdle() // pedido carregado, state == Idle

        viewModel.pay()
        runCurrent() // deixa a coroutine rodar até a suspensão dentro de processPayment
        viewModel.pay() // "duplo toque" — deve ser ignorado, state já é Processing

        advanceUntilIdle()

        coVerify(exactly = 1) { processPayment(any(), any()) }
    }

    @Test
    fun `cancelar enquanto processando nao deixa um resultado tardio sobrescrever o estado`() = runTest {
        coEvery { processPayment(any(), any()) } coAnswers {
            delay(10_000)
            AppResult.Success(PurchaseReceiptModel("key-1", order, PurchaseStatus.APPROVED, "tx-1"))
        }

        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.pay()
        runCurrent() // chega no delay() dentro de processPayment, state == Processing
        viewModel.cancelWaiting()

        val cancelled = viewModel.state.value.paymentState as PaymentUiState.Cancelled
        assertEquals(R.string.payment_error_cancelled, cancelled.messageRes)

        // Mesmo avançando o tempo todo (o que resolveria o delay original),
        // o job foi cancelado — não deve haver Success sobrescrevendo depois.
        advanceUntilIdle()
        assertTrue(viewModel.state.value.paymentState is PaymentUiState.Cancelled)
    }

    @Test
    fun `idempotencyKey persiste no SavedStateHandle entre recriacoes do ViewModel`() = runTest {
        // Simula process death: mesmo SavedStateHandle (o Android recria o
        // Bundle a partir do estado salvo), novo ViewModel. A chave usada
        // na segunda cobrança tem que ser IDÊNTICA à da primeira — é isso
        // que impede um "tentar de novo" pós-restauração de cobrar de novo.
        val handle = SavedStateHandle(mapOf("eventId" to event.id, "quantity" to 2))
        coEvery { processPayment(any(), any()) } returns AppResult.Failure(DomainError.PaymentDenied(null))

        val firstViewModel = PaymentViewModel(handle, getEventById, processPayment)
        advanceUntilIdle()
        firstViewModel.pay()
        advanceUntilIdle()

        val secondViewModel = PaymentViewModel(handle, getEventById, processPayment)
        advanceUntilIdle()
        secondViewModel.pay()
        advanceUntilIdle()

        val usedKeys = mutableListOf<String>()
        coVerify(exactly = 2) { processPayment(any(), capture(usedKeys)) }
        assertEquals(usedKeys[0], usedKeys[1])
    }
}
