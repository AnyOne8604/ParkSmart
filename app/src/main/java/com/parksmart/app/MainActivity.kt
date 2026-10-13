package com.parksmart.app

// Entrada Android nativa de ParkSmart (base de la app v0.1.0; Compose se mantiene en las versiones posteriores).

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.parksmart.app.ui.ParkSmartApp
import com.parksmart.app.ui.theme.ParkSmartTheme

// Aloja la interfaz Compose dentro del ciclo de vida estándar de una Activity.
class MainActivity : ComponentActivity() {
    // Construye la pantalla raíz una vez creada la Activity y aplica antes el tema visual de ParkSmart.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParkSmartTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ParkSmartApp()
                }
            }
        }
    }
}
