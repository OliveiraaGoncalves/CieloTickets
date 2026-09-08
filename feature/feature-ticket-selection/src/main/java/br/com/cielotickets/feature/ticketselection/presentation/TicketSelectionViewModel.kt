package br.com.cielotickets.feature.ticketselection.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.common.GetEventByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketSelectionState(
    val event: EventModel? = null,
    val loadFailed: Boolean = false,
    val quantity: Int = 1
) {
    val totalCents: Long get() = (event?.priceCents ?: 0) * quantity
}

/**
 * Requisito funcional 2: "Selecionar a quantidade de ingressos".
 *
 * Recebe só o `eventId` via [SavedStateHandle] (rota tipada
 * `TicketSelectionRoute`, em `navigation/`) e recarrega o [EventModel] sozinho —
 * sobrevive a `process death` porque não depende de nenhum objeto de
 * domínio guardado em `remember` no NavHost. Efeito colateral bom: como o
 * `init` só roda uma vez por instância do ViewModel (não a cada
 * recomposição), voltar pra essa tela via `popBackStack()` — ex.: pagamento
 * cancelado — reencontra a MESMA instância com a quantidade já escolhida
 * preservada, sem precisar de nenhuma lógica manual de "não resetar se for
 * o mesmo evento".
 *
 * Lê `eventId` direto do [SavedStateHandle] (`get<String>`) em vez de
 * `savedStateHandle.toRoute<TicketSelectionRoute>()`: o decoder de rota
 * tipada passa por `Bundle` por baixo dos panos, que não é mockado em teste
 * unitário puro (só em instrumentado/Robolectric) — `get()` simples usa o
 * mapa interno do próprio `SavedStateHandle` e funciona nos dois. A chave
 * "eventId" tem que casar com o nome do campo em `TicketSelectionRoute`.
 */
@HiltViewModel
class TicketSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventById: GetEventByIdUseCase
) : ViewModel() {

    private val eventId = checkNotNull(savedStateHandle.get<String>("eventId"))

    private val _state = MutableStateFlow(TicketSelectionState())
    val state: StateFlow<TicketSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = getEventById(eventId)) {
                is AppResult.Success -> _state.value = _state.value.copy(event = result.data, loadFailed = false)
                is AppResult.Failure -> _state.value = _state.value.copy(loadFailed = true)
            }
        }
    }

    fun increment() {
        val max = _state.value.event?.availableTickets ?: Int.MAX_VALUE
        _state.value = _state.value.copy(quantity = (_state.value.quantity + 1).coerceAtMost(max))
    }

    fun decrement() {
        _state.value = _state.value.copy(quantity = (_state.value.quantity - 1).coerceAtLeast(1))
    }
}
