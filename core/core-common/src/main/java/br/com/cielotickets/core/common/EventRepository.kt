package br.com.cielotickets.core.common

interface EventRepository {
    suspend fun getAvailableEvents(): AppResult<List<EventModel>>
    suspend fun getEventById(id: String): AppResult<EventModel>
}