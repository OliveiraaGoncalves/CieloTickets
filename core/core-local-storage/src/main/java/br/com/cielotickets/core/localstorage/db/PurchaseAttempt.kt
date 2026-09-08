package br.com.cielotickets.core.localstorage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val PURCHASE_ATTEMPT_TABLE = "purchase_attempt"

@Entity(tableName = PURCHASE_ATTEMPT_TABLE)
data class PurchaseAttempt(
    @PrimaryKey val idempotencyKey: String,
    val eventId: String,
    val eventTitle: String,
    val ticketQuantity: Int,
    val totalAmountCents: Long,
    val status: String,
    val cieloTransactionId: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)