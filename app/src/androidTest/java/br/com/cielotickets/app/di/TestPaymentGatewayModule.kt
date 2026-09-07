package br.com.cielotickets.app.di

import br.com.cielotickets.core.paymentcielo.CieloChargeRequest
import br.com.cielotickets.core.paymentcielo.CieloChargeResult
import br.com.cielotickets.core.paymentcielo.CieloCredentialsProvider
import br.com.cielotickets.core.paymentcielo.CieloPaymentGateway
import br.com.cielotickets.core.paymentcielo.DevCieloCredentialsProvider
import br.com.cielotickets.core.paymentcielo.PaymentGatewayModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Substitui [PaymentGatewayModule] nos testes instrumentados: a implementação
 * real abre a Cielo Smart via Deeplink e espera um callback físico, o que
 * trava qualquer teste automatizado. [ControllableFakePaymentGateway] deixa
 * cada `@Test` decidir o resultado (aprovado/negado/cancelado) sem depender
 * do emulador da Cielo instalado no dispositivo/CI.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PaymentGatewayModule::class])
abstract class TestPaymentGatewayModule {

    @Binds
    abstract fun bindCredentialsProvider(impl: DevCieloCredentialsProvider): CieloCredentialsProvider

    @Binds
    @Singleton
    abstract fun bindPaymentGateway(impl: ControllableFakePaymentGateway): CieloPaymentGateway
}

/**
 * `@Singleton` garante que o mesmo objeto injetado no app (via [CieloPaymentGateway])
 * também é o que o teste enxerga ao injetar esta classe concreta — é assim
 * que um `@Test` reconfigura [nextResult] antes de tocar em "Pagar".
 */
@Singleton
class ControllableFakePaymentGateway @Inject constructor() : CieloPaymentGateway {

    @Volatile
    var nextResult: CieloChargeResult = CieloChargeResult.Approved(
        cieloTransactionId = "test-tx-id",
        nsu = "123456"
    )

    /** Delay pequeno o bastante pra não deixar os testes lentos, mas real o
     * suficiente pra dar tempo de a UI mostrar "Processando..." e o teste
     * poder interagir com ela (ex: tocar em "Cancelar") antes da resposta. */
    @Volatile
    var responseDelayMs: Long = 300

    override suspend fun charge(request: CieloChargeRequest): CieloChargeResult {
        delay(responseDelayMs)
        return nextResult
    }
}
