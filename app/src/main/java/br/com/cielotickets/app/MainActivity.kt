package br.com.cielotickets.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.cielotickets.app.navigation.CieloNavHost
import br.com.cielotickets.core.designsystem.CieloTicketsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CieloTicketsTheme {
                CieloNavHost()
            }
        }
    }
}
