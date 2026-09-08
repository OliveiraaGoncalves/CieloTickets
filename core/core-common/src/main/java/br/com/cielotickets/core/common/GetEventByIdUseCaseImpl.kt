package br.com.cielotickets.core.common

import javax.inject.Inject

class GetEventByIdUseCaseImpl @Inject constructor(
    private val repository: EventRepository
) : GetEventByIdUseCase {
    override suspend fun invoke(params: String): AppResult<EventModel> = repository.getEventById(params)
}
