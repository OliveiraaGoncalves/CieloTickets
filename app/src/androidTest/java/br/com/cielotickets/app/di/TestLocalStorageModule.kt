package br.com.cielotickets.app.di

import android.content.Context
import androidx.room.Room
import br.com.cielotickets.core.localstorage.LocalStorageModule
import br.com.cielotickets.core.localstorage.db.AppDatabase
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import br.com.cielotickets.core.localstorage.db.SeedEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Substitui [LocalStorageModule] nos testes: banco em memória (nunca toca no
 * arquivo real do dispositivo) e populado de forma SÍNCRONA. A versão de
 * produção popula via coroutine "fire-and-forget" no `onCreate` do Room — na
 * prática nunca chegou a causar problema visível porque a Home já mostra um
 * loading enquanto espera, mas num teste isso viraria uma corrida (a Home
 * podia compor antes do insert terminar). Aqui a gente garante os eventos
 * já no banco antes do primeiro `provideAppDatabase()` retornar.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LocalStorageModule::class])
object TestLocalStorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        runBlocking { db.eventDao().insertAll(SeedEvents.all) }
        return db
    }

    @Provides
    fun providePurchaseAttemptDao(database: AppDatabase): PurchaseAttemptDao = database.purchaseAttemptDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()
}
