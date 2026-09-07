package br.com.cielotickets.feature.home.di

import br.com.cielotickets.feature.home.data.EventRepositoryImpl
import br.com.cielotickets.feature.home.domain.EventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    @Binds
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository
}
