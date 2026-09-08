package br.com.cielotickets.core.network.events

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.EventModel
import br.com.cielotickets.core.common.SeedEvents
import kotlinx.coroutines.delay

private const val SIMULATED_NETWORK_DELAY_MS = 300L

class FakeEventRemoteDataSource : EventRemoteDataSource {
    override suspend fun getEvents(): AppResult<List<EventModel>> {
        delay(SIMULATED_NETWORK_DELAY_MS)
        return AppResult.Success(SeedEvents.all)
    }
}