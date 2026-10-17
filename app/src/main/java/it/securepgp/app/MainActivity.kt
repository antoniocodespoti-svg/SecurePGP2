package it.securepgp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.securepgp.app.ui.screens.HomeScreen
import it.securepgp.app.ui.theme.SecurePGPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurePGPTheme {
                HomeScreen()
            }
        }
    }
}
