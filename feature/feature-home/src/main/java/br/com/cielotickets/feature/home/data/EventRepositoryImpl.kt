package br.com.cielotickets.feature.home.data

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.feature.home.domain.Event
import br.com.cielotickets.feature.home.domain.EventRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {
    override suspend fun getAvailableEvents(): AppResult<List<Event>> = try {
        AppResult.Success(eventDao.getAll().map { it.toDomain() })
    } catch (e: CancellationException) {
        throw e // nunca engolir cancelamento de coroutine — deixa propagar
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e, "Não foi possível carregar os eventos locais"))
    }

    override suspend fun getEventById(id: String): AppResult<Event> = try {
        val entity = eventDao.getById(id)
        if (entity != null) {
            AppResult.Success(entity.toDomain())
        } else {
            // Só acontece se o catálogo mudar entre a Home carregar a lista e o
            // usuário navegar (ou numa restauração de processo com um id
            // obsoleto) — não é um caso de rede, então cai no mesmo DomainError
            // genérico usado pelas outras falhas de leitura local.
            AppResult.Failure(DomainError.Unknown(message = "Evento não encontrado: $id"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e, "Não foi possível carregar o evento local"))
    }
}
