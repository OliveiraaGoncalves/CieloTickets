package br.com.cielotickets.core.localstorage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventDao {
    @Query("SELECT * FROM event ORDER BY dateTimeIso ASC")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM event WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<EventEntity>)
}
