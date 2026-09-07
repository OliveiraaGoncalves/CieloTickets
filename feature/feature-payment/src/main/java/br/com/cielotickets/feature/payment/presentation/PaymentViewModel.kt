package br.com.cielotickets.feature.payment.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.feature.home.domain.GetEventByIdUseCase
import br.com.cielotickets.feature.payment.R
import br.com.cielotickets.feature.payment.domain.ProcessPaymentUseCase
import br.com.cielotickets.feature.payment.domain.PurchaseOrder
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class PaymentUiState {
    data object Idle : PaymentUiState()
    data object Processing : PaymentUiState()
    data class Success(val receipt: PurchaseReceipt) : PaymentUiState()

    /** Negada/erro genérico — fica na tela, oferece "Tentar novamente". */
    data class Failed(@StringRes val messageRes: Int) : PaymentUiState()

    /**
     * Cancelada (pelo usuário ou pela maquininha) ou timeout — requisito do
     * case pede um tratamento diferente daqui de "negada": "retorno seguro
     * para o resumo da compra mantendo o estado anterior", não "tentar de
     * novo aqui". `PaymentScreen` reage a esse estado navegando de volta
     * pro resumo (`ticket_selection`) em vez de mostrar retry.
     */
    data class Cancelled(@StringRes val messageRes: Int) : PaymentUiState()
}

data class PaymentScreenState(
    /** Null enquanto o [PurchaseOrder] ainda não foi reconstruído a partir do eventId da rota. */
    val order: PurchaseOrder? = null,
    val orderLoadFailed: Boolean = false,
    val paymentState: PaymentUiState = PaymentUiState.Idle
)

/**
 * Recebe só `eventId`+`quantity` via [SavedStateHandle] (rota tipada
 * `PaymentRoute`, em `navigation/`) e reconstrói o [PurchaseOrder] sozinho —
 * mesma ideia do `TicketSelectionViewModel` (inclusive o porquê de ler os
 * campos direto via `get<T>()` em vez de `toRoute()`: ver docstring de lá).
 *
 * **Ponto crítico de correção**: a `idempotencyKey` é persistida no próprio
 * [SavedStateHandle] em vez de viver só num campo do ViewModel. Se o
 * processo morrer com uma cobrança `PENDING` na Cielo Smart (a janela mais
 * provável de `process death` neste app, já que o fluxo inteiro depende de
 * sair pro app da maquininha e voltar) e o ViewModel for recriado do zero,
 * ele precisa continuar enxergando a MESMA chave — senão o curto-circuito
 * de idempotência em [ProcessPaymentUseCase] nunca encontraria a tentativa
 * antiga, e um "tentar de novo" pós-restauração cobraria de novo.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventById: GetEventByIdUseCase,
    private val processPayment: ProcessPaymentUseCase
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle.get<String>("eventId"))
    private val quantity: Int = checkNotNull(savedStateHandle.get<Int>("quantity"))

    private val idempotencyKey: String =
        savedStateHandle.get<String>(KEY_IDEMPOTENCY_KEY) ?: UUID.randomUUID().toString().also {
            savedStateHandle[KEY_IDEMPOTENCY_KEY] = it
        }

    private val _state = MutableStateFlow(PaymentScreenState())
    val state: StateFlow<PaymentScreenState> = _state.asStateFlow()

    private var paymentJob: Job? = null

    init {
        viewModelScope.launch {
            when (val result = getEventById(eventId)) {
                is AppResult.Success -> _state.value = _state.value.copy(
                    order = PurchaseOrder(
                        eventId = result.data.id,
                        eventTitle = result.data.title,
                        ticketQuantity = quantity,
                        totalAmountCents = result.data.priceCents * quantity
                    )
                )
                is AppResult.Failure -> _state.value = _state.value.copy(orderLoadFailed = true)
            }
        }
    }

    fun pay() {
        val order = _state.value.order ?: return
        if (_state.value.paymentState is PaymentUiState.Processing) return // trava reenvio por duplo clique
        paymentJob = viewModelScope.launch {
            _state.value = _state.value.copy(paymentState = PaymentUiState.Processing)
            when (val result = processPayment(order, idempotencyKey)) {
                is AppResult.Success -> _state.value = _state.value.copy(paymentState = PaymentUiState.Success(result.data))
                is AppResult.Failure -> _state.value = _state.value.copy(paymentState = stateFor(result.error))
            }
        }
    }

    /**
     * Botão "Cancelar" enquanto aguarda a Cielo Smart — sem isso, se o
     * usuário sair do app da Cielo sem concluir nada, a tela ficava presa
     * em "Processando..." até o timeout de 5 minutos do gateway, sem
     * nenhuma saída visível. Cancela a coroutine de verdade (não só a UI):
     * `ProcessPaymentUseCase` grava CANCELLED antes de propagar a
     * cancelação, então um novo "Pagar" depois não fica preso atrás do
     * curto-circuito de PENDING.
     */
    fun cancelWaiting() {
        if (_state.value.paymentState !is PaymentUiState.Processing) return
        paymentJob?.cancel()
        _state.value = _state.value.copy(paymentState = PaymentUiState.Cancelled(R.string.payment_error_cancelled))
    }

    /**
     * Requisito: tratamento explícito e diferenciado por tipo de falha —
     * negada fica na tela (retry), cancelada/timeout volta pro resumo.
     */
    private fun stateFor(error: DomainError): PaymentUiState = when (error) {
        is DomainError.PaymentDenied -> PaymentUiState.Failed(R.string.payment_error_denied)
        is DomainError.PaymentCancelled -> PaymentUiState.Cancelled(R.string.payment_error_cancelled)
        is DomainError.Timeout -> PaymentUiState.Cancelled(R.string.payment_error_timeout)
        is DomainError.Network -> PaymentUiState.Failed(R.string.payment_error_network)
        else -> PaymentUiState.Failed(R.string.payment_error_generic)
    }

    private companion object {
        const val KEY_IDEMPOTENCY_KEY = "idempotency_key"
    }
}
