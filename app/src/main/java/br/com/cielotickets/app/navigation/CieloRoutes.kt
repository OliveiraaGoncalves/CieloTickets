package br.com.cielotickets.app.navigation

import kotlinx.serialization.Serializable

/**
 * Rota tipada (Navigation-Compose 2.8+) do destino inicial. As demais rotas
 * (com parâmetros) são donas de cada feature module — `:app` depende de
 * TODAS as features, mas o inverso nunca pode acontecer, então uma rota que
 * uma feature precisa ler do próprio `SavedStateHandle` tem que morar dentro
 * dela mesma (ver `TicketSelectionRoute`, `PaymentRoute`, `ReceiptRoute`).
 * `Home` não carrega parâmetro nenhum e ninguém fora do NavHost precisa
 * dela, então fica aqui sem problema.
 */
@Serializable
data object Home
