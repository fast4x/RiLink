package it.fast4x.ritune

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import it.fast4x.ritune.service.registerNsdService
import it.fast4x.ritune.ui.Player
import it.fast4x.ritune.ui.theme.RiTuneTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            RiTuneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Player(innerPadding)
                }
            }
        }
        registerNsdService()
    }

    override fun onDestroy() {
        super.onDestroy()
        registerNsdService(unRegister = true)
    }
}
