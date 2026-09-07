package br.com.cielotickets.core.paymentcielo

import android.content.Context
import android.content.Intent
import br.com.cielotickets.core.paymentcielo.deeplink.CieloCheckoutRequestPayload
import br.com.cielotickets.core.paymentcielo.deeplink.CieloDeeplinkCodec
import br.com.cielotickets.core.paymentcielo.deeplink.CieloDeeplinkResultBus
import br.com.cielotickets.core.paymentcielo.deeplink.CieloItemPayload
import br.com.cielotickets.core.paymentcielo.deeplink.CieloResponseActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Implementação REAL via integração Deeplink com a Cielo Smart, conforme
 * https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local —
 * a mesma que roda contra o emulador Cielo Smart exigido pelo case, sem
 * depender de nenhum .aar publicado fora do Maven público.
 *
 * Fluxo:
 * 1. Monta o JSON de checkout (reference = idempotencyKey do nosso pedido).
 * 2. Base64 + `Intent(ACTION_VIEW, "lio://payment?...")` — abre a Cielo
 *    Smart/emulador em outro app/tela.
 * 3. Suspende aguardando o [CieloDeeplinkResultBus], que é alimentado pela
 *    [CieloResponseActivity] quando a Cielo Smart devolve o controle via
 *    `order://response`.
 * 4. Mapeia o JSON de volta para [CieloChargeResult], sem vazar o formato
 *    específico da Cielo para o restante do app (só quem conhece esse
 *    contrato é este arquivo + os models em `deeplink/`).
 */
class CieloDeeplinkPaymentGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsProvider: CieloCredentialsProvider
) : CieloPaymentGateway {

    private val timeoutMs = 5 * 60_000L // 5 min — cliente pode demorar a inserir o cartão

    override suspend fun charge(request: CieloChargeRequest): CieloChargeResult {
        val credentials = credentialsProvider.get()

        val payload = CieloCheckoutRequestPayload(
            accessToken = credentials.accessToken,
            clientID = credentials.clientId,
            reference = request.idempotencyKey,
            installments = 0,
            items = listOf(
                CieloItemPayload(
                    name = request.orderDescription,
                    quantity = 1,
                    sku = request.idempotencyKey.take(20),
                    unitOfMeasure = "unidade",
                    unitPrice = request.amountCents.toInt()
                )
            ),
            // paymentCode omitido de propósito — a Cielo Smart pergunta a
            // forma de pagamento (crédito à vista, parcelado, débito...) na
            // própria tela da maquininha, respeitando o que estiver
            // habilitado nas credenciais do lojista. Ver CieloDeeplinkModels.kt.
            paymentCode = null,
            value = request.amountCents.toString()
        )

        val checkoutUri = CieloDeeplinkCodec.buildCheckoutUri(payload)

        val launched = try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, checkoutUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            false
        }

        if (!launched) {
            return CieloChargeResult.IntegrationError(
                IllegalStateException("Não foi possível abrir a Cielo Smart. O app/emulador está instalado?")
            )
        }

        val event = withTimeoutOrNull(timeoutMs) {
            CieloDeeplinkResultBus.events
                .filter { it.reference == request.idempotencyKey || it.reference == CieloResponseActivity.PENDING_REFERENCE_WILDCARD }
                .first()
        } ?: return CieloChargeResult.Timeout

        return when (val decoded = event.response) {
            is CieloDeeplinkCodec.DecodedResponse.Success -> {
                val payment = decoded.order.payments.firstOrNull()
                CieloChargeResult.Approved(
                    cieloTransactionId = decoded.order.id ?: request.idempotencyKey,
                    nsu = payment?.cieloCode
                )
            }
            is CieloDeeplinkCodec.DecodedResponse.Error -> {
                // code=1 é o código documentado para "cancelado pelo usuário".
                if (decoded.error.code == 1) {
                    CieloChargeResult.CancelledByUser
                } else {
                    CieloChargeResult.Denied(
                        reasonCode = decoded.error.code.toString(),
                        message = decoded.error.reason
                    )
                }
            }
            CieloDeeplinkCodec.DecodedResponse.Malformed -> CieloChargeResult.IntegrationError(
                IllegalStateException("Retorno da Cielo Smart em formato inesperado")
            )
        }
    }
}
