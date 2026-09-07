package br.com.cielotickets.core.paymentcielo.deeplink

/**
 * Ponte entre a `ResponseActivity` (que recebe o callback de outro app —
 * a Cielo Smart/emulador — via Intent) e o `CieloDeeplinkPaymentGateway`
 * (que está suspenso esperando o resultado dentro de uma coroutine).
 *
 * É um singleton de processo porque o callback chega numa Activity nova,
 * desacoplada da tela que iniciou o pagamento — não dá pra devolver o
 * resultado por retorno de função normal.
 */
object CieloDeeplinkResultBus {

    data class Event(val reference: String, val response: CieloDeeplinkCodec.DecodedResponse)

    private val flow = kotlinx.coroutines.flow.MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 8
    )

    val events: kotlinx.coroutines.flow.SharedFlow<Event> = flow

    /** Chamado pela ResponseActivity ao receber o callback. Não suspende. */
    fun publish(reference: String, response: CieloDeeplinkCodec.DecodedResponse) {
        flow.tryEmit(Event(reference, response))
    }
}
