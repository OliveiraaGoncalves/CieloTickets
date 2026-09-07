package br.com.cielotickets.core.paymentcielo.deeplink

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Activity "invisível" que recebe o callback da Cielo Smart / emulador
 * após o fluxo de pagamento (ver AndroidManifest deste módulo — o filtro
 * é action=VIEW, data scheme="order" host="response", casando com a
 * urlCallback montada em [CieloDeeplinkCodec.buildCheckoutUri]).
 *
 * Ela não desenha UI: decodifica o resultado, publica no
 * [CieloDeeplinkResultBus] e fecha, devolvendo o usuário para a Activity
 * anterior do próprio app (que o Android já mantém na back stack).
 */
class CieloResponseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (Intent.ACTION_VIEW != intent.action) return

        when (val decoded = CieloDeeplinkCodec.decodeCallback(uri)) {
            is CieloDeeplinkCodec.DecodedResponse.Success -> {
                val reference = decoded.order.reference.orEmpty()
                CieloDeeplinkResultBus.publish(reference, decoded)
            }
            is CieloDeeplinkCodec.DecodedResponse.Error -> {
                // O payload de erro não traz a reference de volta — o
                // gateway trata isso fazendo broadcast para QUALQUER
                // chamada pendente (só há uma por vez na prática, já que
                // a PaymentViewModel bloqueia reenvio durante Processing).
                CieloDeeplinkResultBus.publish(PENDING_REFERENCE_WILDCARD, decoded)
            }
            CieloDeeplinkCodec.DecodedResponse.Malformed -> Unit
        }
    }

    companion object {
        const val PENDING_REFERENCE_WILDCARD = "*"
    }
}
