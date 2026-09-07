package br.com.cielotickets.core.common

import androidx.annotation.StringRes

/**
 * Estado genérico pra telas cujo fluxo é "busca algo, mostra loading,
 * sucesso ou erro" (ex.: `HomeViewModel`). Não é obrigatório pra toda
 * tela — fluxos transacionais (ex.: pagamento, com estado `Processing`
 * e ação de cancelar) modelam seu próprio sealed state, mais específico
 * do que esse molde genérico permitiria.
 *
 * `Error` carrega um `@StringRes` (não a `String` já resolvida) pra manter
 * o texto centralizado em `strings.xml` sem precisar injetar `Context`/
 * `Resources` no ViewModel — resolve com `stringResource()` na Composable.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(@StringRes val messageRes: Int) : UiState<Nothing>
}
