package br.com.cielotickets.core.localstorage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EventDao {
    @Query("SELECT * FROM $EVENT_TABLE ORDER BY dateTimeIso ASC")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM $EVENT_TABLE WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Query("DELETE FROM $EVENT_TABLE")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        deleteAll()
        insertAll(events)
    }
}