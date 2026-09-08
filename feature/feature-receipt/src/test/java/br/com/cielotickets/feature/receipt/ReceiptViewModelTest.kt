package br.com.cielotickets.feature.receipt

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.core.common.PurchaseOrderModel
import br.com.cielotickets.core.common.PurchaseReceiptModel
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.feature.receipt.domain.GetReceiptUseCase
import br.com.cielotickets.feature.receipt.presentation.ReceiptViewModel
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
 * Requisito funcional 5: "Exibir comprovante/resumo da compra". O ViewModel
 * recebe só a `idempotencyKey` via [SavedStateHandle] e reconstrói o
 * comprovante via [GetReceiptUseCase] — é isso que permite sobreviver a
 * `process death` (ver docstring do ViewModel).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptViewModelTest {

    private val receipt = PurchaseReceiptModel(
        idempotencyKey = "key-1",
        order = PurchaseOrderModel("evt-1", "Show X", 2, 30000),
        status = PurchaseStatus.APPROVED,
        cieloTransactionId = "tx-1"
    )
    private val getReceipt: GetReceiptUseCase = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(idempotencyKey: String = "key-1") =
        ReceiptViewModel(SavedStateHandle(mapOf("idempotencyKey" to idempotencyKey)), getReceipt)

    @Test
    fun `sucesso reconstroi o comprovante certo`() = runTest {
        coEvery { getReceipt("key-1") } returns AppResult.Success(receipt)

        viewModel().uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            val success = awaitItem() as UiState.Success
            assertEquals(receipt, success.data)
        }
    }

    @Test
    fun `chave inexistente vira UiState Error`() = runTest {
        coEvery { getReceipt("key-inexistente") } returns AppResult.Failure(
            DomainError.Unknown(message = "não encontrado")
        )

        viewModel(idempotencyKey = "key-inexistente").uiState.test {
            skipItems(1) // Loading
            val error = awaitItem() as UiState.Error
            assertEquals(R.string.receipt_load_error, error.messageRes)
        }
    }
}
