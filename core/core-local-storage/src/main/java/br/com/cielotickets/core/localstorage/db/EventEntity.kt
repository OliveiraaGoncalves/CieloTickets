package br.com.cielotickets.core.localstorage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catálogo de eventos local — o case libera não ter backend próprio
 * (ver docs/ARCHITECTURE.md), então esta tabela É a fonte de verdade dos
 * eventos disponíveis, populada uma única vez via [SeedEvents].
 */
@Entity(tableName = "event")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val venue: String,
    val dateTimeIso: String,
    val priceCents: Long,
    val availableTickets: Int,
    val imageUrl: String?
)
