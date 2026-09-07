package br.com.cielotickets.feature.receipt.di

import br.com.cielotickets.feature.receipt.data.ReceiptRepositoryImpl
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
}
