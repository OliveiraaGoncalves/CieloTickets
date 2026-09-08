package br.com.cielotickets.feature.payment.data

import br.com.cielotickets.core.common.PurchaseStatus
import br.com.cielotickets.core.localstorage.db.PurchaseAttempt
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.feature.payment.domain.PurchaseAttemptModel
import br.com.cielotickets.feature.payment.domain.PurchaseRepository
import javax.inject.Inject

class PurchaseRepositoryImpl @Inject constructor(
    private val dao: PurchaseAttemptDao
) : PurchaseRepository {
    override suspend fun findAttempt(idempotencyKey: String): PurchaseAttemptModel? =
        dao.findByKey(idempotencyKey)?.toDomain()

    override suspend fun insertAttempt(attempt: PurchaseAttemptModel) {
        dao.insert(attempt.toEntity())
    }

    override suspend fun updateAttempt(attempt: PurchaseAttemptModel) {
        dao.update(attempt.toEntity())
    }
}

private fun PurchaseAttempt.toDomain() = PurchaseAttemptModel(
    idempotencyKey = idempotencyKey,
    eventId = eventId,
    eventTitle = eventTitle,
    ticketQuantity = ticketQuantity,
    totalAmountCents = totalAmountCents,
    status = PurchaseStatus.valueOf(status),
    cieloTransactionId = cieloTransactionId,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

private fun PurchaseAttemptModel.toEntity() = PurchaseAttempt(
    idempotencyKey = idempotencyKey,
    eventId = eventId,
    eventTitle = eventTitle,
    ticketQuantity = ticketQuantity,
    totalAmountCents = totalAmountCents,
    status = status.name,
    cieloTransactionId = cieloTransactionId,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)
