package com.kdevs.spendly.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Android 13+ shows no notifications until the person allows them.
 * Call from onCreate (before the activity starts). The returned function asks once and then stays quiet;
 * Settings has a shortcut after that.
 */
fun ComponentActivity.registerNotificationPermission(): () -> Unit {
    val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return ask@{
        if (Build.VERSION.SDK_INT < 33) return@ask
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return@ask
        val sp = getSharedPreferences("spendly_inbox", Context.MODE_PRIVATE)
        if (sp.getBoolean("asked_notifications", false)) return@ask
        sp.edit { putBoolean("asked_notifications", true) }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
