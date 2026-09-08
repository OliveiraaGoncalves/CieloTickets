package br.com.cielotickets.core.common

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EventUseCaseModule {
    @Binds
    abstract fun bindGetAvailableEventsUseCase(impl: GetAvailableEventsUseCaseImpl): GetAvailableEventsUseCase

    @Binds
    abstract fun bindGetEventByIdUseCase(impl: GetEventByIdUseCaseImpl): GetEventByIdUseCase
}
