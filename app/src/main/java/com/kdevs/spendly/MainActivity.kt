package com.kdevs.spendly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.kdevs.spendly.data.Inbox
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.kdevs.spendly.data.Biometric
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.Res
import com.kdevs.spendly.nav.SpendlyNav
import com.kdevs.spendly.ui.screens.LockScreen
import com.kdevs.spendly.ui.splash.SplashOverlay
import com.kdevs.spendly.ui.theme.SpendlyTheme
import android.graphics.Color as AndroidColor

// FragmentActivity (it extends ComponentActivity) is what the system biometric prompt needs.
class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    private var leftAt = 0L

    // Android 13+ shows no notifications until the person allows them.
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val sp = getSharedPreferences("spendly_inbox", MODE_PRIVATE)
        if (sp.getBoolean("asked_notifications", false)) return // ask once; Settings has a shortcut after that
        sp.edit().putBoolean("asked_notifications", true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
        readChatExtra(intent)

        // "System" theme follows the phone. The activity restarts when the phone switches mode.
        Prefs.systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Locked on a fresh start. A rotation keeps whatever state it had.
        locked = savedInstanceState?.getBoolean("locked")
            ?: (Prefs.biometric && Repo.isSignedIn && Biometric.available(this))

        setContent {
            val dark = Prefs.isDark
            val uid by Repo.uidFlow.collectAsState()
            LaunchedEffect(uid) { if (uid != null) askForNotifications() }
            // Status and navigation bar icons follow the app theme, not only the phone's.
            DisposableEffect(dark) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle =
                        SystemBarStyle.auto(
                            AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF),
                            AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B),
                        ) { dark },
                )
                window.setBackgroundDrawable(ColorDrawable(Res.colorInt(if (dark) R.color.bg_dark else R.color.bg_light)))
                onDispose { }
            }
            SpendlyTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    SpendlyNav()
                    if (locked && uid != null) {
                        LockScreen(active = !showSplash) {
                            locked = false
                            leftAt = 0L
                        }
                    }
                    if (showSplash) SplashOverlay { showSplash = false }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("locked", locked)
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Coming back after more than app_lock_after_seconds (res/values/config.xml) away asks again.
        if (leftAt != 0L && Prefs.biometric && Repo.isSignedIn &&
            SystemClock.elapsedRealtime() - leftAt > Res.integer(R.integer.app_lock_after_seconds) * 1000L && Biometric.available(this)
        ) {
            locked = true
        }
    }
}