package br.com.cielotickets.core.common

/**
 * Caso de uso assíncrono padrão (Clean Architecture). Cada feature expõe
 * casos de uso pequenos e testáveis em vez de "repositórios gordos"
 * consumidos direto pela ViewModel.
 */
abstract class UseCase<in Params, out T> {
    abstract suspend operator fun invoke(params: Params): AppResult<T>
}

abstract class NoParamsUseCase<out T> {
    abstract suspend operator fun invoke(): AppResult<T>
}
