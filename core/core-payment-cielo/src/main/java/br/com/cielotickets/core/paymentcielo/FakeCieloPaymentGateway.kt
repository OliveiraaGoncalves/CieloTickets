package br.com.cielotickets.core.paymentcielo

import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Implementação usada com o EMULADOR Cielo Smart / desenvolvimento sem POS
 * físico. Troque o binding no PaymentModule pela implementação real que
 * embrulha o SDK nativo da Cielo Lio (ver docs/AI_USAGE.md sobre como a IA
 * foi usada para desenhar essa borda de integração).
 */
class FakeCieloPaymentGateway : CieloPaymentGateway {
    override suspend fun charge(request: CieloChargeRequest): CieloChargeResult {
        delay(1500) // simula latência do POS
        return CieloChargeResult.Approved(
            cieloTransactionId = UUID.randomUUID().toString(),
            nsu = (100000..999999).random().toString()
        )
    }
}
