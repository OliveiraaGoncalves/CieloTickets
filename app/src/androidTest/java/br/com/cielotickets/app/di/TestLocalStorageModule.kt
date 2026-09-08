package br.com.cielotickets.app.di

import android.content.Context
import androidx.room.Room
import br.com.cielotickets.core.localstorage.LocalStorageModule
import br.com.cielotickets.core.localstorage.db.AppDatabase
import br.com.cielotickets.core.localstorage.db.EventDao
import br.com.cielotickets.core.localstorage.db.PurchaseAttemptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Substitui [LocalStorageModule] nos testes: banco em memória, nunca toca
 * no arquivo real do dispositivo. Sem seed de eventos aqui — o catálogo
 * agora vem da rede (ver `TestEventNetworkModule`), e o próprio fluxo real
 * de produção (`HomeViewModel` chama `getAvailableEvents()` no `init`) já
 * popula essa tabela como cache, exatamente como em produção.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LocalStorageModule::class])
object TestLocalStorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides
    fun providePurchaseAttemptDao(database: AppDatabase): PurchaseAttemptDao = database.purchaseAttemptDao()

    @Provides
    fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()
}