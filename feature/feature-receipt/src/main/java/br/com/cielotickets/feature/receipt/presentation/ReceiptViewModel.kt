package br.com.cielotickets.feature.receipt.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt
import br.com.cielotickets.feature.receipt.R
import br.com.cielotickets.feature.receipt.domain.GetReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Requisito funcional 5: "Exibir comprovante/resumo da compra".
 *
 * Recebe só a `idempotencyKey` via [SavedStateHandle] (rota
 * `ReceiptRoute`, em `navigation/`) — sobrevive a `process death` porque
 * reconstrói o comprovante inteiro a partir do Room, em vez de receber um
 * [PurchaseReceipt] guardado em `remember` no NavHost. Lê a chave direto via
 * `get<String>()` em vez de `toRoute()` pelo mesmo motivo do
 * `TicketSelectionViewModel` (ver docstring de lá).
 */
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReceipt: GetReceiptUseCase
) : ViewModel() {

    private val idempotencyKey = checkNotNull(savedStateHandle.get<String>("idempotencyKey"))

    private val _uiState = MutableStateFlow<UiState<PurchaseReceipt>>(UiState.Loading)
    val uiState: StateFlow<UiState<PurchaseReceipt>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = when (val result = getReceipt(idempotencyKey)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(R.string.receipt_load_error)
            }
        }
    }
}
