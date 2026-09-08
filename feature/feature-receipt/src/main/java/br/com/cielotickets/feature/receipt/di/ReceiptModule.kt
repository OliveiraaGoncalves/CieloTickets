package br.com.cielotickets.feature.receipt.di

import br.com.cielotickets.feature.receipt.data.ReceiptRepositoryImpl
import br.com.cielotickets.feature.receipt.domain.GetReceiptUseCase
import br.com.cielotickets.feature.receipt.domain.GetReceiptUseCaseImpl
import br.com.cielotickets.feature.receipt.domain.ReceiptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReceiptModule {
    @Binds
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository

    @Binds
    abstract fun bindGetReceiptUseCase(impl: GetReceiptUseCaseImpl): GetReceiptUseCase
}
