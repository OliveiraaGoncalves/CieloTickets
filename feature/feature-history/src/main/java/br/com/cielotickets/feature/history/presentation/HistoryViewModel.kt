package br.com.cielotickets.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.feature.history.R
import br.com.cielotickets.feature.history.domain.GetPurchaseHistoryUseCase
import br.com.cielotickets.feature.history.domain.PurchaseHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPurchaseHistory: GetPurchaseHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<PurchaseHistoryItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PurchaseHistoryItem>>> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = getPurchaseHistory()) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(R.string.history_error_loading)
            }
        }
    }
}
