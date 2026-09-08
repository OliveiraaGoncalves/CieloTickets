package br.com.cielotickets.feature.payment.domain

interface PurchaseRepository {
    suspend fun findAttempt(idempotencyKey: String): PurchaseAttemptModel?
    suspend fun insertAttempt(attempt: PurchaseAttemptModel)
    suspend fun updateAttempt(attempt: PurchaseAttemptModel)
}
