package br.com.cielotickets.feature.payment.di

import br.com.cielotickets.feature.payment.data.PurchaseRepositoryImpl
import br.com.cielotickets.feature.payment.domain.ProcessPaymentUseCase
import br.com.cielotickets.feature.payment.domain.ProcessPaymentUseCaseImpl
import br.com.cielotickets.feature.payment.domain.PurchaseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {
    @Binds
    abstract fun bindPurchaseRepository(impl: PurchaseRepositoryImpl): PurchaseRepository

    @Binds
    abstract fun bindProcessPaymentUseCase(impl: ProcessPaymentUseCaseImpl): ProcessPaymentUseCase
}
