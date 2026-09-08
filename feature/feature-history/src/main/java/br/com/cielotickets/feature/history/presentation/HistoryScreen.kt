package br.com.cielotickets.feature.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.designsystem.AppTopBar
import br.com.cielotickets.core.designsystem.FullScreenError
import br.com.cielotickets.core.designsystem.FullScreenLoading
import br.com.cielotickets.core.designsystem.LabeledValueRow
import br.com.cielotickets.core.designsystem.SectionCard
import br.com.cielotickets.core.designsystem.Spacing
import br.com.cielotickets.feature.history.R
import br.com.cielotickets.feature.history.domain.PurchaseHistoryItem
import java.time.Instant
import java.time.ZoneId

@Composable
fun HistoryScreen(onBack: () -> Unit = {}, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.history_title), onBack = onBack) }) { innerPadding ->
        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(
                message = stringResource(R.string.history_loading),
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is UiState.Error -> FullScreenError(
                message = stringResource(s.messageRes),
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                onRetry = viewModel::load
            )
            is UiState.Success -> if (s.data.isEmpty()) {
                FullScreenError(
                    message = stringResource(R.string.history_empty),
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(s.data, key = { it.idempotencyKey }) { item -> HistoryItemCard(item) }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: PurchaseHistoryItem) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.eventTitle, style = MaterialTheme.typography.titleMedium)
            StatusChip(item.status)
        }
        Text(
            text = item.createdAtEpochMs.toFriendlyDateTime(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
        LabeledValueRow(stringResource(R.string.history_label_tickets), item.ticketQuantity.toString())
        LabeledValueRow(
            label = stringResource(R.string.history_label_total),
            value = item.totalAmountCents.centsToBrl(),
            emphasize = true
        )
    }
}

@Composable
private fun StatusChip(status: PurchaseStatus) {
    val scheme = MaterialTheme.colorScheme
    val (labelRes, container, onContainer) = when (status) {
        PurchaseStatus.APPROVED -> Triple(R.string.history_status_approved, scheme.tertiaryContainer, scheme.onTertiaryContainer)
        PurchaseStatus.DENIED -> Triple(R.string.history_status_denied, scheme.errorContainer, scheme.onErrorContainer)
        PurchaseStatus.CANCELLED -> Triple(R.string.history_status_cancelled, scheme.surfaceVariant, scheme.onSurfaceVariant)
        PurchaseStatus.PENDING, PurchaseStatus.ERROR -> Triple(R.string.history_status_pending, scheme.errorContainer, scheme.onErrorContainer)
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = onContainer,
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = Spacing.sm, vertical = 4.dp)
    )
}

/** `createdAtEpochMs` é epoch millis (não ISO-8601 como o `dateTimeIso` de [EventModel][br.com.cielotickets.core.common.EventModel]) — formata direto, sem passar por `toFriendlyDateTime()` de String. */
private fun Long.toFriendlyDateTime(): String {
    val dateTime = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.toString().replace('T', ' ')
}
