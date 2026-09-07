package br.com.cielotickets.core.paymentcielo.deeplink

import kotlinx.serialization.Serializable

/**
 * Espelha o contrato JSON documentado em
 * https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local
 * (seção "Pagamento" da Integração via Deeplink).
 *
 * [reference] é a nossa idempotencyKey — é o campo que a Cielo devolve
 * de volta no JSON de resposta, e é o que usamos para correlacionar a
 * resposta assíncrona (troca de app) com o pedido que originou a chamada.
 *
 * [paymentCode] é opcional de propósito: omitido (`null`, e por causa de
 * `explicitNulls = false` nem entra no JSON), a própria Cielo Smart
 * pergunta ao cliente a forma de pagamento (crédito à vista, parcelado,
 * débito — o que estiver habilitado nas credenciais do lojista) na tela da
 * maquininha. Mandar um valor fixo arrisca erro se esse método específico
 * não estiver habilitado para o lojista.
 */
@Serializable
data class CieloCheckoutRequestPayload(
    val accessToken: String,
    val clientID: String,
    val reference: String,
    val email: String? = null,
    val installments: Int = 0,
    val items: List<CieloItemPayload>,
    val paymentCode: String? = null,
    val value: String
)

@Serializable
data class CieloItemPayload(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    val unitPrice: Int
)

/** Resposta de sucesso (aprovada) — subconjunto dos campos que usamos. */
@Serializable
data class CieloOrderResponse(
    val id: String? = null,
    val reference: String? = null,
    val status: String? = null,
    val paidAmount: Long? = null,
    val pendingAmount: Long? = null,
    val price: Long? = null,
    val payments: List<CieloPaymentResponse> = emptyList()
)

@Serializable
data class CieloPaymentResponse(
    val authCode: String? = null,
    val cieloCode: String? = null, // NSU
    val brand: String? = null,
    val amount: Long? = null
)

/** Resposta de erro/cancelamento — {"code":1,"reason":"CANCELADO PELO USUÁRIO"}. */
@Serializable
data class CieloErrorResponse(
    val code: Int,
    val reason: String
)
