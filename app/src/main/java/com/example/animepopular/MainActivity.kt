package com.example.animepopular

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.NavGraph
import com.example.animepopular.ui.theme.AnimePopularTheme
import com.example.animepopular.ui.theme.BackgroundDark

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AnimeApplication
        val container = app.container

        // ✅ Auto-login dihapus — LoginScreen yang menangani autentikasi.
        //    Jika token sudah tersimpan (isLoggedInFlow = true),
        //    NavGraph akan langsung skip Login ke Home.

        setContent {
            val darkMode by container.preferences.darkModeFlow
                .collectAsStateWithLifecycle(initialValue = true)

            AnimePopularTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    NavGraph(container = container)
                }
            }
        }
    }
}