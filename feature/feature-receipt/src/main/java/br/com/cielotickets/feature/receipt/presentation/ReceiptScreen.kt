package br.com.cielotickets.feature.receipt.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.cielotickets.core.common.UiState
import br.com.cielotickets.core.common.centsToBrl
import br.com.cielotickets.core.designsystem.AppTopBar
import br.com.cielotickets.core.designsystem.DashedDivider
import br.com.cielotickets.core.designsystem.FullScreenError
import br.com.cielotickets.core.designsystem.FullScreenLoading
import br.com.cielotickets.core.designsystem.LabeledValueRow
import br.com.cielotickets.core.designsystem.SecondaryButton
import br.com.cielotickets.core.designsystem.SectionCard
import br.com.cielotickets.core.designsystem.Spacing
import br.com.cielotickets.feature.payment.domain.PurchaseReceipt
import br.com.cielotickets.feature.payment.domain.PurchaseStatus
import br.com.cielotickets.feature.receipt.R

/**
 * Requisito funcional 5: "Exibir comprovante/resumo da compra".
 *
 * Só recebe `onDone` — o [PurchaseReceipt] vem do [ReceiptViewModel],
 * reconstruído a partir da `idempotencyKey` da rota (sobrevive a
 * `process death`, ver docstring do ViewModel).
 */
@Composable
fun ReceiptScreen(onDone: () -> Unit = {}, viewModel: ReceiptViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.receipt_title)) }) { innerPadding ->
        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(modifier = Modifier.padding(innerPadding).fillMaxSize())
            is UiState.Error -> FullScreenError(
                message = stringResource(s.messageRes),
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is UiState.Success -> ReceiptContent(receipt = s.data, onDone = onDone, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ReceiptContent(receipt: PurchaseReceipt, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        StatusBanner(receipt.status)

        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SectionCard {
                LabeledValueRow(stringResource(R.string.receipt_label_event), receipt.order.eventTitle)
                LabeledValueRow(stringResource(R.string.receipt_label_tickets), receipt.order.ticketQuantity.toString())
                LabeledValueRow(stringResource(R.string.receipt_label_total), receipt.order.totalAmountCents.centsToBrl())
                receipt.cieloTransactionId?.let { LabeledValueRow(stringResource(R.string.receipt_label_transaction), it) }
            }

            if (receipt.status == PurchaseStatus.APPROVED) {
                SectionCard {
                    DashedDivider()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        val qr = remember(receipt.idempotencyKey) {
                            QrCodeGenerator.generate("cielotickets://ticket/${receipt.idempotencyKey}")
                        }
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = stringResource(R.string.receipt_qr_content_description),
                            modifier = Modifier
                                .size(200.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(Spacing.sm)
                        )
                        Text(
                            text = stringResource(R.string.receipt_qr_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            SecondaryButton(text = stringResource(R.string.receipt_done_button), onClick = onDone)
        }
    }
}

@Composable
private fun StatusBanner(status: PurchaseStatus) {
    val scheme = MaterialTheme.colorScheme
    val visual = statusVisuals(status, scheme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(visual.container)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(visual.icon, contentDescription = null, tint = visual.onContainer, modifier = Modifier.size(48.dp))
        Text(text = stringResource(visual.labelRes), style = MaterialTheme.typography.headlineSmall, color = visual.onContainer)
    }
}

private data class StatusVisual(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
    val container: Color,
    val onContainer: Color
)

private fun statusVisuals(status: PurchaseStatus, scheme: ColorScheme): StatusVisual = when (status) {
    PurchaseStatus.APPROVED -> StatusVisual(Icons.Filled.CheckCircle, R.string.receipt_status_approved, scheme.tertiaryContainer, scheme.onTertiaryContainer)
    PurchaseStatus.DENIED -> StatusVisual(Icons.Filled.Cancel, R.string.receipt_status_denied, scheme.errorContainer, scheme.onErrorContainer)
    PurchaseStatus.CANCELLED -> StatusVisual(Icons.Filled.Cancel, R.string.receipt_status_cancelled, scheme.surfaceVariant, scheme.onSurfaceVariant)
    PurchaseStatus.PENDING, PurchaseStatus.ERROR -> StatusVisual(Icons.Filled.ErrorOutline, R.string.receipt_status_pending, scheme.errorContainer, scheme.onErrorContainer)
}
