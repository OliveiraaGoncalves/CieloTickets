package br.com.cielotickets.feature.home.data

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.DomainError
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.common.EventRepository
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.toDomain
import br.com.cielotickets.core.localstorage.db.toEntity
import br.com.cielotickets.core.network.events.EventRemoteDataSource
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val remoteDataSource: EventRemoteDataSource,
    private val eventDao: EventDao
) : EventRepository {

    companion object {
        private const val ERROR_LOADING_EVENTS = "Não foi possível carregar os eventos"
        private const val ERROR_EVENT_NOT_FOUND = "Evento não encontrado"
        private const val ERROR_LOADING_LOCAL_EVENT = "Não foi possível carregar o evento local"
    }

    override suspend fun getAvailableEvents(): AppResult<List<EventModel>> = try {
        when (val remote = remoteDataSource.getEvents()) {
            is AppResult.Success -> {
                eventDao.replaceAll(remote.data.map { it.toEntity() })
                remote
            }
            is AppResult.Failure -> {
                val cached = eventDao.getAll().map { it.toDomain() }
                if (cached.isNotEmpty()) AppResult.Success(cached) else remote
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e, ERROR_LOADING_EVENTS))
    }

    override suspend fun getEventById(id: String): AppResult<EventModel> = try {
        val entity = eventDao.getById(id)
        if (entity != null) {
            AppResult.Success(entity.toDomain())
        } else {
            AppResult.Failure(DomainError.Unknown(message = "$ERROR_EVENT_NOT_FOUND: $id"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e, ERROR_LOADING_LOCAL_EVENT))
    }
}