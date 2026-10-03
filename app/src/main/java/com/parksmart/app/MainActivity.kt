package com.parksmart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.parksmart.app.ui.ParkSmartApp
import com.parksmart.app.ui.theme.ParkSmartTheme

class MainActivity : ComponentActivity() {
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
