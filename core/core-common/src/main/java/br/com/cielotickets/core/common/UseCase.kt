package br.com.cielotickets.core.common

interface UseCase<in Params, out T> {
    suspend operator fun invoke(params: Params): AppResult<T>
}

interface NoParamsUseCase<out T> {
    suspend operator fun invoke(): AppResult<T>
}
