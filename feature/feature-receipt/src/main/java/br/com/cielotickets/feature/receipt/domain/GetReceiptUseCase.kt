package br.com.cielotickets.feature.receipt.domain

import br.com.cielotickets.core.common.PurchaseReceiptModel
import br.com.cielotickets.core.common.UseCase

interface GetReceiptUseCase : UseCase<String, PurchaseReceiptModel>
