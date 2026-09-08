package br.com.cielotickets.core.localstorage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val EVENT_TABLE = "event"

@Entity(tableName = EVENT_TABLE)
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val venue: String,
    val dateTimeIso: String,
    val priceCents: Long,
    val availableTickets: Int,
    val imageUrl: String?
)