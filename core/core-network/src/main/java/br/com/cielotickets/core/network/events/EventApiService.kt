package br.com.cielotickets.core.network.events

import retrofit2.http.GET

private const val EVENTS_ENDPOINT = "events"

interface EventApiService {
    @GET(EVENTS_ENDPOINT)
    suspend fun getEvents(): List<EventResponse>
}