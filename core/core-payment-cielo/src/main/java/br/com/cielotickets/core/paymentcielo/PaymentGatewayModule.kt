package br.com.cielotickets.core.paymentcielo

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binding único do gateway de pagamento — trocar entre a implementação
 * real (deeplink) e [FakeCieloPaymentGateway] (dev/CI sem o emulador
 * instalado) é mudar SÓ o alvo do `@Binds` abaixo, sem tocar em
 * feature-payment.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentGatewayModule {

    @Binds
    abstract fun bindCredentialsProvider(impl: DevCieloCredentialsProvider): CieloCredentialsProvider

    @Binds
    abstract fun bindPaymentGateway(impl: CieloDeeplinkPaymentGateway): CieloPaymentGateway
}
