package br.com.cielotickets.feature.history.domain

import br.com.cielotickets.core.common.NoParamsUseCase

interface GetPurchaseHistoryUseCase : NoParamsUseCase<List<PurchaseHistoryItem>>
