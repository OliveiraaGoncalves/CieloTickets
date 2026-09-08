package br.com.cielotickets.feature.history.di

import br.com.cielotickets.feature.history.data.PurchaseHistoryRepositoryImpl
import br.com.cielotickets.feature.history.domain.GetPurchaseHistoryUseCase
import br.com.cielotickets.feature.history.domain.GetPurchaseHistoryUseCaseImpl
import br.com.cielotickets.feature.history.domain.PurchaseHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {
    @Binds
    abstract fun bindPurchaseHistoryRepository(impl: PurchaseHistoryRepositoryImpl): PurchaseHistoryRepository

    @Binds
    abstract fun bindGetPurchaseHistoryUseCase(impl: GetPurchaseHistoryUseCaseImpl): GetPurchaseHistoryUseCase
}
