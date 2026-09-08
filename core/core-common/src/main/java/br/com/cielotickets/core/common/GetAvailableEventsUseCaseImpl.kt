package br.com.cielotickets.core.common

import javax.inject.Inject

class GetAvailableEventsUseCaseImpl @Inject constructor(
    private val repository: EventRepository
) : GetAvailableEventsUseCase {
    override suspend fun invoke(): AppResult<List<EventModel>> = repository.getAvailableEvents()
}
