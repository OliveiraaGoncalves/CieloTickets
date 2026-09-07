package br.com.cielotickets.feature.payment.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.designsystem.AppTopBar
import br.com.cielotickets.core.designsystem.FullScreenError
import br.com.cielotickets.core.designsystem.FullScreenLoading
import br.com.cielotickets.core.designsystem.LabeledValueRow
import br.com.cielotickets.core.designsystem.PrimaryButton
import br.com.cielotickets.core.designsystem.SecondaryButton
import br.com.cielotickets.core.designsystem.SectionCard
import br.com.cielotickets.core.designsystem.Spacing
import br.com.cielotickets.feature.payment.R
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt

/**
 * Só recebe `onApproved`/`onCancelled`/`onBack` — o [PurchaseOrder][br.com.cielotickets.feature.payment.domain.PurchaseOrder]
 * vem do [PaymentViewModel], reconstruído a partir do `eventId`+`quantity`
 * da rota (ver docstring do ViewModel pra o porquê disso importar pra
 * sobrevivência a `process death`).
 */
@Composable
fun PaymentScreen(
    onApproved: (PurchaseReceipt) -> Unit,
    onCancelled: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val order = screenState.order

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.payment_title), onBack = onBack) }) { innerPadding ->
        when {
            screenState.orderLoadFailed -> FullScreenError(
                message = stringResource(R.string.payment_load_error),
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            order == null -> FullScreenLoading(modifier = Modifier.padding(innerPadding).fillMaxSize())
            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(Spacing.md)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SectionCard {
                    LabeledValueRow(label = stringResource(R.string.payment_label_event), value = order.eventTitle)
                    HorizontalDivider()
                    LabeledValueRow(label = stringResource(R.string.payment_label_tickets), value = order.ticketQuantity.toString())
                    LabeledValueRow(
                        label = stringResource(R.string.payment_label_total),
                        value = order.totalAmountCents.centsToBrl(),
                        emphasize = true
                    )
                }

                when (val s = screenState.paymentState) {
                    is PaymentUiState.Idle -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.payment_idle_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(text = stringResource(R.string.payment_cta), onClick = viewModel::pay)
                    }

                    is PaymentUiState.Processing -> Column(modifier = Modifier.fillMaxSize()) {
                        FullScreenLoading(
                            message = stringResource(R.string.payment_processing),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.payment_cancel_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )
                        SecondaryButton(text = stringResource(R.string.payment_cancel_button), onClick = viewModel::cancelWaiting)
                    }

                    is PaymentUiState.Success -> {
                        LaunchedEffect(s.receipt) { onApproved(s.receipt) }
                        FullScreenLoading(
                            message = stringResource(R.string.payment_approved_generating_receipt),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is PaymentUiState.Failed -> FullScreenError(
                        message = stringResource(s.messageRes),
                        modifier = Modifier.fillMaxSize(),
                        onRetry = viewModel::pay
                    )

                    is PaymentUiState.Cancelled -> {
                        LaunchedEffect(s) { onCancelled() }
                        FullScreenError(
                            message = stringResource(s.messageRes),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
