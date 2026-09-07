package br.com.cielotickets.core.localstorage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PurchaseAttemptDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(attempt: PurchaseAttempt): Long

    @Update
    suspend fun update(attempt: PurchaseAttempt)

    @Query("SELECT * FROM purchase_attempt WHERE idempotencyKey = :key LIMIT 1")
    suspend fun findByKey(key: String): PurchaseAttempt?

    @Query("SELECT * FROM purchase_attempt ORDER BY createdAtEpochMs DESC")
    suspend fun history(): List<PurchaseAttempt>
}
