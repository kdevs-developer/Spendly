package com.kdevs.spendly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kdevs.spendly.nav.SpendlyNav
import com.kdevs.spendly.ui.splash.SplashOverlay
import com.kdevs.spendly.ui.theme.SpendlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Swaps Theme.Spendly.Starting for Theme.Spendly once the first frame is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendlyTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    SpendlyNav()
                    if (showSplash) SplashOverlay { showSplash = false }
                }
            }
        }
    }
}
