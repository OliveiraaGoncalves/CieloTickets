package br.com.cielotickets.core.paymentcielo

/**
 * Porta (interface de domínio) para o gateway de pagamento. A feature de
 * pagamento depende SÓ desta interface — nunca do SDK da Cielo diretamente.
 * Isso isola o app da API específica da Cielo Smart e permite trocar a
 * implementação real por [FakeCieloPaymentGateway] em testes/emulador.
 */
interface CieloPaymentGateway {
    /**
     * Inicia uma cobrança na Cielo Lio.
     * [idempotencyKey] é a mesma chave gravada em PurchaseAttempt — se a
     * transação já tiver sido processada com essa chave, a implementação
     * real deve tratar como retry seguro (não cobrar de novo).
     */
    suspend fun charge(request: CieloChargeRequest): CieloChargeResult
}

data class CieloChargeRequest(
    val idempotencyKey: String,
    val amountCents: Long,
    val orderDescription: String
)

sealed class CieloChargeResult {
    data class Approved(val cieloTransactionId: String, val nsu: String?) : CieloChargeResult()
    data class Denied(val reasonCode: String?, val message: String?) : CieloChargeResult()
    data object CancelledByUser : CieloChargeResult()
    data object Timeout : CieloChargeResult()
    data class IntegrationError(val cause: Throwable) : CieloChargeResult()
}
