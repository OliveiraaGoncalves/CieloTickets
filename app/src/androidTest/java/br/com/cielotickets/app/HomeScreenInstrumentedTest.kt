package br.com.cielotickets.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.common.toFriendlyDateTime
import br.com.cielotickets.core.common.SeedEvents
import br.com.cielotickets.feature.home.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CT-01 (docs/desafio.md): "Exibe eventos locais com título, data e preço
 * corretos." Roda contra o catálogo real de [SeedEvents] via
 * `TestLocalStorageModule` (Room em memória) — se o mapeamento
 * Entity → Domain quebrar um campo, esse teste sente, diferente de um teste
 * que hardcoda os valores esperados direto no próprio teste.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun listaEventos_exibeTituloDataEPrecoDoPrimeiroEvento() {
        val event = SeedEvents.all.first()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(event.title).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(event.dateTimeIso.toFriendlyDateTime()).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.home_price_from, event.priceCents.centsToBrl())
        ).assertIsDisplayed()
    }

    @Test
    fun listaEventos_carregaCatalogoCompletoDoRoom() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(SeedEvents.all.first().title).fetchSemanticsNodes().isNotEmpty()
        }

        // Rola até cada evento pra provar que o catálogo INTEIRO foi carregado
        // do Room (não só o primeiro item, que já apareceria sem rolar).
        SeedEvents.all.forEach { event ->
            composeTestRule.onNodeWithTag("home_event_list")
                .performScrollToNode(hasText(event.title))
            composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
        }
    }
}
