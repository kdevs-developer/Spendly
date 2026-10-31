package com.kdevs.spendly

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kdevs.spendly.data.Biometric
import com.kdevs.spendly.data.Inbox
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.nav.SpendlyNav
import com.kdevs.spendly.ui.main.MainViewModel
import com.kdevs.spendly.ui.main.SystemBarsEffect
import com.kdevs.spendly.ui.main.registerNotificationPermission
import com.kdevs.spendly.ui.screens.settings.LockScreen
import com.kdevs.spendly.ui.splash.SplashOverlay
import com.kdevs.spendly.ui.theme.SpendlyTheme
import dagger.hilt.android.AndroidEntryPoint

// FragmentActivity (it extends ComponentActivity) is what the system biometric prompt needs.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()
    private lateinit var askForNotifications: () -> Unit

    private fun readChatExtra(intent: Intent?) {
        intent?.getStringExtra(Inbox.EXTRA_CHAT)?.takeIf { it.isNotBlank() }?.let {
            Inbox.openRequest.value = it
            intent.removeExtra(Inbox.EXTRA_CHAT)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readChatExtra(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Swaps Theme.Spendly.Starting for Theme.Spendly once the first frame is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotifications = registerNotificationPermission()
        readChatExtra(intent)

        // "System" theme follows the phone. The activity restarts when the phone switches mode.
        Prefs.systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        vm.startSession(Biometric.available(this))

        setContent {
            val uid by vm.uid.collectAsStateWithLifecycle()
            val locked by vm.locked.collectAsStateWithLifecycle()
            LaunchedEffect(uid) { if (uid != null) askForNotifications() }
            SystemBarsEffect(Prefs.isDark)
            SpendlyTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    SpendlyNav()
                    if (locked && uid != null) LockScreen(active = !showSplash) { vm.unlock() }
                    if (showSplash) SplashOverlay { showSplash = false }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.onAppStopped()
    }

    override fun onStart() {
        super.onStart()
        vm.onAppStarted(Biometric.available(this))
    }
}
