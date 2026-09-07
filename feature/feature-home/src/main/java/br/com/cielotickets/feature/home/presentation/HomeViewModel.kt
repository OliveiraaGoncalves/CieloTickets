package br.com.cielotickets.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.feature.home.R
import br.com.cielotickets.feature.home.domain.Event
import br.com.cielotickets.feature.home.domain.GetAvailableEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAvailableEvents: GetAvailableEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Event>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Event>>> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = getAvailableEvents()) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(R.string.home_error_loading)
            }
        }
    }
}
