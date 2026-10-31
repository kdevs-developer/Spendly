package com.kdevs.spendly.ui.main

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Res
import android.graphics.Color as AndroidColor

/** Status and navigation bar icons follow the app theme (Settings > Appearance), not only the phone's. */
@Composable
fun ComponentActivity.SystemBarsEffect(dark: Boolean) {
    DisposableEffect(dark) {
        enableEdgeToEdge(
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
}
