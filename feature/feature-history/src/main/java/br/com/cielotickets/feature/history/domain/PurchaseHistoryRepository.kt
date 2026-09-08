package br.com.cielotickets.feature.history.domain

import br.com.cielotickets.core.common.AppResult

interface PurchaseHistoryRepository {
    suspend fun getHistory(): AppResult<List<PurchaseHistoryItem>>
}
