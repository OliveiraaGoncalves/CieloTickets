package br.com.cielotickets.core.localstorage.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PurchaseAttempt::class, EventEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun purchaseAttemptDao(): PurchaseAttemptDao
    abstract fun eventDao(): EventDao
}
