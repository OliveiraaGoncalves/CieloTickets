package br.com.cielotickets.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.common.toFriendlyDateTime
import br.com.cielotickets.core.designsystem.AppTopBar
import br.com.cielotickets.core.designsystem.FullScreenError
import br.com.cielotickets.core.designsystem.FullScreenLoading
import br.com.cielotickets.core.designsystem.InfoBadge
import br.com.cielotickets.core.designsystem.Spacing
import br.com.cielotickets.feature.home.R
import br.com.cielotickets.feature.home.domain.Event

/** Requisito funcional 1: "Visualizar eventos disponíveis para compra". */
@Composable
fun HomeScreen(
    onEventSelected: (Event) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.home_title)) }) { innerPadding ->
        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(
                message = stringResource(R.string.home_loading),
                modifier = Modifier.padding(innerPadding)
            )
            is UiState.Error -> FullScreenError(
                message = stringResource(s.messageRes),
                modifier = Modifier.padding(innerPadding),
                onRetry = viewModel::load
            )
            is UiState.Success -> if (s.data.isEmpty()) {
                FullScreenError(
                    message = stringResource(R.string.home_empty),
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .testTag("home_event_list"),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(s.data, key = { it.id }) { event ->
                        EventCard(event = event, onClick = { onEventSelected(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ConfirmationNumber,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = event.venue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = event.dateTimeIso.toFriendlyDateTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_price_from, event.priceCents.centsToBrl()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    InfoBadge(
                        text = pluralStringResource(
                            R.plurals.home_available_badge,
                            event.availableTickets,
                            event.availableTickets
                        )
                    )
                }
            }
        }
    }
}
