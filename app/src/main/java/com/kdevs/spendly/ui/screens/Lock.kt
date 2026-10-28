package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kdevs.spendly.data.Biometric
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.findFragmentActivity
import com.kdevs.spendly.ui.components.Hint
import com.kdevs.spendly.ui.components.PrimaryButton
import com.kdevs.spendly.ui.theme.Bg
import com.kdevs.spendly.ui.theme.ButtonBrush
import com.kdevs.spendly.ui.theme.Ink
import kotlinx.coroutines.delay

/**
 * Covers the whole app until the person proves it is them. [active] turns true once the splash
 * is gone, and the system prompt then opens by itself. Unlock asks again after a failure.
 */
@Composable
fun LockScreen(active: Boolean, onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }

    fun ask() {
        val activity = ctx.findFragmentActivity()
        // The phone lost its fingerprint / screen lock: do not lock the person out of their own data.
        if (activity == null || !Biometric.available(ctx)) {
            Prefs.updateBiometric(false)
            onUnlocked()
            return
        }
        if (waiting) return
        waiting = true
        failed = false
        Biometric.prompt(activity, "Unlock Spendly", "Confirm it is you") { ok ->
            waiting = false
            if (ok) onUnlocked() else failed = true
        }
    }

    LaunchedEffect(active) {
        if (active) { delay(200); ask() }
    }

    Column(
        Modifier.fillMaxSize().background(Bg)
            // Swallow touches so nothing underneath can be tapped while locked.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding().navigationBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(ButtonBrush), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Spendly is locked", style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(8.dp))
        Hint("Use your fingerprint, face or screen lock to open it.", center = true)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Unlock", { ask() }, busy = waiting)
        if (failed) {
            Spacer(Modifier.height(12.dp))
            Hint("That did not work. Tap Unlock to try again.", center = true)
        }
    }
}
