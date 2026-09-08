package br.com.cielotickets.feature.ticketselection.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.common.toFriendlyDateTime
import br.com.cielotickets.core.designsystem.AppTopBar
import br.com.cielotickets.core.designsystem.FullScreenError
import br.com.cielotickets.core.designsystem.FullScreenLoading
import br.com.cielotickets.core.designsystem.LabeledValueRow
import br.com.cielotickets.core.designsystem.PrimaryButton
import br.com.cielotickets.core.designsystem.SectionCard
import br.com.cielotickets.core.designsystem.Spacing
import br.com.cielotickets.feature.ticketselection.R

/**
 * Requisito funcional 2: "Selecionar a quantidade de ingressos".
 *
 * Só recebe `onConfirm(eventId, quantity)`/`onBack` — o [EventModel][br.com.cielotickets.core.common.EventModel]
 * em si vem do [TicketSelectionViewModel], carregado a partir do `eventId`
 * da rota (ver docstring do ViewModel pra o porquê).
 */
@Composable
fun TicketSelectionScreen(
    onConfirm: (String, Int) -> Unit,
    onBack: () -> Unit = {},
    viewModel: TicketSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event = state.event

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.ticket_selection_title), onBack = onBack) },
        bottomBar = {
            if (event != null) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        Modifier
                            .padding(Spacing.md)
                            .navigationBarsPadding()
                    ) {
                        LabeledValueRow(
                            label = stringResource(R.string.ticket_selection_total),
                            value = state.totalCents.centsToBrl(),
                            emphasize = true
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        PrimaryButton(
                            text = stringResource(R.string.ticket_selection_cta),
                            onClick = { onConfirm(event.id, state.quantity) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            state.loadFailed -> FullScreenError(
                message = stringResource(R.string.ticket_selection_load_error),
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            event == null -> FullScreenLoading(modifier = Modifier.padding(innerPadding).fillMaxSize())
            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SectionCard {
                    Text(text = event.title, style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(Spacing.md))
                        Text(event.venue, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(Spacing.md))
                        Text(event.dateTimeIso.toFriendlyDateTime(), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionCard(title = stringResource(R.string.ticket_selection_quantity_title)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = viewModel::decrement, enabled = state.quantity > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.ticket_selection_decrement_cd))
                        }
                        Text(text = state.quantity.toString(), style = MaterialTheme.typography.headlineMedium)
                        FilledTonalIconButton(
                            onClick = viewModel::increment,
                            enabled = state.quantity < event.availableTickets
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ticket_selection_increment_cd))
                        }
                    }

                    HorizontalDivider()

                    LabeledValueRow(label = stringResource(R.string.ticket_selection_unit_price), value = event.priceCents.centsToBrl())
                    Text(
                        text = pluralStringResource(
                            R.plurals.ticket_selection_available,
                            event.availableTickets,
                            event.availableTickets
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
