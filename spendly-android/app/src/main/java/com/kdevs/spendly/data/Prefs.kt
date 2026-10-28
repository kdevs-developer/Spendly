package com.kdevs.spendly.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/**
 * Small on-device settings: theme and app lock. They belong to the phone, not the account,
 * so they live in SharedPreferences. The values are Compose state, so screens update at once.
 */
object Prefs {
    private lateinit var sp: SharedPreferences

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var biometric by mutableStateOf(false)
        private set

    /** Kept up to date by MainActivity so "System" can follow the phone. */
    var systemDark by mutableStateOf(false)

    val isDark: Boolean
        get() = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemDark
        }

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("spendly_prefs", Context.MODE_PRIVATE)
        themeMode = runCatching { ThemeMode.valueOf(sp.getString("theme", ThemeMode.SYSTEM.name).orEmpty()) }
            .getOrDefault(ThemeMode.SYSTEM)
        biometric = sp.getBoolean("biometric", false)
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit().putString("theme", mode.name).apply()
    }

    fun setBiometric(on: Boolean) {
        biometric = on
        sp.edit().putBoolean("biometric", on).apply()
    }
}
