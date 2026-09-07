package br.com.cielotickets.core.localstorage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro local de CADA tentativa de compra, criado ANTES de chamar a
 * Cielo Smart e atualizado com o resultado (aprovada/negada/cancelada).
 * A chave de idempotência é gerada uma única vez por "intenção de compra"
 * e reaproveitada em reenvios — é isso que impede duplicidade de cobrança
 * se o usuário perder conexão e tocar em "pagar" de novo.
 */
@Entity(tableName = "purchase_attempt")
data class PurchaseAttempt(
    @PrimaryKey val idempotencyKey: String,
    val eventId: String,
    val ticketQuantity: Int,
    val totalAmountCents: Long,
    val status: String, // PENDING | APPROVED | DENIED | CANCELLED | ERROR
    val cieloTransactionId: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
