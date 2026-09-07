package br.com.cielotickets.core.localstorage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.cielotickets.core.localstorage.db.AppDatabase
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.core.localstorage.db.SeedEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalStorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        lateinit var instance: AppDatabase
        instance = Room.databaseBuilder(context, AppDatabase::class.java, "cielo-tickets.db")
            .fallbackToDestructiveMigration() // aceitável para o escopo do case; ver docs/ARCHITECTURE.md
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Popula o catálogo local de eventos na primeira criação do banco —
                    // `instance` já está atribuída quando isso dispara, pois o Room só
                    // materializa o arquivo de banco no primeiro acesso, não em .build().
                    CoroutineScope(Dispatchers.IO).launch {
                        instance.eventDao().insertAll(SeedEvents.all)
                    }
                }
            })
            .build()
        return instance
    }

    @Provides
    fun providePurchaseAttemptDao(database: AppDatabase): PurchaseAttemptDao = database.purchaseAttemptDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()
}
