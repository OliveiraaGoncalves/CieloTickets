package br.com.cielotickets.core.common

/**
 * Wrapper de resultado usado em todas as fronteiras domain/data do app.
 * Evita exceptions "soltas" cruzando camadas — todo erro de integração
 * (rede, pagamento, storage) deve ser mapeado para um DomainError explícito.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: DomainError) : AppResult<Nothing>()
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (DomainError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
