package br.com.cielotickets.feature.home.domain

import br.com.cielotickets.core.common.AppResult

/** Porta de domínio — a UI e o UseCase não sabem que existe Retrofit por trás. */
interface EventRepository {
    suspend fun getAvailableEvents(): AppResult<List<Event>>
    suspend fun getEventById(id: String): AppResult<Event>
}
