package br.com.cielotickets.core.network

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

private const val ERROR_UNEXPECTED_NETWORK = "Falha inesperada de rede"

suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    AppResult.Failure(DomainError.Timeout(e))
} catch (e: IOException) {
    AppResult.Failure(DomainError.Network(e))
} catch (e: HttpException) {
    AppResult.Failure(DomainError.Network(e))
} catch (e: Exception) {
    AppResult.Failure(DomainError.Unknown(e, ERROR_UNEXPECTED_NETWORK))
}