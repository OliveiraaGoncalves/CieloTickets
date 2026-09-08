package br.com.cielotickets.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.cielotickets.feature.history.navigation.HistoryRoute
import br.com.cielotickets.feature.history.presentation.HistoryScreen
import br.com.cielotickets.feature.home.presentation.HomeScreen
import br.com.cielotickets.feature.payment.navigation.PaymentRoute
import br.com.cielotickets.feature.payment.presentation.PaymentScreen
import br.com.cielotickets.feature.receipt.navigation.ReceiptRoute
import br.com.cielotickets.feature.receipt.presentation.ReceiptScreen
import br.com.cielotickets.feature.ticketselection.navigation.TicketSelectionRoute
import br.com.cielotickets.feature.ticketselection.presentation.TicketSelectionScreen

/**
 * NavHost fica no :app de propósito — é o único módulo que pode importar
 * telas de TODAS as features. Cada feature não conhece a existência da
 * outra (Home não importa Payment, etc.), a navegação é quem faz a ponte.
 *
 * Rotas tipadas (`@Serializable`, ver `CieloRoutes.kt` e o pacote
 * `navigation/` de cada feature) em vez de string solta: nenhum dado de
 * domínio fica guardado aqui (nem em `remember`, nem em campo nenhum) — cada
 * tela recarrega o que precisa sozinha a partir do `SavedStateHandle` da
 * própria rota, o que é o que permite o app sobreviver a `process death` no
 * meio do fluxo de compra (ver docs/ARCHITECTURE.md).
 */
@Composable
fun CieloNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            HomeScreen(
                onEventSelected = { event -> navController.navigate(TicketSelectionRoute(event.id)) },
                onHistoryClick = { navController.navigate(HistoryRoute) }
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable<TicketSelectionRoute> {
            TicketSelectionScreen(
                onConfirm = { eventId, quantity -> navController.navigate(PaymentRoute(eventId, quantity)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<PaymentRoute> {
            PaymentScreen(
                onApproved = { receipt ->
                    // Tira "TicketSelectionRoute"/"PaymentRoute" da pilha: depois de
                    // aprovado não faz sentido voltar pra dentro do fluxo de compra
                    // (e evita o Payment reentrar em composição com o ViewModel
                    // ainda em Success, o que reempilharia "Receipt" a cada volta).
                    navController.navigate(ReceiptRoute(receipt.idempotencyKey)) {
                        popUpTo<Home>()
                    }
                },
                // Cancelada/timeout: "retorno seguro pro resumo da compra
                // mantendo o estado anterior" — volta pra TicketSelectionRoute
                // (que nunca saiu do back stack, então mantém a quantidade
                // escolhida) em vez de ficar na tela de pagamento oferecendo
                // retry, como acontece em "negada".
                onCancelled = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ReceiptRoute> {
            ReceiptScreen(
                onDone = {
                    navController.navigate(Home) {
                        popUpTo<Home> { inclusive = true }
                    }
                }
            )
        }
    }
}
