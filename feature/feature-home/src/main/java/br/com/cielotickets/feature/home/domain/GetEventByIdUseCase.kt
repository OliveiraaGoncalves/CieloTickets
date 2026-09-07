package br.com.cielotickets.feature.home.domain

import br.com.cielotickets.core.common.AppResult
import br.com.cielotickets.core.common.UseCase
import javax.inject.Inject

/**
 * Usado por `TicketSelectionViewModel`/`PaymentViewModel` pra reconstruir o
 * [Event] a partir só do `eventId` guardado na rota — é o que permite essas
 * telas sobreviverem a `process death` sem precisar de um objeto `Event`
 * inteiro passado por `remember` no NavHost.
 */
class GetEventByIdUseCase @Inject constructor(
    private val repository: EventRepository
) : UseCase<String, Event>() {
    override suspend fun invoke(params: String): AppResult<Event> = repository.getEventById(params)
}
