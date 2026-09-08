package br.com.cielotickets.app.di

import br.com.cielotickets.core.network.di.EventNetworkModule
import br.com.cielotickets.core.network.events.EventRemoteDataSource
import br.com.cielotickets.core.network.events.FakeEventRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/**
 * Substitui [EventNetworkModule] nos testes instrumentados: usa
 * [FakeEventRemoteDataSource] (mesma fixture — `SeedEvents` — que a
 * produção usa pra rodar sem o mockapi.io configurado) em vez de bater no
 * mockapi.io de verdade. Sem isso os testes ficariam dependentes de rede e
 * não-determinísticos.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [EventNetworkModule::class])
object TestEventNetworkModule {
    @Provides
    fun provideEventRemoteDataSource(): EventRemoteDataSource = FakeEventRemoteDataSource()
}