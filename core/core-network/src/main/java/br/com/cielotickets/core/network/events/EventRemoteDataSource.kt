package br.com.cielotickets.core.network.events

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.network.safeApiCall
import javax.inject.Inject

interface EventRemoteDataSource {
    suspend fun getEvents(): AppResult<List<EventModel>>
}

class EventRemoteDataSourceImpl @Inject constructor(
    private val api: EventApiService
) : EventRemoteDataSource {
    override suspend fun getEvents(): AppResult<List<EventModel>> = safeApiCall {
        api.getEvents().map { it.toDomain() }
    }
}