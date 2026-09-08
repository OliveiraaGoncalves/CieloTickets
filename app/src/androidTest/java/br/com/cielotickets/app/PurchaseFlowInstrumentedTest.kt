package br.com.cielotickets.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.cielotickets.app.di.ControllableFakePaymentGateway
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.common.SeedEvents
import br.com.cielotickets.core.paymentcielo.CieloChargeResult
import br.com.cielotickets.core.paymentcielo.CieloPaymentGateway
import br.com.cielotickets.feature.payment.R as PaymentR
import br.com.cielotickets.feature.receipt.R as ReceiptR
import br.com.cielotickets.feature.ticketselection.R as TicketSelectionR
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Cobre a jornada de compra ponta a ponta (Home → Seleção → Pagamento →
 * Comprovante) contra os cenários da matriz de testes do case (CT-02 a
 * CT-05) e duas regressões reais encontradas via teste manual nesta mesma
 * sessão, que a matriz original não cobria:
 *  - pilha de navegação duplicando "receipt" ao voltar depois de aprovado;
 *  - cancelar durante "Processando..." precisa voltar pro resumo, não travar.
 *
 * [ControllableFakePaymentGateway] substitui a integração real (Deeplink pra
 * Cielo Smart) via `TestPaymentGatewayModule` — sem isso o teste dependeria
 * do emulador físico da Cielo instalado no dispositivo de CI.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PurchaseFlowInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var paymentGateway: CieloPaymentGateway

    private val fakeGateway get() = paymentGateway as ControllableFakePaymentGateway

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun ct02_selecionarQuantidade_recalculaTotalNoResumo() {
        val event = SeedEvents.all.first()
        abrirTelaDeSelecao(event)

        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_increment_cd)
        ).performClick()

        // 2 ingressos: total precisa refletir no resumo, não só no ViewModel.
        // (Não dá pra checar quantidade 1 por texto único — com qty=1 o total
        // é igual ao preço unitário, que também aparece na tela.)
        composeTestRule.onNodeWithText((event.priceCents * 2).centsToBrl()).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_increment_cd)
        ).performClick()

        composeTestRule.onNodeWithText((event.priceCents * 3).centsToBrl()).assertIsDisplayed()
    }

    @Test
    fun ct03_pagamentoAprovado_geraComprovanteEBackStackVoltaDireitoPraHome() {
        val event = SeedEvents.all.first()
        abrirTelaDePagamento(event, quantity = 1)
        fakeGateway.nextResult = CieloChargeResult.Approved(cieloTransactionId = "tx-approved", nsu = "999999")

        clicarPagar()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(ReceiptR.string.receipt_status_approved)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(ReceiptR.string.receipt_qr_content_description)
        ).assertIsDisplayed()

        // Regressão: antes da correção, voltar aqui reabria "payment" (que
        // reexecutava o efeito de sucesso e empilhava "receipt" de novo) em
        // vez de ir direto pra Home.
        Espresso.pressBack()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(event.title).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(PaymentR.string.payment_approved_generating_receipt)
        ).assertDoesNotExist()
    }

    @Test
    fun ct04_pagamentoNegado_exibeErroSemQuebrarEPermiteNovaTentativa() {
        val event = SeedEvents.all.first()
        abrirTelaDePagamento(event, quantity = 1)
        fakeGateway.nextResult = CieloChargeResult.Denied(reasonCode = "51", message = "saldo insuficiente")

        clicarPagar()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(PaymentR.string.payment_error_denied)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Muda o resultado ANTES de tentar de novo — prova que "tentar
        // novamente" dispara uma cobrança de verdade, não só reexibe o
        // resultado antigo (bug de idempotência já corrigido nesta sessão).
        fakeGateway.nextResult = CieloChargeResult.Approved(cieloTransactionId = "tx-retry-ok", nsu = "111111")
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(br.com.cielotickets.core.designsystem.R.string.action_retry)
        ).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(ReceiptR.string.receipt_status_approved)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun ct05_pagamentoBloqueiaReenvioAposPrimeiroToque() {
        val event = SeedEvents.all.first()
        abrirTelaDePagamento(event, quantity = 1)
        fakeGateway.responseDelayMs = 1_500 // dá tempo de checar o estado antes da resposta chegar

        clicarPagar()

        // O CTA "Pagar com Cielo Smart" precisa ter sumido — a troca de estado
        // (Idle → Processing) substitui o botão por um loading, então não tem
        // como um segundo toque físico disparar outra cobrança.
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(PaymentR.string.payment_cta)
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(PaymentR.string.payment_processing)
        ).assertIsDisplayed()
    }

    @Test
    fun cancelarDuranteProcessamento_voltaPraResumoMantendoQuantidade() {
        val event = SeedEvents.all.first()
        abrirTelaDeSelecao(event)
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_increment_cd)
        ).performClick() // quantidade = 2, precisa sobreviver ao cancelamento

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_cta)
        ).performClick()

        fakeGateway.responseDelayMs = 3_000
        clicarPagar()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(PaymentR.string.payment_cancel_button)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(PaymentR.string.payment_cancel_button)
        ).performClick()

        // "Retorno seguro pro resumo mantendo o estado anterior" (docs/desafio.md).
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_cta)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText((event.priceCents * 2).centsToBrl()).assertIsDisplayed()
    }

    private fun abrirTelaDeSelecao(event: br.com.cielotickets.core.common.EventModel) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(event.title).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(event.title).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_cta)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun abrirTelaDePagamento(event: br.com.cielotickets.core.common.EventModel, quantity: Int) {
        abrirTelaDeSelecao(event)
        repeat(quantity - 1) {
            composeTestRule.onNodeWithContentDescription(
                composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_increment_cd)
            ).performClick()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(TicketSelectionR.string.ticket_selection_cta)
        ).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(PaymentR.string.payment_cta)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clicarPagar() {
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(PaymentR.string.payment_cta)
        ).performClick()
    }
}
