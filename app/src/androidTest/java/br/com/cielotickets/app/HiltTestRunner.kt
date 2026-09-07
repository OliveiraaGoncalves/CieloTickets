package br.com.cielotickets.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Troca a Application real por [HiltTestApplication] — sem isso os módulos
 * anotados com `@TestInstallIn` (ver `di/Test*Module.kt`) nunca substituem
 * os módulos de produção, e um teste instrumentado tentaria abrir a Cielo
 * Smart de verdade ou gravar no banco real do dispositivo.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
