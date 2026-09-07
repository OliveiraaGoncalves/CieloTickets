package br.com.cielotickets.core.paymentcielo.deeplink

import android.net.Uri
import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Monta a URI de checkout ("lio://payment?...") e decodifica o callback
 * de retorno ("order://response?response=BASE64"), conforme documentado
 * no sample oficial da Cielo (ver core-payment-cielo/README interno em
 * docs/ARCHITECTURE.md).
 */
object CieloDeeplinkCodec {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Host/scheme do nosso callback — DEVEM bater com o AndroidManifest. */
    const val CALLBACK_SCHEME = "order"
    const val CALLBACK_HOST = "response"

    fun buildCheckoutUri(payload: CieloCheckoutRequestPayload): Uri {
        val jsonString = json.encodeToString(CieloCheckoutRequestPayload.serializer(), payload)
        val base64Request = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val urlCallback = "$CALLBACK_SCHEME://$CALLBACK_HOST"
        return Uri.parse("lio://payment?request=$base64Request&urlCallback=$urlCallback")
    }

    /**
     * Resultado bruto e já desambiguado (sucesso vs erro) de um callback
     * recebido em [br.com.cielotickets.core.paymentcielo.deeplink.CieloResponseActivity].
     */
    sealed class DecodedResponse {
        data class Success(val order: CieloOrderResponse) : DecodedResponse()
        data class Error(val error: CieloErrorResponse) : DecodedResponse()
        data object Malformed : DecodedResponse()
    }

    fun decodeCallback(callbackUri: Uri): DecodedResponse {
        val encoded = callbackUri.getQueryParameter("response") ?: return DecodedResponse.Malformed
        val decodedJson = try {
            String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            return DecodedResponse.Malformed
        }

        // O payload de erro/cancelamento é {"code":..., "reason":...} — como
        // CieloOrderResponse só tem campos opcionais, tentar decodificá-lo
        // primeiro "engolia" o payload de erro sem lançar exceção (virava um
        // Success vazio classificado como Malformed). Decidir pelo shape do
        // JSON em vez de tentativa-e-erro evita essa ambiguidade.
        return try {
            val root = json.parseToJsonElement(decodedJson).jsonObject
            if (root.containsKey("code")) {
                DecodedResponse.Error(json.decodeFromJsonElement(CieloErrorResponse.serializer(), root))
            } else {
                DecodedResponse.Success(json.decodeFromJsonElement(CieloOrderResponse.serializer(), root))
            }
        } catch (e: Exception) {
            DecodedResponse.Malformed
        }
    }
}
