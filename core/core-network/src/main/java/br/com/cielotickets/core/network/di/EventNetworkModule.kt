package br.com.cielotickets.core.network.di

import br.com.cielotickets.core.network.events.EventApiService
import br.com.cielotickets.core.network.events.EventRemoteDataSource
import br.com.cielotickets.core.network.events.EventRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

/**
 * Troca entre a implementação real (mockapi.io via Retrofit) e
 * `FakeEventRemoteDataSource` (dev sem o mockapi.io configurado) é mudar só
 * o alvo do `@Binds` abaixo — mesmo padrão do `PaymentGatewayModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EventNetworkModule {

    @Binds
    abstract fun bindEventRemoteDataSource(impl: EventRemoteDataSourceImpl): EventRemoteDataSource

    companion object {
        @Provides
        fun provideEventApiService(retrofit: Retrofit): EventApiService = retrofit.create(EventApiService::class.java)
    }
}