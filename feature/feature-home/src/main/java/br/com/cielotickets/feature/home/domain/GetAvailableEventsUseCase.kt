package br.com.cielotickets.feature.home.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.NoParamsUseCase
import javax.inject.Inject

class GetAvailableEventsUseCase @Inject constructor(
    private val repository: EventRepository
) : NoParamsUseCase<List<Event>>() {
    override suspend fun invoke(): AppResult<List<Event>> = repository.getAvailableEvents()
}
