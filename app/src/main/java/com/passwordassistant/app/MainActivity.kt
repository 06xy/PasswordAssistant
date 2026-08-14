package com.passwordassistant.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.passwordassistant.app.ui.PasswordAssistantApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val lockScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lockJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasswordAssistantApp()
        }
    }

    override fun onStart() {
        super.onStart()
        lockJob?.cancel()
    }

    override fun onStop() {
        super.onStop()
        val vault = (application as PasswordApp).container.vaultManager
        lockJob = lockScope.launch {
            delay(60_000)
            vault.lock()
        }
    }
}
