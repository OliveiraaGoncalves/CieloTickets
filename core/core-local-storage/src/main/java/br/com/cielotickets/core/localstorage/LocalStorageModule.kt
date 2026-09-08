package br.com.cielotickets.core.localstorage

import android.content.Context
import androidx.room.Room
import br.com.cielotickets.core.localstorage.db.AppDatabase
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalStorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "cielo-tickets.db")
            .fallbackToDestructiveMigration() // aceitável para o escopo do case; ver docs/ARCHITECTURE.md
            // Sem seed no onCreate: o catálogo de eventos vem do mockapi.io
            // agora, não é mais fixo — `EventRepositoryImpl` popula a tabela
            // `event` (como cache) na primeira chamada bem-sucedida à rede.
            .build()

    @Provides
    fun providePurchaseAttemptDao(database: AppDatabase): PurchaseAttemptDao = database.purchaseAttemptDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()
}